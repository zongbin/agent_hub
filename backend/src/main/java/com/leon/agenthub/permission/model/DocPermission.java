package com.leon.agenthub.permission.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 文档级授权实体 — 对应 doc_permissions 表。
 *
 * <p>用于机密文档的细粒度授权，支持按用户或按部门授权。
 */
@Data
@Entity
@Table(name = "doc_permissions")
public class DocPermission implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 文档 ID */
    @Column(name = "document_id", nullable = false)
    private Long documentId;

    /** 被授权用户 ID */
    @Column(name = "grantee_id", nullable = false)
    private Long granteeId;

    /** 权限类型：READ / WRITE */
    @Column(nullable = false, length = 16)
    private String permission;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
