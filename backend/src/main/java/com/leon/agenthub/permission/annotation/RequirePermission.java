package com.leon.agenthub.permission.annotation;

import com.leon.agenthub.permission.enums.SecurityLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解 — 标注在服务方法上，由 PermissionAspect 拦截并执行权限校验。
 *
 * <p>使用示例：
 * <pre>{@code
 * @RequirePermission(resource = "DOCUMENT", action = "READ", minLevel = SecurityLevel.INTERNAL)
 * public Document getDocument(String docId) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 资源类型（如 DOCUMENT、AGENT、ADMIN）。
     */
    String resource();

    /**
     * 操作类型（READ/WRITE/DELETE/EXECUTE）。
     */
    String action();

    /**
     * 最低安全密级要求。
     */
    SecurityLevel minLevel() default SecurityLevel.PUBLIC;
}
