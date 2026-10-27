package com.leon.agenthub.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解 — 标注在需要记录审计日志的方法上。
 *
 * <p>AuditLogAspect 拦截后自动写入 audit_logs 表。
 *
 * <p>使用示例：
 * <pre>{@code
 * @AuditLog(action = "VIEW", resource = "DOCUMENT")
 * public Document getDocument(String docId) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * 操作类型（如 LOGIN、VIEW、EDIT、DELETE、GRANT、REVOKE、DENIED）。
     */
    String action();

    /**
     * 资源类型（如 DOCUMENT、USER、SYSTEM）。
     */
    String resource() default "";
}
