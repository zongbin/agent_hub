package com.leon.agenthub.knowledge.service;

import com.leon.agenthub.knowledge.enums.DocumentStatus;
import com.leon.agenthub.knowledge.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文档索引失败标记器 — 在独立事务中把文档标记为失败。
 *
 * <p>独立成 Bean 是为了保证 {@code @Transactional(REQUIRES_NEW)} 经由代理生效
 * （同类内部自调用不会开启新事务）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIndexFailureHandler {

    private final DocumentRepository documentRepository;

    /**
     * 将文档标记为处理失败并记录原因。
     *
     * @param docId   文档 ID
     * @param message 失败原因
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long docId, String message) {
        try {
            documentRepository.updateStatusAndError(docId, DocumentStatus.FAILED.getCode(), message);
            log.warn("Document marked as FAILED: docId={}, reason={}", docId, message);
        } catch (Exception e) {
            log.error("Failed to mark document as FAILED: docId={}", docId, e);
        }
    }
}
