package com.leon.agenthub.knowledge.enricher;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.transformer.KeywordMetadataEnricher;
import org.springframework.ai.transformer.SummaryMetadataEnricher;
import org.springframework.ai.transformer.SummaryMetadataEnricher.SummaryType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cn.hutool.core.util.StrUtil;

@Component
public class KnowlegeDocumentEnricher {

    /** 中文摘要提示模板，必须包含 {context_str} 占位符 */
    private static final String CHINESE_SUMMARY_TEMPLATE = """
            以下是文档片段的内容：
            {context_str}

            请用中文概括该片段的关键主题和关键实体。

            摘要：""";

    /** 摘要元信息在 metadata 中的 key */
    private static final String METADATA_SUMMARY = "section_summary";

    private final ChatModel chatModel;

    /**
     * 需要调用 LLM 生成摘要的分块数量（其余分块复用首个分块的摘要）。
     *
     * <p>默认 1：只对首块调用一次 LLM，避免分块数 × LLM 调用次数的耗时与费用放大。
     */
    @Value("${agenthub.knowledge.summary-chunks:1}")
    private int summaryChunks;

    KnowlegeDocumentEnricher(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    // 关键词元信息增强器（整篇文档一次调用，关键词写入每个分块）
    public List<Document> enrichDocumentsByKeyword(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return documents;
        }
        KeywordMetadataEnricher enricher = new KeywordMetadataEnricher(this.chatModel, 3);
        return enricher.apply(documents);
    }

    /**
     * 摘要元信息增强器 —— 只对前 {@code agenthub.knowledge.summary-chunks} 个分块调用 LLM，
     * 其余分块复用首个分块生成的摘要（section_summary），保证元信息完整但调用次数可控。
     */
    public List<Document> enrichDocumentsBySummary(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return documents;
        }
        int limit = Math.min(Math.max(summaryChunks, 0), documents.size());
        if (limit == 0) {
            return documents;
        }

        // 自定义提示词，使用中文生成摘要
        SummaryMetadataEnricher enricher = new SummaryMetadataEnricher(chatModel,
                List.of(SummaryType.PREVIOUS, SummaryType.CURRENT, SummaryType.NEXT),
                CHINESE_SUMMARY_TEMPLATE, MetadataMode.ALL);

        List<Document> head = new ArrayList<>(documents.subList(0, limit));
        List<Document> enrichedHead = enricher.apply(head);

        // 取首个分块的摘要，供其余分块复用
        String summary = enrichedHead.stream()
                .map(doc -> doc.getMetadata().get(METADATA_SUMMARY))
                .map(value -> value == null ? "" : value.toString())
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(null);

        List<Document> result = new ArrayList<>(enrichedHead);
        for (Document document : documents.subList(limit, documents.size())) {
            if (summary != null) {
                document.getMetadata().put(METADATA_SUMMARY, summary);
            }
            result.add(document);
        }
        return result;
    }
}
