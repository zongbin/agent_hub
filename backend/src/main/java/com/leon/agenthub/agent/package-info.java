/**
 * SuperAgent 模块 — 与 Python LangGraph 微服务的 HTTP 桥接。
 *
 * <ul>
 *   <li>{@code controller}  — 任务提交 / 计划确认 / SSE 进度流接口</li>
 *   <li>{@code service}     — 任务生命周期：创建 → 计划确认 → 执行 → 汇总</li>
 *   <li>{@code client}      — agent-service HTTP 客户端（OkHttp / WebClient）</li>
 *   <li>{@code model}       — Task / Step 领域模型（对应 DB 表 agent_tasks / agent_steps）</li>
 *   <li>{@code dto}         — TaskRequest / PlanResponse / StepEvent</li>
 *   <li>{@code config}      — agent-service 地址、超时配置</li>
 * </ul>
 *
 * <p>Plan-and-Execute 模式：Planner 生成 JSON 计划 → 用户确认 → Executor 逐步执行。
 * 写操作（api_call）触发 HITL 暂停，读操作自动执行。
 */
package com.leon.agenthub.agent;
