package com.leon.agenthub.knowledge.converter;

import com.leon.agenthub.knowledge.dto.DocumentChunkVO;
import com.leon.agenthub.knowledge.dto.DocumentVO;
import com.leon.agenthub.knowledge.enums.DocumentStatus;
import com.leon.agenthub.knowledge.model.Document;
import com.leon.agenthub.knowledge.model.DocumentChunk;

/**
 * 文档转换器 — 实体与视图对象之间的映射。
 *
 * <p>放在转换器而非 Controller，避免表现层承担业务映射逻辑。
 */
public final class DocumentConverter {

    private DocumentConverter() {
    }

    /**
     * 实体转 VO。
     *
     * @param doc 文档实体
     * @return 文档视图对象
     */
    public static DocumentVO toVO(Document doc) {
        DocumentVO vo = new DocumentVO();
        vo.setId(doc.getId());
        vo.setTitle(doc.getTitle());
        vo.setFileName(doc.getFileKey());
        vo.setFileType(doc.getFileType());
        vo.setFileSize(doc.getFileSize());
        vo.setSecurityLevel(doc.getSecurityLevel());
        vo.setSummary(doc.getSummary());
        vo.setTags(doc.getTags());
        vo.setChunkCount(doc.getChunkCount());
        vo.setCreatedAt(doc.getCreatedAt());
        vo.setStatus(resolveStatus(doc));
        return vo;
    }

    /**
     * 分块实体转 VO。
     *
     * @param chunk 分块
     * @return 分块视图对象
     */
    public static DocumentChunkVO toChunkVO(DocumentChunk chunk) {
        DocumentChunkVO vo = new DocumentChunkVO();
        vo.setChunkIndex(chunk.getChunkIndex());
        vo.setContent(chunk.getContent() == null ? "" : chunk.getContent());
        vo.setCharCount(vo.getContent().length());
        return vo;
    }

    /**
     * 状态映射：实体状态 → 前端展示状态。
     *
     * <p>软删除（deleted=1）优先于 status 列。
     */
    private static DocumentStatus resolveStatus(Document doc) {
        if (doc.getDeleted() != null && doc.getDeleted() == 1) {
            return DocumentStatus.DELETED;
        }
        return doc.getStatus();
    }
}
