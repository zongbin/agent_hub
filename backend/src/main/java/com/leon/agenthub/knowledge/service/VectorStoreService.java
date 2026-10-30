package com.leon.agenthub.knowledge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 向量存储服务 — 封装 Spring AI 的 {@link VectorStore}，统一管理 vector_store 表的写入与删除。
 *
 * <p>权限感知的检索由 {@code com.leon.agenthub.qa.rag.RagRetriever} 负责，
 * 过滤规则统一由 {@code DocumentAccessFilter} 提供。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final VectorStore pgVectorStore;

    /**
     * 直接写入文档（保留原有语义）。
     */
    public void addDocuments(List<Document> documents) {
        pgVectorStore.add(documents);
    }

    /**
     * 写入一个文档的全部分块。
     *
     * <p>每个分块的 metadata 固定存 documentId/securityLevel/title/chunkIndex，
     * 供检索时的权限过滤与引用展示使用。id 用确定性 UUID（documentId + chunkIndex），
     * 保证同一文档重复上传时 upsert 覆盖而非重复插入。
     */
    public void addChunks(Long documentId, String securityLevel, String title, List<Document> chunks) {
        List<Document> docs = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            String id = UUID.nameUUIDFromBytes((documentId + "-" + i).getBytes()).toString();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentId", documentId);
            metadata.put("securityLevel", securityLevel);
            metadata.put("title", title);
            metadata.put("chunkIndex", i);
            docs.add(Document.builder().id(id).text(chunk.getText()).metadata(metadata).build());
        }
        pgVectorStore.add(docs);
        log.debug("Added {} chunks for document {} to vector_store", chunks.size(), documentId);
    }

    /**
     * 删除某文档在 vector_store 中的全部分块（文档软删除时调用）。
     */
    public void deleteByDocumentId(Long documentId) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        pgVectorStore.delete(b.eq("documentId", documentId).build());
        log.debug("Deleted vector_store entries for document {}", documentId);
    }
}
