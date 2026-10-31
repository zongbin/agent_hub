package com.leon.agenthub.permission.annotation;

import com.leon.agenthub.permission.enums.PermissionAction;
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
 * @RequirePermission(resource = "DOCUMENT", action = PermissionAction.READ, minLevel = SecurityLevel.INTERNAL)
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
     * 操作类型（READ / WRITE / DELETE）。
     */
    PermissionAction action();

    /**
     * 最低安全密级要求。
     */
    SecurityLevel minLevel() default SecurityLevel.PUBLIC;

    /**
     * 是否仅管理员（ADMIN）可访问。
     *
     * <p>用于权限管理这类「只有管理员能操作」的接口 —— 角色密级矩阵无法表达
     * 「仅 ADMIN」，因此单独提供该开关。
     */
    boolean adminOnly() default false;
}
