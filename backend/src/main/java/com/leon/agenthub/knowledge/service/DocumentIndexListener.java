package com.leon.agenthub.knowledge.service;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
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

    /** 是否调用 LLM 生成关键词/摘要（关闭后仅做分块向量化，成本与耗时更低） */
    @Value("${agenthub.knowledge.enrich-with-llm:true}")
    private boolean enrichWithLlm;

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
            // 1. 加载文档并分隔文本
            List<org.springframework.ai.document.Document> documentList =
                    documentSpliter.split(documentParser.loadDocument(filePath));

            // 2. LLM 元信息增强（关键词/摘要）：属于锦上添花，失败不阻断索引
            documentList = enrichQuietly(documentList);

            // 3. 读取文档元信息（密级、标题）
            Document doc = documentRepository.findById(docId)
                    .orElseThrow(() -> new IllegalStateException("Document not found: " + docId));

            // 4. 写入 vector_store（内部自动 embedding）
            vectorStoreService.addChunks(docId, doc.getSecurityLevel(), doc.getTitle(), documentList);

            // 5. 更新 chunk_count 和状态
            doc.setChunkCount(documentList.size());
            doc.setStatus(DocumentStatus.ACTIVE);
            documentRepository.save(doc);

            // 6. 生成 AI 摘要和标签
            extractSummaryAndTags(docId, documentList);

            log.info("Document processing complete: docId={}", docId);

        } catch (Exception e) {
            log.error("Document processing failed: docId={}", docId, e);
            // 标记失败
            try {
                Document failDoc = documentRepository.findById(docId).orElse(new Document());
                failDoc.setId(docId);
                failDoc.setStatus(DocumentStatus.FAILED);
                failDoc.setErrorMessage(errorMessageOf(e));
                documentRepository.save(failDoc);
            } catch (Exception ex) {
                log.error("Failed to update error status for docId={}", docId, ex);
            }
        }
    }

    /**
     * 关键词/摘要增强：LLM 不可用（欠费、限流、超时）时降级为「无增强元信息」，
     * 不影响后续分块向量化与检索。
     */
    private List<org.springframework.ai.document.Document> enrichQuietly(
            List<org.springframework.ai.document.Document> documents) {
        if (!enrichWithLlm) {
            log.info("LLM 元信息增强已关闭，跳过关键词/摘要生成: {} 个分块", documents.size());
            return documents;
        }
        List<org.springframework.ai.document.Document> enriched = documents;
        try {
            enriched = knowlegeDocumentEnricher.enrichDocumentsByKeyword(enriched);
        } catch (Exception e) {
            log.warn("关键词增强失败，跳过（不影响索引）: {}", e.getMessage());
        }
        try {
            enriched = knowlegeDocumentEnricher.enrichDocumentsBySummary(enriched);
        } catch (Exception e) {
            log.warn("摘要增强失败，跳过（不影响索引）: {}", e.getMessage());
        }
        return enriched;
    }

    /**
     * 提取可读的错误信息（截断，避免把整段异常堆栈写进字段）。
     */
    private String errorMessageOf(Exception e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    /**
     * 生成摘要和标签；LLM 增强缺失或失败时降级为「正文截断摘要 + 空标签」。
     */
    private void extractSummaryAndTags(Long docId, List<org.springframework.ai.document.Document> documentList) {
        try {
            Document target = documentRepository.findById(docId).orElse(new Document());
            target.setId(docId);

            String summary = documentList.stream()
                    .map(org.springframework.ai.document.Document::getMetadata)
                    .map(metadata -> (String) metadata.get("section_summary"))
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining(" "));
            if (StrUtil.isBlank(summary)) {
                // 降级：使用正文首段截断作为摘要
                summary = documentList.stream()
                        .map(org.springframework.ai.document.Document::getText)
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.joining(" "));
            }
            target.setSummary(summary.substring(0, Math.min(200, summary.length())));

            String[] excerptKeywords = documentList.stream()
                    .map(org.springframework.ai.document.Document::getMetadata)
                    .map(metadata -> (String) metadata.get("excerpt_keywords"))
                    .filter(StrUtil::isNotBlank)
                    .flatMap(key -> Arrays.stream(key.split(",")))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .toArray(String[]::new);

            target.setTags(excerptKeywords);
            target.setStatus(DocumentStatus.ACTIVE);
            documentRepository.save(target);

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
