package com.leon.agenthub.agent.dto;

import lombok.Data;

/**
 * 智能体对话请求 — 用户提交的任务描述，可携带会话 ID 以继续多轮对话。
 */
@Data
public class AgentChatRequest {

    /** 任务描述 */
    private String message;

    /** 会话 ID（可选，为空则新建会话） */
    private String conversationId;
}
