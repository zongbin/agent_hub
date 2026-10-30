package com.leon.agenthub.qa.dto;

import lombok.Data;

/**
 * 问答错误视图对象 — SSE error 事件载荷。
 */
@Data
public class QaErrorVO {

    /** 错误信息 */
    private String message;

    public static QaErrorVO of(String message) {
        QaErrorVO vo = new QaErrorVO();
        vo.setMessage(message == null ? "问答处理失败" : message);
        return vo;
    }
}
