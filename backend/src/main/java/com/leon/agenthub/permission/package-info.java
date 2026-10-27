/**
 * 权限模块 — RBAC + 安全等级 + 文档级授权。
 *
 * <ul>
 *   <li>{@code annotation}  — {@code @RequirePermission} 注解定义</li>
 *   <li>{@code aspect}      — 切面实现：JWT → 角色/部门 → 资源安全等级判定</li>
 *   <li>{@code service}     — 权限查询服务（用户角色、文档授权、部门范围）</li>
 *   <li>{@code config}      — 权限缓存、安全等级枚举配置</li>
 * </ul>
 *
 * <p>4 角色（ADMIN / EDITOR / VIEWER / GUEST）× 3 安全等级（PUBLIC / INTERNAL /
 * CONFIDENTIAL）+ doc_permissions 表细粒度授权。
 */
package com.leon.agenthub.permission;
