package com.leon.agenthub.knowledge.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 文档分块实体 — 对应 document_chunks 表。
 *
 * <p>注意：embedding 列使用 pgvector 的 vector(1024) 类型，
 * 通过自定义原生 SQL 写入（CAST(... AS vector) 强制转换），标记为 @Transient 不参与 JPA 自动映射。
 */
@Data
@Entity
@Table(name = "document_chunks")
public class DocumentChunk implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属文档 ID */
    @Column(name = "document_id", nullable = false)
    private Long documentId;

    /** 分块序号 */
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    /** 分块文本内容 */
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** token 数估算 */
    @Column(name = "token_count", nullable = false)
    private Integer tokenCount;

    /** 向量（通过原生 SQL 写入，不参与 JPA 自动映射） */
    @Transient
    private String embedding;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
