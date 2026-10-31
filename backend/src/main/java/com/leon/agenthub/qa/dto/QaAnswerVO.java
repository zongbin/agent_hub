package com.leon.agenthub.qa.dto;

import java.util.List;

import com.leon.agenthub.common.llm.ChatCitation;

import lombok.Data;

/**
 * 问答结果视图对象 — SSE done 事件的元数据，供前端启用反馈按钮与渲染引用来源。
 *
 * <p>完整回答内容已通过 delta 事件逐段推送，此处不再重复携带。
 */
@Data
public class QaAnswerVO {

    /** 会话 ID */
    private String conversationId;

    /** 回答消息 ID（可用于提交反馈） */
    private Long messageId;

    /** 引用来源（前端可跳转到对应文档详情页） */
    private List<ChatCitation> sources;
}
