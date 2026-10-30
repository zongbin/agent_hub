package com.leon.agenthub.qa.rag;

import java.util.List;

import com.leon.agenthub.common.llm.ChatCitation;

/**
 * RAG 检索上下文 — 一次召回的产物。
 *
 * @param query       实际用于检索的查询（可能已被改写）
 * @param citations   命中的引用来源（编号与 contextText 中的 [n] 一一对应）
 * @param contextText 拼装好的参考资料文本，直接作为 system 消息交给大模型
 */
public record RagContext(String query, List<ChatCitation> citations, String contextText) {
}
