package com.leon.agenthub.agent.model;

import java.io.Serializable;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Data;

/**
 * 智能体会话实体 — 对应 {@code agent_conversations} 表。
 *
 * <p>与 {@code qa_conversations} 结构一致但物理隔离：智能体消息要存步骤轨迹，
 * RAG 问答消息要存引用来源与反馈，混在一张表会产生大量互为 NULL 的列，
 * 且会污染问答页的历史会话列表。
 */
@Data
@Entity
@Table(name = "agent_conversations")
public class AgentConversation implements Serializable {

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
