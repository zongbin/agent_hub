package com.leon.agenthub.agent.dto;

import lombok.Data;

/**
 * 智能体对话响应 — SSE {@code agent.complete} 事件载荷。
 *
 * <p>携带会话 ID 与消息 ID，前端据此把本轮结果关联到会话与历史记录。
 */
@Data
public class AgentChatResponse {

    /** 会话 ID */
    private String conversationId;

    /** 智能体回答的消息 ID */
    private Long messageId;

    /** 最终回答 */
    private String answer;

    public static AgentChatResponse of(String conversationId, Long messageId, String answer) {
        AgentChatResponse vo = new AgentChatResponse();
        vo.setConversationId(conversationId);
        vo.setMessageId(messageId);
        vo.setAnswer(answer == null ? "" : answer);
        return vo;
    }
}
