package com.leon.agenthub.qa.dto;

import lombok.Data;

/**
 * 问答请求 — 用户提交的问题，可携带会话 ID 以继续多轮对话。
 */
@Data
public class QaRequest {

    /** 会话 ID（可选，为空则新建会话） */
    private String conversationId;

    /** 问题内容 */
    private String question;
}
