package com.leon.agenthub.qa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 问答消息实体 — 对应 qa_messages 表。
 */
@Data
@Entity
@Table(name = "qa_messages")
public class ChatMemoryMessage implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属会话 ID（存储 Spring AI 的字符串 conversationId） */
    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;

    /** 角色：user / assistant */
    @Column(nullable = false, length = 16)
    private String role;

    /** 消息内容 */
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** 引用来源（assistant 消息，JSON 数组） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String sources;

    /** 反馈：1 有帮助 / 0 无帮助 / null 未反馈 */
    private Integer feedback;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
