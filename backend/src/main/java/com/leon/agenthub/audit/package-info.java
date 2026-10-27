/**
 * 审计模块 — 操作日志记录。
 *
 * <ul>
 *   <li>{@code annotation}  — {@code @AuditLog} 注解：标注需要记录审计日志的方法</li>
 *   <li>{@code aspect}      — 切面：解析操作人、操作类型、目标资源、结果，异步写入</li>
 *   <li>{@code service}     — 审计日志查询（管理后台用）</li>
 *   <li>{@code config}      — 异步线程池、忽略字段配置</li>
 * </ul>
 */
package com.leon.agenthub.audit;
