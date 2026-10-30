package com.leon.agenthub.qa.dto;

import lombok.Data;

/**
 * 回答增量视图对象 — SSE delta 事件载荷。
 */
@Data
public class QaDeltaVO {

    /** 增量文本 */
    private String content;

    public static QaDeltaVO of(String content) {
        QaDeltaVO vo = new QaDeltaVO();
        vo.setContent(content == null ? "" : content);
        return vo;
    }
}
