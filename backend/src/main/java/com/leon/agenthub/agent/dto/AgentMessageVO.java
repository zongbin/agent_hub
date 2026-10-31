package com.leon.agenthub.agent.dto;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.Data;

/**
 * 智能体消息视图对象 — 含步骤轨迹，前端回看历史会话时可还原步骤时间线。
 */
@Data
public class AgentMessageVO {

    private Long id;

    /** 消息角色：USER / ASSISTANT */
    private String role;

    /** 消息内容 */
    private String content;

    /** 步骤轨迹（助手消息） */
    private List<AgentStepEventDTO> steps;

    private OffsetDateTime createdAt;
}
