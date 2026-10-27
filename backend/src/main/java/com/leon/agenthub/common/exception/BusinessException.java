package com.leon.agenthub.common.exception;

import lombok.Getter;

/**
 * 业务异常 — 被 {@link GlobalExceptionHandler} 捕获后转换为 {@code R.fail(code, message)}。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(400, message);
    }
}
