package com.leon.agenthub.knowledge.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 文档实体 — 对应 documents 表。
 */
@Data
@Entity
@Table(name = "documents")
@Where(clause = "deleted = 0")
public class Document implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 文档标题 */
    @Column(nullable = false, length = 512)
    private String title;

    /** MinIO 对象键 / 本地文件路径标识 */
    @Column(name = "file_key", nullable = false, length = 256)
    private String fileKey;

    /** 文件类型：docx/pdf/pptx/xlsx/txt/md */
    @Column(name = "file_type", nullable = false, length = 16)
    private String fileType;

    /** 文件大小（字节） */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /** 安全密级：PUBLIC/INTERNAL/CONFIDENTIAL */
    @Column(name = "security_level", nullable = false, length = 16)
    private String securityLevel;

    /** 上传者 ID */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 所属部门 */
    @Column(length = 64)
    private String department;

    /** AI 生成摘要 */
    @Column(columnDefinition = "text")
    private String summary;

    /** AI 生成标签 — Hibernate 6 原生 PostgreSQL text[] 支持 */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;

    /** 状态：1 正常 0 删除 2 处理中 */
    @Column(nullable = false)
    private Integer status;

    /** 分块数量 */
    @Column(name = "chunk_count")
    private Integer chunkCount;

    /** 错误信息（处理失败时） */
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(nullable = false)
    private Integer deleted = 0;
}
