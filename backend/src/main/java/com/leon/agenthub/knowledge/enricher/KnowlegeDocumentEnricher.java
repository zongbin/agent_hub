package com.leon.agenthub.knowledge.enricher;

import java.util.List;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.KeywordMetadataEnricher;
import org.springframework.ai.transformer.SummaryMetadataEnricher;
import org.springframework.ai.transformer.SummaryMetadataEnricher.SummaryType;
import org.springframework.stereotype.Component;

@Component
public class KnowlegeDocumentEnricher {

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
        SummaryMetadataEnricher enricher = new SummaryMetadataEnricher(chatModel,
                List.of(SummaryType.PREVIOUS, SummaryType.CURRENT, SummaryType.NEXT));
        return enricher.apply(documents);
    }
}