package com.leon.agenthub.agent.dto;

import com.leon.agenthub.agent.enums.AgentEventType;

import lombok.Data;

/**
 * 智能体步骤事件载荷 — SSE {@code agent.step.start} / {@code agent.step.progress}
 * / {@code agent.step.complete} 三类事件的通用数据结构。
 *
 * <p>{@code eventType} 随载荷下发，前端据此区分事件类型，无需硬编码事件名字符串。
 */
@Data
public class AgentStepEventDTO {

    /** 事件类型 — 前端据此分发 */
    private AgentEventType eventType;

    /** 步骤序号（从 1 开始） */
    private Integer stepIndex;

    /** 步骤名称 — 本步选用的工具名，模型直接作答时为「回答」 */
    private String stepName;

    /** 进展描述 — 模型的思考内容或工具调用说明 */
    private String message;

    /** 工具名 */
    private String toolName;

    /** 工具执行结果 */
    private String toolResult;

    /**
     * 构造步骤开始事件。
     */
    public static AgentStepEventDTO ofStart(int stepIndex, String stepName) {
        AgentStepEventDTO dto = new AgentStepEventDTO();
        dto.setEventType(AgentEventType.STEP_START);
        dto.setStepIndex(stepIndex);
        dto.setStepName(stepName);
        return dto;
    }

    /**
     * 构造步骤进展事件。
     */
    public static AgentStepEventDTO ofProgress(int stepIndex, String message) {
        AgentStepEventDTO dto = new AgentStepEventDTO();
        dto.setEventType(AgentEventType.STEP_PROGRESS);
        dto.setStepIndex(stepIndex);
        dto.setMessage(message);
        return dto;
    }

    /**
     * 构造步骤完成事件。
     */
    public static AgentStepEventDTO ofComplete(int stepIndex, String toolName, String toolResult) {
        AgentStepEventDTO dto = new AgentStepEventDTO();
        dto.setEventType(AgentEventType.STEP_COMPLETE);
        dto.setStepIndex(stepIndex);
        dto.setToolName(toolName);
        dto.setToolResult(toolResult);
        return dto;
    }
}
