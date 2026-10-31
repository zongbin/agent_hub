package com.leon.agenthub.agent.enums;

/**
 * 智能体会话消息角色 — 对应 {@code agent_messages.role} 列的取值。
 *
 * <p>约定：枚举名即数据库存储值与接口返回字符串，业务代码禁止再出现
 * {@code "USER"} 这类字符串字面量。
 */
public enum AgentMessageRole {

    /** 用户提问 */
    USER("用户"),

    /** 智能体回答 */
    ASSISTANT("智能体");

    /** 中文标签 */
    private final String label;

    AgentMessageRole(String label) {
        this.label = label;
    }

    /**
     * 中文标签。
     */
    public String getLabel() {
        return label;
    }
}
