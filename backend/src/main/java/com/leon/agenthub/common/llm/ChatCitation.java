package com.leon.agenthub.common.llm;

/**
 * 引用来源 — 大模型回答所引用的知识库文档分块。
 *
 * @param documentId  文档 ID
 * @param title       文档标题
 * @param chunkIndex  分块序号（从 0 开始）
 * @param snippet     分块内容片段（截断后，用于前端展示）
 * @param documentUrl 文档详情页地址（前端可直接跳转）
 */
public record ChatCitation(Long documentId, String title, Integer chunkIndex, String snippet, String documentUrl) {
}
