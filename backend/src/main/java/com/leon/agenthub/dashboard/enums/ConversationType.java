package com.leon.agenthub.dashboard.enums;

/**
 * 工作台会话类型 — 区分智能问答会话与超级智能体会话。
 *
 * <p>两张会话表物理隔离（{@code qa_conversations} / {@code agent_conversations}），
 * 但在工作台的「最近会话」列表里需要合并展示，因此用本枚举统一标识来源。
 *
 * <p>约定：枚举名即接口与前端使用的字符串（Jackson 按枚举名序列化）。
 */
public enum ConversationType {

    /** 智能问答会话 */
    QA("智能问答"),

    /** 超级智能体会话 */
    AGENT("超级智能体");

    /** 中文标签 */
    private final String label;

    ConversationType(String label) {
        this.label = label;
    }

    /**
     * 中文标签。
     */
    public String getLabel() {
        return label;
    }
}
