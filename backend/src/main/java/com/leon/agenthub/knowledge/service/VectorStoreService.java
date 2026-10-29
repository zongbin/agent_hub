package com.leon.agenthub.knowledge.service;

import com.leon.agenthub.permission.model.DocPermission;
import com.leon.agenthub.permission.repository.DocPermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 向量存储服务 — 封装 Spring AI 的 {@link VectorStore}，统一管理 vector_store 表的写入、权限感知检索与删除。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final VectorStore pgVectorStore;
    private final DocPermissionRepository docPermissionRepository;

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
        log.debug("Added {} chunks for document {} to vector_store", docs.size(), documentId);
    }

    /**
     * 权限感知的相似度检索。
     *
     * <p>按「角色×密级 + 文档级授权」过滤：
     * ADMIN 全看；EDITOR/VIEWER 看 PUBLIC+INTERNAL；GUEST 只看 PUBLIC；
     * 此外，用户在 doc_permissions 中被单独授权的文档（如 CONFIDENTIAL）也会命中。
     */
    public List<Document> search(String query, Long userId, String userRole, int topK) {
        SearchRequest.Builder builder = SearchRequest.builder().query(query).topK(topK);
        if (!"ADMIN".equals(userRole)) {
            builder.filterExpression(buildFilter(userRole, loadGrantedDocumentIds(userId)));
        }
        return pgVectorStore.similaritySearch(builder.build());
    }

    /**
     * 删除某文档在 vector_store 中的全部分块（文档软删除时调用）。
     */
    public void deleteByDocumentId(Long documentId) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        pgVectorStore.delete(b.eq("documentId", documentId).build());
        log.debug("Deleted vector_store entries for document {}", documentId);
    }

    /**
     * 查找用户被单独授权的文档 ID（doc_permissions 中按用户授权的记录）。
     */
    private List<Long> loadGrantedDocumentIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return docPermissionRepository.findByGranteeId(userId).stream()
                .map(DocPermission::getDocumentId)
                .distinct()
                .toList();
    }

    /**
     * 构造「角色×密级 + 文档级授权」的过滤表达式。
     */
    private Filter.Expression buildFilter(String userRole, List<Long> grantedIds) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op roleFilter = switch (userRole) {
            case "GUEST" -> b.in("securityLevel", "PUBLIC");
            default -> b.in("securityLevel", "PUBLIC", "INTERNAL");
        };
        if (grantedIds.isEmpty()) {
            return roleFilter.build();
        }
        return b.or(roleFilter, b.in("documentId", grantedIds.toArray())).build();
    }
}
