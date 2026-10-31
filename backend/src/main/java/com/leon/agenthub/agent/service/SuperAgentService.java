package com.leon.agenthub.agent.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.leon.agenthub.agent.base.AgentEventListener;
import com.leon.agenthub.agent.dto.AgentChatRequest;
import com.leon.agenthub.agent.dto.AgentChatResponse;
import com.leon.agenthub.agent.dto.AgentErrorEventDTO;
import com.leon.agenthub.agent.dto.AgentStepEventDTO;
import com.leon.agenthub.agent.enums.AgentEventType;
import com.leon.agenthub.agent.enums.AgentMessageRole;
import com.leon.agenthub.agent.model.AgentConversation;
import com.leon.agenthub.agent.superagent.SuperManus;
import com.leon.agenthub.common.exception.BusinessException;
import com.leon.agenthub.permission.annotation.RequirePermission;
import com.leon.agenthub.permission.enums.PermissionAction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 超级智能体服务 — 负责创建智能体实例、驱动执行并把事件翻译成 SSE 推送出去。
 *
 * <p>本类是 agent 运行时与 HTTP 传输层之间的适配器：运行时只回调
 * {@link AgentEventListener}，事件如何送达客户端由这里决定。
 *
 * <p>会话与消息的持久化由 {@link AgentConversationService} 承担。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAgentService {

    /** SSE 连接超时（5 分钟），与 LLM 长回答耗时匹配 */
    private static final long SSE_TIMEOUT_MS = 300_000L;

    /** 任务描述最大长度 */
    private static final int MAX_MESSAGE_LENGTH = 2000;

    /** 智能体可用的工具 */
    private final ToolCallback[] allTools;

    /** 底层 ChatModel（DashScope） */
    private final ChatModel dashscopeChatModel;

    /** 会话与消息持久化 */
    private final AgentConversationService conversationService;

    /**
     * 以用户消息启动一次超级智能体任务。
     *
     * @param request 请求体（任务描述 + 可选会话 ID）
     * @param userId  当前用户 ID
     * @return SSE 发射器，逐步推送执行事件
     */
    @RequirePermission(resource = "AGENT", action = PermissionAction.READ)
    public SseEmitter chat(AgentChatRequest request, Long userId) {
        String message = request == null ? null : request.getMessage();
        if (message == null || message.isBlank()) {
            throw new BusinessException(400, "任务描述不能为空");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new BusinessException(400, "任务描述长度不能超过 " + MAX_MESSAGE_LENGTH + " 字符");
        }

        // 会话解析必须在有登录态的请求线程内完成（异步线程没有 SecurityContext）
        AgentConversation conversation = conversationService.resolveConversation(
                request.getConversationId(), message, userId);
        String conversationId = conversation.getId();

        // 载入本轮之前的历史对话，作为多轮上下文回填给 agent
        List<Message> history = loadHistory(conversationId, userId);

        // 用户提问落库 —— 与助手消息同表，保持一问一答的消息序列
        conversationService.appendMessage(conversationId, AgentMessageRole.USER, message, List.of());

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        AtomicBoolean closed = new AtomicBoolean(false);

        emitter.onTimeout(() -> {
            closed.set(true);
            log.warn("智能体 SSE 连接超时，conversationId={}", conversationId);
            emitter.complete();
        });
        emitter.onError(e -> {
            closed.set(true);
            log.warn("智能体 SSE 连接异常: {}", e.getMessage());
        });

        // 本轮收集到的步骤轨迹，完成时随助手消息一起落库
        List<AgentStepEventDTO> steps = new ArrayList<>();

        AgentEventListener listener = new SseEventListener(emitter, closed, steps, conversationId);

        // 智能体实例带运行期状态（步骤计数、消息上下文），因此每个任务都新建一个实例
        SuperManus superManus = new SuperManus(allTools, dashscopeChatModel);
        // 多轮上下文：把历史对话回填进 agent 的消息上下文
        superManus.getMessageList().addAll(history);

        // 异步执行，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                superManus.setEventListener(listener);
                superManus.run(message);
            } catch (Exception e) {
                log.error("智能体执行异常: {}", e.getMessage(), e);
                listener.onError("执行错误: " + e.getMessage());
            }
        });

        return emitter;
    }

    /**
     * 载入会话的历史对话 — 作为多轮上下文回填。
     *
     * <p>只还原「用户提问 + 智能体最终回答」的文本，不还原中间的工具调用与工具结果原始结构
     * （见 design.md D4 的 MVP 取舍说明）。
     *
     * @param conversationId 会话 ID
     * @return 历史消息序列
     */
    private List<Message> loadHistory(String conversationId, Long userId) {
        List<Message> history = new ArrayList<>();
        conversationService.listMessages(conversationId, userId).forEach(vo -> {
            if (AgentMessageRole.USER.name().equals(vo.getRole())) {
                history.add(new UserMessage(vo.getContent()));
            } else if (AgentMessageRole.ASSISTANT.name().equals(vo.getRole())) {
                history.add(new AssistantMessage(vo.getContent()));
            }
        });
        return history;
    }

    /**
     * SSE 事件监听器 — 把 agent 运行时事件翻译成 SSE 事件。
     */
    private class SseEventListener implements AgentEventListener {

        private final SseEmitter emitter;
        private final AtomicBoolean closed;
        private final List<AgentStepEventDTO> steps;
        private final String conversationId;

        SseEventListener(SseEmitter emitter, AtomicBoolean closed, List<AgentStepEventDTO> steps,
                String conversationId) {
            this.emitter = emitter;
            this.closed = closed;
            this.steps = steps;
            this.conversationId = conversationId;
        }

        @Override
        public void onStepStart(AgentStepEventDTO event) {
            steps.add(event);
            send(AgentEventType.STEP_START, event);
        }

        @Override
        public void onStepProgress(AgentStepEventDTO event) {
            steps.add(event);
            send(AgentEventType.STEP_PROGRESS, event);
        }

        @Override
        public void onStepComplete(AgentStepEventDTO event) {
            steps.add(event);
            send(AgentEventType.STEP_COMPLETE, event);
        }

        @Override
        public void onComplete(String finalResult) {
            // 落库在异步线程执行，此时没有登录态，不能带 @RequirePermission
            Long messageId = conversationService.appendMessage(conversationId,
                    AgentMessageRole.ASSISTANT, finalResult, steps);
            send(AgentEventType.COMPLETE, AgentChatResponse.of(conversationId, messageId, finalResult));
            emitter.complete();
        }

        @Override
        public void onError(String errorMessage) {
            send(AgentEventType.ERROR, AgentErrorEventDTO.of(errorMessage));
            emitter.complete();
        }

        /**
         * 推送一个 SSE 事件。
         */
        private void send(AgentEventType eventType, Object payload) {
            if (closed.get()) {
                return;
            }
            try {
                emitter.send(SseEmitter.event().name(eventType.getCode()).data(payload));
            } catch (IOException e) {
                if (closed.compareAndSet(false, true)) {
                    emitter.completeWithError(e);
                }
            }
        }
    }
}
