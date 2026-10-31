package com.leon.agenthub.agent.model;

import java.io.Serializable;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.Data;

/**
 * 智能体会话消息实体 — 对应 {@code agent_messages} 表。
 *
 * <p>{@code stepsJson} 以 JSONB 保存本轮助手消息的步骤轨迹，
 * 前端回看历史会话时可据此还原步骤时间线，无需重跑智能体。
 */
@Data
@Entity
@Table(name = "agent_messages")
public class AgentMessage implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属会话 ID */
    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;

    /** 消息角色：USER / ASSISTANT（取值见 {@link com.leon.agenthub.agent.enums.AgentMessageRole}） */
    @Column(nullable = false, length = 16)
    private String role;

    /** 消息内容 — 用户提问或智能体最终回答 */
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** 步骤轨迹（助手消息，JSON 数组） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "steps_json", columnDefinition = "jsonb")
    private String stepsJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
