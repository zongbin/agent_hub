package com.leon.agenthub.common.exception;

import com.leon.agenthub.common.response.R;
import com.leon.agenthub.permission.exception.PermissionDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器 — 兜底所有未捕获异常，保证返回格式统一为 {@code R}。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusiness(BusinessException e) {
        log.warn("Business exception: code={}, msg={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public R<Void> handlePermissionDenied(PermissionDeniedException e) {
        log.warn("Permission denied: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleUnknown(Exception e) {
        log.error("Unhandled exception", e);
        return R.fail(500, "服务器内部错误");
    }
}
