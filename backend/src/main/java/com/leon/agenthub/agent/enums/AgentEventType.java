package com.leon.agenthub.agent.enums;

/**
 * 智能体执行事件类型 — 超级智能体 SSE 事件名的唯一来源。
 *
 * <p>约定：
 * <ul>
 *   <li>{@code code} 为 SSE 事件名（对应技术设计 §7.3），由服务端随事件载荷下发</li>
 *   <li>枚举名即接口返回 / 前端使用的字符串（JSON 序列化按枚举名输出）</li>
 *   <li>前端按枚举值分发事件，禁止硬编码事件名字符串</li>
 * </ul>
 *
 * <p>未纳入的事件：{@code agent.plan} 属于 Plan-and-Execute 规划器语义、
 * {@code agent.step.skip} 属于 HITL 人工确认语义，当前 ReAct 运行时不产生。
 */
public enum AgentEventType {

    /** 步骤开始 — 数据含步骤序号与步骤名称 */
    STEP_START("agent.step.start", "步骤开始"),

    /** 步骤进展 — 数据含步骤序号与进展描述 */
    STEP_PROGRESS("agent.step.progress", "步骤进展"),

    /** 步骤完成 — 数据含步骤序号与工具执行结果 */
    STEP_COMPLETE("agent.step.complete", "步骤完成"),

    /** 执行完成 — 数据含最终结果 */
    COMPLETE("agent.complete", "执行完成"),

    /** 执行出错 — 数据含错误描述 */
    ERROR("agent.error", "执行出错");

    /** SSE 事件名 */
    private final String code;

    /** 中文标签 */
    private final String label;

    AgentEventType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * SSE 事件名。
     */
    public String getCode() {
        return code;
    }

    /**
     * 中文标签。
     */
    public String getLabel() {
        return label;
    }
}
