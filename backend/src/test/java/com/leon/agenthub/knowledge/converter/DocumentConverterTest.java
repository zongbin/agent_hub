package com.leon.agenthub.knowledge.converter;

import com.leon.agenthub.knowledge.enums.DocumentStatus;
import com.leon.agenthub.knowledge.model.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link DocumentConverter} 与 {@link DocumentStatus} 单元测试 — 覆盖状态枚举与视图状态映射。
 */
class DocumentConverterTest {

    @Test
    void statusCodeMapsToEnum() {
        Document doc = new Document();
        doc.setStatus(DocumentStatus.PROCESSING);
        assertEquals(Integer.valueOf(2), doc.getStatusCode());
        assertEquals(DocumentStatus.PROCESSING, doc.getStatus());

        doc.setStatus(DocumentStatus.ACTIVE);
        assertEquals(DocumentStatus.ACTIVE, doc.getStatus());

        doc.setStatus(DocumentStatus.FAILED);
        assertEquals(DocumentStatus.FAILED, doc.getStatus());
    }

    @Test
    void unknownOrNullCodeFallsBackToActive() {
        assertEquals(DocumentStatus.ACTIVE, DocumentStatus.fromCode(null));
        assertEquals(DocumentStatus.ACTIVE, DocumentStatus.fromCode(99));
    }

    @Test
    void deletedOverridesStatusInVo() {
        Document doc = new Document();
        doc.setStatus(DocumentStatus.ACTIVE);
        doc.setDeleted(1);

        assertEquals(DocumentStatus.DELETED, DocumentConverter.toVO(doc).getStatus());
    }

    @Test
    void voStatusFollowsEntityStatus() {
        Document doc = new Document();
        doc.setStatus(DocumentStatus.PROCESSING);
        doc.setDeleted(0);

        assertEquals(DocumentStatus.PROCESSING, DocumentConverter.toVO(doc).getStatus());
    }
}
