package com.leon.agenthub.knowledge.service;

import com.leon.agenthub.knowledge.enums.DocumentStatus;
import com.leon.agenthub.knowledge.repository.DocumentRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * {@link DocumentIndexFailureHandler} 单元测试。
 */
class DocumentIndexFailureHandlerTest {

    @Test
    void marksDocumentAsFailedWithReason() {
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        DocumentIndexFailureHandler handler = new DocumentIndexFailureHandler(documentRepository);

        handler.markFailed(42L, "索引队列已满");

        verify(documentRepository).updateStatusAndError(42L, DocumentStatus.FAILED.getCode(), "索引队列已满");
    }

    @Test
    void swallowsRepositoryFailure() {
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                .when(documentRepository).updateStatusAndError(1L, DocumentStatus.FAILED.getCode(), "x");
        DocumentIndexFailureHandler handler = new DocumentIndexFailureHandler(documentRepository);

        // 不应抛出异常 —— 失败标记本身失败时也不能影响主流程
        handler.markFailed(1L, "x");
    }
}
