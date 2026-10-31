package com.leon.agenthub.agent.base;

import com.leon.agenthub.agent.dto.AgentStepEventDTO;

/**
 * 智能体事件监听器 — agent 运行时与传输层之间的唯一接口。
 *
 * <p>agent 运行时只负责「发生了什么」，不关心事件如何送达客户端。
 * 由 Service 层实现本接口，把运行时事件翻译成 SSE 事件（或其它传输协议）。
 */
public interface AgentEventListener {

    /**
     * 步骤开始。
     *
     * @param event 步骤事件载荷（含步骤序号与步骤名称）
     */
    void onStepStart(AgentStepEventDTO event);

    /**
     * 步骤进展 — 模型产生思考内容或发起工具调用。
     *
     * @param event 步骤事件载荷（含步骤序号与进展描述）
     */
    void onStepProgress(AgentStepEventDTO event);

    /**
     * 步骤完成 — 工具执行完毕。
     *
     * @param event 步骤事件载荷（含步骤序号与工具执行结果）
     */
    void onStepComplete(AgentStepEventDTO event);

    /**
     * 全部步骤完成。
     *
     * @param finalResult 最终结果文本
     */
    void onComplete(String finalResult);

    /**
     * 执行出错。
     *
     * @param errorMessage 错误描述
     */
    void onError(String errorMessage);
}
