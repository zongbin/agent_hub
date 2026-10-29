package com.leon.agenthub.qa.rag;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import com.leon.agenthub.permission.model.DocPermission;
import com.leon.agenthub.permission.repository.DocPermissionRepository;

import cn.hutool.core.util.StrUtil;

public class RagAdvisorService {

    private final DocPermissionRepository docPermissionRepository;
    private final Advisor ragAdvisor; // 关键：类型改为 Spring AI 的 Advisor

    public RagAdvisorService(VectorStore vectorStore, String userId, String userRole,
            DocPermissionRepository docPermissionRepository, ChatClient.Builder chatClientBuilder) {
        this(vectorStore, userId, userRole, docPermissionRepository, chatClientBuilder, 5);
    }

    public RagAdvisorService(VectorStore vectorStore, String userId, String userRole,
            DocPermissionRepository docPermissionRepository, ChatClient.Builder chatClientBuilder, int topK) {

        this.docPermissionRepository = docPermissionRepository;

        if (StrUtil.isBlank(userRole)) {
            throw new IllegalArgumentException("userRole is required");
        }

        Filter.Expression filterExpr = null;
        if (!"ADMIN".equals(userRole)) {
            Long uid = StrUtil.isBlank(userId) ? null : Long.valueOf(userId);
            List<Long> grantedIds = loadGrantedDocumentIds(uid);
            filterExpr = buildFilter(userRole, grantedIds);
        }

        VectorStoreDocumentRetriever.Builder builder = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(topK);

        if (filterExpr != null) {
            builder.filterExpression(filterExpr);
        }

        PromptTemplate customTemplate = new PromptTemplate("""
                将以下用户查询改写为适合在 {target} 中检索的形式。
                保留核心关键词，去除口语化和无关信息。
                原始查询：{query}
                改写后的查询：
                """);
        // 查询重写
        QueryTransformer queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .promptTemplate(customTemplate)
                .build();

        this.ragAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(queryTransformer)
                .documentRetriever(builder.build())
                .build();
    }

    public Advisor getAdvisor() { // 返回类型改为 Advisor
        return ragAdvisor;
    }

    /**
     *
     * 查找用户有权限的文档
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

    private Filter.Expression buildFilter(String userRole, List<Long> grantedIds) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op roleFilter = switch (userRole) {
            case "GUEST" -> b.in("securityLevel", "PUBLIC");
            default -> b.in("securityLevel", "PUBLIC", "INTERNAL"); // EDITOR/VIEWER/未知角色
        };
        if (grantedIds.isEmpty()) {
            return roleFilter.build();
        }
        return b.or(roleFilter, b.in("documentId", grantedIds.toArray())).build();
    }

}
