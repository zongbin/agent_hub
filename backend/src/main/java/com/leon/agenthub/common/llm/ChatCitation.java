package com.leon.agenthub.common.llm;

/**
 * 引用来源 — 大模型回答所引用的知识库文档分块。
 *
 * <p>仅存储定位所需的最小信息。分块内容与文档详情由前端在用户点击时
 * 请求文档详情接口获取，避免消息来源字段冗余存储。
 *
 * @param documentId 文档 ID
 * @param title      文档标题
 * @param chunkIndex 分块序号（从 0 开始）
 */
public record ChatCitation(Long documentId, String title, Integer chunkIndex) {
}
