package com.leon.agenthub.agent.dto;

import lombok.Data;

/**
 * 智能体错误事件载荷 — SSE {@code agent.error} 事件。
 */
@Data
public class AgentErrorEventDTO {

    /** 错误描述 */
    private String message;

    public static AgentErrorEventDTO of(String message) {
        AgentErrorEventDTO vo = new AgentErrorEventDTO();
        vo.setMessage(message == null ? "智能体执行失败" : message);
        return vo;
    }
}
