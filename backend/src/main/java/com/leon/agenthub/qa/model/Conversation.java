package com.leon.agenthub.qa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 问答会话实体 — 对应 qa_conversations 表。
 */
@Data
@Entity
@Table(name = "qa_conversations")
public class Conversation implements Serializable {

    /** 会话 ID（UUID 字符串，由应用生成） */
    @Id
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    /** 所属用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 会话标题（取首问截断） */
    @Column(nullable = false, length = 200)
    private String title;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
