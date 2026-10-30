package com.leon.agenthub.qa.dto;

import java.util.List;

import com.leon.agenthub.common.llm.ChatCitation;

import lombok.Data;

/**
 * 问答结果视图对象 — 回答内容与引用来源一起返回，供前端渲染。
 */
@Data
public class QaAnswerVO {

    /** 会话 ID */
    private String conversationId;

    /** 回答消息 ID（可用于提交反馈） */
    private Long messageId;

    /** 完整回答内容 */
    private String answer;

    /** 引用来源（前端可跳转到对应文档详情页） */
    private List<ChatCitation> sources;
}
