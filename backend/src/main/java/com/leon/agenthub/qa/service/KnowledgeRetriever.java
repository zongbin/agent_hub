package com.leon.agenthub.qa.service;

import com.leon.agenthub.knowledge.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识检索服务 — 通过 {@link VectorStoreService} 做权限感知的向量相似度检索。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeRetriever {

    private final VectorStoreService vectorStoreService;

    /** 检索返回的分块数 */
    @Value("${agenthub.qa.top-k:5}")
    private int topK;

    /**
     * 检索与问题最相关的分块。
     *
     * @param question 用户问题
     * @param userId   当前用户 ID（用于文档级授权判断）
     * @param userRole 当前用户角色代码
     * @return 命中分块（Spring AI Document，含 text 与 metadata）
     */
    public List<org.springframework.ai.document.Document> retrieve(String question, Long userId, String userRole) {
        return vectorStoreService.search(question, userId, userRole, topK);
    }
}
