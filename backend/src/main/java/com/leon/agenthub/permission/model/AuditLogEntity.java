package com.leon.agenthub.permission.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 审计日志实体 — 对应 audit_logs 表。
 *
 * <p>记录用户操作（登录、文档访问、权限变更等），用于安全审计和合规追溯。
 */
@Data
@Entity
@Table(name = "audit_logs")
public class AuditLogEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 操作用户 ID */
    @Column(name = "user_id")
    private Long userId;

    /** 操作用户名（冗余存储，防关联丢失） */
    @Column(length = 64)
    private String username;

    /** 操作类型（LOGIN/VIEW/EDIT/DELETE/GRANT/REVOKE/DENIED 等） */
    @Column(nullable = false, length = 64)
    private String action;

    /** 目标资源描述 */
    @Column(length = 128)
    private String resource;

    /** 操作详情（JSON） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail", columnDefinition = "jsonb")
    private Map<String, Object> detail;

    /** 用户 IP 地址 */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /** 操作是否成功：1 成功，0 失败/拒绝 */
    @Column(nullable = false)
    private Integer success;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
