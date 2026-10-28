package com.leon.agenthub.knowledge.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.leon.agenthub.common.embedding.DashScopeEmbeddingService;
import com.leon.agenthub.knowledge.enricher.KnowlegeDocumentEnricher;
import com.leon.agenthub.knowledge.model.Document;
import com.leon.agenthub.knowledge.model.DocumentChunk;
import com.leon.agenthub.knowledge.parser.DocumentParser;
import com.leon.agenthub.knowledge.repository.DocumentChunkRepository;
import com.leon.agenthub.knowledge.repository.DocumentRepository;
import com.leon.agenthub.knowledge.spliter.DocumentSpliter;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文档索引事件监听器 — 异步处理文档解析→分块→向量化→写库。
 *
 * <p>
 * 流程：解析文本 → 文本分块 → 向量化 → 批量写入 document_chunks 表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentIndexListener {

    private final DocumentParser documentParser;
    private final DocumentSpliter documentSpliter;
    private final DashScopeEmbeddingService embeddingService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final KnowlegeDocumentEnricher knowlegeDocumentEnricher;

    /**
     * 监听文档上传事件，异步执行解析→分块→向量化。
     */
    @Async
    @EventListener
    public void handleDocumentUploaded(DocumentUploadedEvent event) {
        Long docId = event.getDocId();
        Path filePath = event.getFilePath();
        log.info("Processing uploaded document: docId={}, file={}", docId, filePath);

        try {

            // 1. 加载文档，分隔文本并增加摘要和关键词
            List<org.springframework.ai.document.Document> documentList = knowlegeDocumentEnricher
                    .enrichDocumentsBySummary(
                            knowlegeDocumentEnricher.enrichDocumentsByKeyword(
                                    documentSpliter.split(documentParser.loadDocument(filePath))));

            // 2. 向量化
            List<float[]> embeddings = embeddingService.embed(documentList.stream()
                    .map(org.springframework.ai.document.Document::getText).collect(Collectors.toList()));

            // 3. 写入 chunks 表
            List<DocumentChunk> chunkEntities = new ArrayList<>();
            for (int i = 0; i < documentList.size(); i++) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(docId);
                chunk.setChunkIndex(i);
                chunk.setContent(documentList.get(i).getText());
                chunk.setTokenCount(estimateTokenCount(documentList.get(i).getText()));
                // 将 float[] 转为 pgvector 字符串格式
                chunk.setEmbedding(floatArrayToVectorString(embeddings.get(i)));
                chunkEntities.add(chunk);
            }
            documentChunkRepository.insertBatch(chunkEntities);

            // 4. 更新 chunk_count 和状态
            Document updateDoc = documentRepository.findById(docId)
                    .orElseThrow(() -> new IllegalStateException("Document not found: " + docId));
            updateDoc.setChunkCount(documentList.size());
            updateDoc.setStatus(1); // ACTIVE
            documentRepository.save(updateDoc);

            // 5. 生成 AI 摘要和标签
            extractSummaryAndTags (docId, documentList);

            log.info("Document processing complete: docId={}", docId);

        } catch (Exception e) {
            log.error("Document processing failed: docId={}", docId, e);
            // 标记失败
            try {
                Document failDoc = documentRepository.findById(docId).orElse(new Document());
                failDoc.setId(docId);
                failDoc.setStatus(0); // 失败
                failDoc.setErrorMessage(e.getMessage());
                documentRepository.save(failDoc);
            } catch (Exception ex) {
                log.error("Failed to update error status for docId={}", docId, ex);
            }
        }
    }

    /**
     * 生成摘要和标签，失败时降级处理。
     */
    private void extractSummaryAndTags (Long docId, List<org.springframework.ai.document.Document> documentList) {
        try {
            Document fallbackDoc = documentRepository.findById(docId).orElse(new Document());
            fallbackDoc.setId(docId);
            String summary = documentList.stream()
                    .map(org.springframework.ai.document.Document::getMetadata)
                    .map(metadata -> (String) metadata.get("section_summary"))
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.joining(" "));
            fallbackDoc.setSummary(summary.substring(0, Math.min(200, summary.length())));

            String[] excerptKeywords = documentList.stream()
                    .map(org.springframework.ai.document.Document::getMetadata)
                    .map(metadata -> (String) metadata.get("excerpt_keywords"))
                    .filter(StrUtil::isNotBlank)
                    .flatMap(key -> Arrays.stream(key.split(",")))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .toArray(String[]::new);

            fallbackDoc.setTags(excerptKeywords);
            fallbackDoc.setStatus(1); // 仍然标记为 ACTIVE
            documentRepository.save(fallbackDoc);

        } catch (Exception e) {
            log.warn("Summary generation failed, using fallback: docId={}", docId, e);
            // 降级：使用文本截断作为摘要
            try {
                Document fallbackDoc = documentRepository.findById(docId).orElse(new Document());
                fallbackDoc.setId(docId);
                String text = documentList.stream()
                        .map(org.springframework.ai.document.Document::getText)
                        .collect(Collectors.joining(" "));
                fallbackDoc.setSummary(text.substring(0, Math.min(200, text.length())));
                fallbackDoc.setTags(new String[0]);
                fallbackDoc.setStatus(1); // 仍然标记为 ACTIVE
                documentRepository.save(fallbackDoc);
            } catch (Exception ex) {
                log.error("Failed to save fallback for docId={}", docId, ex);
            }
        }
    }

    /**
     * 估算 token 数（粗略）。
     */
    private int estimateTokenCount(String text) {
        return (int) (text.length() / 2.5);
    }

    /**
     * 将 float[] 转为 pgvector 字符串格式：[0.1,0.2,...]
     */
    private String floatArrayToVectorString(float[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
