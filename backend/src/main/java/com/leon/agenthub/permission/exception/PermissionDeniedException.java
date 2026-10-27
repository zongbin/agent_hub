package com.leon.agenthub.permission.exception;

import lombok.Getter;

/**
 * 权限不足异常 — 权限校验失败时抛出，由 GlobalExceptionHandler 捕获后返回 HTTP 403。
 */
@Getter
public class PermissionDeniedException extends RuntimeException {

    /** HTTP 状态码 */
    private static final int CODE = 403;

    public PermissionDeniedException(String message) {
        super(message);
    }

    public int getCode() {
        return CODE;
    }
}
