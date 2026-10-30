package com.leon.agenthub.common.llm;

import java.util.List;

import reactor.core.publisher.Flux;

/**
 * 流式问答结果 — 结构化返回「回答内容 + 引用文档」，供前端一次性渲染。
 *
 * @param content   回答内容流（增量文本）
 * @param citations 引用来源（与回答一起返回，不提前推送）
 */
public record ChatStreamResult(Flux<String> content, List<ChatCitation> citations) {
}
