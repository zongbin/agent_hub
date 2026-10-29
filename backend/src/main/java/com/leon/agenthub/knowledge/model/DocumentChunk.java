package com.leon.agenthub.knowledge.model;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

/**
 * 文档分块 — 对应 vector_store 表中归属于某个文档的一条分块。
 *
 * <p>非 JPA 实体：分块由 Spring AI 的 PgVectorStore 写入，本类只是其只读投影。
 */
@Data
public class DocumentChunk implements Serializable {

    /** 分块 ID（vector_store 主键） */
    private UUID id;

    /** 分块序号（vector_store.metadata.chunkIndex） */
    private Integer chunkIndex;

    /** 分块文本内容 */
    private String content;
}
