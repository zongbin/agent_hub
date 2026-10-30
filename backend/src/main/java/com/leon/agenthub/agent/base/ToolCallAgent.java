package com.leon.agenthub.agent.base;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.leon.agenthub.agent.enums.AgentStateEnum;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * 工具调用代理 — 实现 think（决定调用哪些工具）与 act（执行工具并回填上下文）。
 *
 * <p>禁用 Spring AI 内置的工具调用机制（{@code internalToolExecutionEnabled=false} 语义），
 * 由本类自行维护消息上下文，以便把每一步的思考与工具结果逐步推送给前端。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    /** 可用的工具 */
    private final ToolCallback[] availableTools;

    /** 保存了工具调用信息的响应 */
    private ChatResponse toolCallChatResponse;

    /** 工具调用管理器 */
    private final ToolCallingManager toolCallingManager;

    /** 禁用内置工具调用机制，自己维护选项和消息上下文 */
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 代理式工具调用：由 Agent 自己执行工具，而不是框架内部执行
        this.chatOptions = DashScopeChatOptions.builder()
                .withProxyToolCalls(true)
                .build();
    }

    /**
     * 思考：让模型决定下一步调用哪些工具。
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think() {
        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            getMessageList().add(new UserMessage(getNextStepPrompt()));
        }
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, chatOptions);
        try {
            // 获取带工具选项的响应
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .tools(availableTools)
                    .call()
                    .chatResponse();
            // 记录响应，供 act 使用
            this.toolCallChatResponse = chatResponse;
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            String result = assistantMessage.getText();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            log.info("{}的思考: {}", getName(), result);
            log.info("{}选择了 {} 个工具来使用", getName(), toolCallList.size());
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s",
                            toolCall.name(),
                            toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            if (toolCallList.isEmpty()) {
                // 只有不调用工具时，才记录助手消息
                getMessageList().add(assistantMessage);
                return false;
            }
            // 需要调用工具时无需记录助手消息 —— 调用工具时会自动记录
            return true;
        } catch (Exception e) {
            log.error("{}的思考过程遇到了问题: {}", getName(), e.getMessage());
            getMessageList().add(new AssistantMessage("处理时遇到错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 行动：执行工具调用并处理结果。
     *
     * @return 执行结果
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具调用";
        }
        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录消息上下文，conversationHistory 已包含助手消息与工具调用结果
        setMessageList(toolExecutionResult.conversationHistory());
        // 取当前工具调用的结果
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) toolExecutionResult.conversationHistory()
                .get(toolExecutionResult.conversationHistory().size() - 1);
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "工具 " + response.name() + " 完成了它的任务！结果: " + response.responseData())
                .collect(Collectors.joining("\n"));
        // 判断是否调用了终止工具
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> "doTerminate".equals(response.name()));
        if (terminateToolCalled) {
            setState(AgentStateEnum.FINISHED);
        }
        log.info(results);
        return results;
    }
}
