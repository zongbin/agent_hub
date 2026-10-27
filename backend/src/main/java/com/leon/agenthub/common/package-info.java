/**
 * 公共模块 — 跨模块共享的基础设施。
 *
 * <ul>
 *   <li>{@code config}      — Redis / MinIO / WebClient / 全局异常等通用配置</li>
 *   <li>{@code llm}         — DeepSeek 聊天/流式客户端封装</li>
 *   <li>{@code embedding}   — Embedding 服务封装（bge-large-zh / DeepSeek-Embedding）</li>
 *   <li>{@code exception}   — 业务异常类 + 全局异常处理器</li>
 *   <li>{@code response}    — 统一响应包装 {@code R<T>} / 分页</li>
 *   <li>{@code util}        — JWT 工具、分页工具等通用静态方法</li>
 * </ul>
 */
package com.leon.agenthub.common;
