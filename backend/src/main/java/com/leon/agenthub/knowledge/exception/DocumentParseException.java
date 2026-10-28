package com.leon.agenthub.knowledge.exception;

import com.leon.agenthub.common.exception.BusinessException;

/**
 * 文档解析异常 — 当 Tika 解析失败时抛出。
 */
public class DocumentParseException extends BusinessException {

    public DocumentParseException(String message) {
        super(422, message);
    }

    public DocumentParseException(String message, Throwable cause) {
        super(422, message + ": " + cause.getMessage());
    }
}
