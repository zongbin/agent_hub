package com.leon.agenthub.agent.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import com.leon.agenthub.agent.dto.AgentStepEventDTO;
import com.leon.agenthub.agent.enums.AgentEventType;

/**
 * {@link ToolCallAgent} 事件上报单元测试 — 验证思考内容、工具调用与工具结果
 * 分别以正确的事件类型回调给监听器。
 *
 * <p>用桩 {@link ChatModel} 返回预置响应，不依赖真实 LLM。
 */
class ToolCallAgentEventTest {

    /** 记录监听器收到的全部事件 */
    private static class RecordingListener implements AgentEventListener {

        final List<AgentStepEventDTO> events = new ArrayList<>();
        String finalResult;
        String errorMessage;

        @Override
        public void onStepStart(AgentStepEventDTO event) {
            events.add(event);
        }

        @Override
        public void onStepProgress(AgentStepEventDTO event) {
            events.add(event);
        }

        @Override
        public void onStepComplete(AgentStepEventDTO event) {
            events.add(event);
        }

        @Override
        public void onComplete(String result) {
            finalResult = result;
        }

        @Override
        public void onError(String message) {
            errorMessage = message;
        }

        /** 取第一个指定类型的事件 */
        AgentStepEventDTO first(AgentEventType type) {
            return events.stream()
                    .filter(e -> e.getEventType() == type)
                    .findFirst()
                    .orElse(null);
        }

        /** 是否存在满足条件的事件 */
        boolean has(AgentEventType type, java.util.function.Predicate<AgentStepEventDTO> matcher) {
            return events.stream()
                    .anyMatch(e -> e.getEventType() == type && matcher.test(e));
        }
    }

    private RecordingListener listener;

    @BeforeEach
    void setUp() {
        listener = new RecordingListener();
    }

    @Test
    void shouldReportStepStartWithToolName() {
        ToolCallAgent agent = buildAgent(assistantWithToolCall("searchWeb", "x"));

        boolean shouldAct = agent.think();

        assertTrue(shouldAct, "存在工具调用时应继续执行");
        AgentStepEventDTO start = listener.first(AgentEventType.STEP_START);
        assertNotNull(start, "应上报步骤开始事件");
        assertEquals("searchWeb", start.getStepName());
        assertEquals(1, start.getStepIndex());
    }

    @Test
    void shouldReportThinkingTextAsProgress() {
        ToolCallAgent agent = buildAgent(assistantWithToolCall("searchWeb", "x"));

        agent.think();

        assertTrue(listener.has(AgentEventType.STEP_PROGRESS,
                e -> e.getMessage() != null && e.getMessage().contains("我需要先搜索")),
                "思考文本应作为步骤进展上报");
    }

    @Test
    void shouldReportToolCallArgumentsAsProgress() {
        ToolCallAgent agent = buildAgent(assistantWithToolCall("searchWeb", "竞品"));

        agent.think();

        assertTrue(listener.has(AgentEventType.STEP_PROGRESS,
                e -> e.getMessage() != null && e.getMessage().contains("searchWeb")),
                "工具调用列表应作为步骤进展上报");
    }

    @Test
    void shouldReportToolResultAsStepComplete() {
        ToolCallAgent agent = buildAgent(assistantWithToolCall("searchWeb", "x"));
        agent.think();

        agent.act();

        AgentStepEventDTO complete = listener.first(AgentEventType.STEP_COMPLETE);
        assertNotNull(complete, "工具执行结果应以上一步骤完成事件上报");
        assertEquals("searchWeb", complete.getToolName());
        assertNotNull(complete.getToolResult());
        assertTrue(complete.getToolResult().contains("搜索结果"), "工具结果应包含工具的实际返回");
    }

    @Test
    void shouldFinishWithFinalResultWhenModelAnswersDirectly() {
        ToolCallAgent agent = buildAgent(assistantWithoutToolCall("这是最终答案"));

        boolean shouldAct = agent.think();

        assertFalse(shouldAct, "没有工具调用时不应继续执行");
        assertNotNull(listener.first(AgentEventType.STEP_COMPLETE), "模型直接作答也应结束本步");
    }

    /**
     * 构造一个使用桩 ChatModel 的智能体。
     *
     * @param response 桩模型返回的响应
     */
    private ToolCallAgent buildAgent(ChatResponse response) {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(response);

        ToolCallback searchWeb = FunctionToolCallback
                .<Map<String, Object>, String>builder("searchWeb",
                        (Map<String, Object> args) -> "搜索结果: " + args.get("query"))
                .description("搜索网页")
                .inputType(Map.class)
                .build();

        ToolCallAgent agent = new ToolCallAgent(new ToolCallback[] { searchWeb });
        agent.setName("test");
        agent.setSystemPrompt("system");
        agent.setNextStepPrompt("next");
        agent.setEventListener(listener);
        // 步骤序号由 BaseAgent.run() 在进入每一步前设置，测试直接调用 think() 需自行置位
        agent.setCurrentStep(1);
        agent.setChatClient(ChatClient.builder(chatModel).build());
        return agent;
    }

    /** 构造一条含工具调用的模型响应（参数为 JSON 对象，与工具的 Map 入参匹配） */
    private static ChatResponse assistantWithToolCall(String toolName, String query) {
        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
                "call-1", "function", toolName, "{\"query\":\"" + query + "\"}");
        AssistantMessage message = new AssistantMessage("我需要先搜索一下", Map.of(), List.of(toolCall));
        return new ChatResponse(List.of(new Generation(message)));
    }

    /** 构造一条不含工具调用的模型响应 */
    private static ChatResponse assistantWithoutToolCall(String text) {
        AssistantMessage message = new AssistantMessage(text);
        return new ChatResponse(List.of(new Generation(message)));
    }
}
