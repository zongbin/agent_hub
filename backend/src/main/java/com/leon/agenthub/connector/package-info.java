/**
 * 连接器模块 — 第三方知识源同步（飞书 / 企业微信文档）。
 *
 * <ul>
 *   <li>{@code feishu}      — 飞书文档 API 适配，OAuth 授权与增量同步</li>
 *   <li>{@code wecom}       — 企业微信素材 API 适配</li>
 *   <li>{@code service}     — 统一同步调度：拉取 → 落 MinIO → 触发知识库索引</li>
 *   <li>{@code config}      — 连接器凭证、同步周期配置</li>
 * </ul>
 *
 * <p>M2 里程碑实现，当前仅预留扩展点。
 */
package com.leon.agenthub.connector;
