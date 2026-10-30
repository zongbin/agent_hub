/**
 * SuperAgent 模块 — 超级智能体运行时与任务编排。
 *
 * <ul>
 *   <li>{@code controller}  — 智能体入口接口（SSE 流式执行）</li>
 *   <li>{@code service}     — 任务生命周期：创建智能体实例 → 执行 → 推送结果</li>
 *   <li>{@code superagent}  — {@code SuperManus} 超级智能体（思考-行动循环）</li>
 *   <li>{@code base}        — 代理基类：{@code BaseAgent} / {@code ReActAgent} / {@code ToolCallAgent}</li>
 *   <li>{@code tool}        — 智能体可调用的工具（如终止工具）</li>
 *   <li>{@code enums}       — 智能体执行状态枚举</li>
 *   <li>{@code config}      — 工具注册配置</li>
 * </ul>
 *
 * <p>当前形态为 ReAct 式工具调用循环（每步结果经 SSE 推送）；
 * 后续与 Python LangGraph 微服务（Plan-and-Execute）并行演进。
 */
package com.leon.agenthub.agent;
