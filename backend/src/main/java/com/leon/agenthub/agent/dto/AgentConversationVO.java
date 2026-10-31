package com.leon.agenthub.agent.dto;

import java.time.OffsetDateTime;

import lombok.Data;

/**
 * 智能体会话视图对象。
 */
@Data
public class AgentConversationVO {

    private String id;

    private String title;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
