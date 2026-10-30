package com.leon.agenthub.agent.enums;

/**
 * 超级智能体执行状态。
 *
 * <p>状态流转：{@code IDLE} → {@code RUNNING} → {@code FINISHED} / {@code ERROR}。
 */
public enum AgentStateEnum {

    /** 空闲状态 */
    IDLE,

    /** 运行中状态 */
    RUNNING,

    /** 已完成状态 */
    FINISHED,

    /** 错误状态 */
    ERROR
}
