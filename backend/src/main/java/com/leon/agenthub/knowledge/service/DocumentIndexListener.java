package com.leon.agenthub.knowledge.service;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.leon.agenthub.common.config.AsyncConfig;
import com.leon.agenthub.knowledge.enricher.KnowlegeDocumentEnricher;
import com.leon.agenthub.knowledge.enums.DocumentStatus;
import com.leon.agenthub.knowledge.model.Document;
import com.leon.agenthub.knowledge.parser.DocumentParser;
import com.leon.agenthub.knowledge.repository.DocumentRepository;
import com.leon.agenthub.knowledge.spliter.DocumentSpliter;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文档索引事件监听器 — 异步处理文档解析→分块→向量化→写库。
 *
 * <p>流程：解析文本 → 文本分块 → 写入 vector_store（内部自动 embedding）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentIndexListener {

    private final DocumentParser documentParser;
    private final DocumentSpliter documentSpliter;
    private final DocumentRepository documentRepository;
    private final VectorStoreService vectorStoreService;
    private final KnowlegeDocumentEnricher knowlegeDocumentEnricher;

    /**
     * 监听文档上传事件，异步执行解析→分块→向量化。
     *
     * <p>使用 {@code documentIndexExecutor} 专用线程池（见 {@link AsyncConfig}），
     * 避免文档索引的长任务与后续其他异步任务争抢默认线程池。
     */
    @Async(AsyncConfig.DOCUMENT_INDEX_EXECUTOR)
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

            // 2. 读取文档元信息（密级、标题）
            Document doc = documentRepository.findById(docId)
                    .orElseThrow(() -> new IllegalStateException("Document not found: " + docId));

            // 3. 写入 vector_store（内部自动 embedding）
            vectorStoreService.addChunks(docId, doc.getSecurityLevel(), doc.getTitle(), documentList);

            // 4. 更新 chunk_count 和状态
            doc.setChunkCount(documentList.size());
            doc.setStatus(DocumentStatus.ACTIVE);
            documentRepository.save(doc);

            // 5. 生成 AI 摘要和标签
            extractSummaryAndTags(docId, documentList);

            log.info("Document processing complete: docId={}", docId);

        } catch (Exception e) {
            log.error("Document processing failed: docId={}", docId, e);
            // 标记失败
            try {
                Document failDoc = documentRepository.findById(docId).orElse(new Document());
                failDoc.setId(docId);
                failDoc.setStatus(DocumentStatus.FAILED);
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
    private void extractSummaryAndTags(Long docId, List<org.springframework.ai.document.Document> documentList) {
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
            fallbackDoc.setStatus(DocumentStatus.ACTIVE);
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
                fallbackDoc.setStatus(DocumentStatus.ACTIVE);
                documentRepository.save(fallbackDoc);
            } catch (Exception ex) {
                log.error("Failed to save fallback for docId={}", docId, ex);
            }
        }
    }
}
