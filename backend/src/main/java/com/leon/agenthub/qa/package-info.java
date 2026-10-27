/**
 * Q&amp;A 模块 — RAG 检索增强生成流水线。
 *
 * <ul>
 *   <li>{@code controller}  — 问答接口（含 SSE 流式端点）</li>
 *   <li>{@code service}     — 意图分发：简单问题走 RAG，复杂任务路由到 agent 模块</li>
 *   <li>{@code rag}         — 查询处理 → Embedding → pgvector 检索 → ReRank → 生成</li>
 *   <li>{@code dto}         — QuestionRequest / AnswerResponse / Citation</li>
 *   <li>{@code config}      — 检索参数（topK、相似度阈值）配置类</li>
 * </ul>
 */
package com.leon.agenthub.qa;
