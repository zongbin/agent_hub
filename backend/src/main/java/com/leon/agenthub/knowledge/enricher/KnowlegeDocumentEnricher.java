package com.leon.agenthub.knowledge.enricher;

import java.util.List;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.transformer.KeywordMetadataEnricher;
import org.springframework.ai.transformer.SummaryMetadataEnricher;
import org.springframework.ai.transformer.SummaryMetadataEnricher.SummaryType;
import org.springframework.stereotype.Component;

@Component
public class KnowlegeDocumentEnricher {

    /** 中文摘要提示模板，必须包含 {context_str} 占位符 */
    private static final String CHINESE_SUMMARY_TEMPLATE = """
            以下是文档片段的内容：
            {context_str}

            请用中文概括该片段的关键主题和关键实体。

            摘要：""";

    private final ChatModel chatModel;

    KnowlegeDocumentEnricher(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    // 关键词元信息增强器
    public List<Document> enrichDocumentsByKeyword(List<Document> documents) {
        KeywordMetadataEnricher enricher = new KeywordMetadataEnricher(this.chatModel, 5);
        return enricher.apply(documents);
    }

    // 摘要元信息增强器
    public List<Document> enrichDocumentsBySummary(List<Document> documents) {
        // 自定义提示词，使用中文生成摘要
        SummaryMetadataEnricher enricher = new SummaryMetadataEnricher(chatModel,
                List.of(SummaryType.PREVIOUS, SummaryType.CURRENT, SummaryType.NEXT),
                CHINESE_SUMMARY_TEMPLATE, MetadataMode.ALL);
        return enricher.apply(documents);
    }
}