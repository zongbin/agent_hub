# Proposal

## Why

超级智能体的后端入口 [AgentController.java:34](../../backend/src/main/java/com/leon/agenthub/agent/controller/AgentController.java#L34) 已经存在，但整条链路是断的：后端只吐明文字符串（`"Step 1: ..."`），前端 `api/agent.ts` 根本不存在，`/agent` 路由指向的仍是 RAG 问答页 `/qa`。用户在界面上看不到「超级智能体」与「智能问答」的任何区别，智能体能力无法被使用。

同时，当前后端每次调用都新建 `SuperManus` 实例（[SuperAgentService.java:41](../../backend/src/main/java/com/leon/agenthub/agent/service/SuperAgentService.java#L41)），运行期上下文随实例丢弃，多轮追问无从谈起。

本变更把这条链路真正打通：后端输出结构化 SSE 事件并带上鉴权用户上下文，前端新增独立的超级智能体页面消费这些事件，并把对话历史落库以支持多轮上下文。

## What Changes

### 后端

- **BREAKING**：`GET /agent/manus/chat?message=xxx` 改为 `POST /agent/manus/chat`，请求体为 `{ message, conversationId }`。改为 POST 的原因有二：任务描述可能超出 URL 长度限制；且需要携带会话 ID，前端必须用 `fetch` + `ReadableStream` 解析 SSE（`EventSource` 无法携带 `Authorization` 头）。
- SSE 输出由明文字符串改为结构化事件，事件名沿用技术设计 §7.3 已定义的集合：`agent.step.start`、`agent.step.progress`、`agent.step.complete`、`agent.complete`、`agent.error`。`agent.plan` 与 `agent.step.skip` 属于 Plan-and-Execute / HITL 语义，当前 ReAct 运行时不产生，本次不实现。
- `BaseAgent` / `ToolCallAgent` 引入事件监听回调，暴露「思考内容」「工具调用」「工具结果」三个中间节点；SSE 发射器的创建与事件写出下沉到 `SuperAgentService`，agent 运行时不再直接持有 HTTP 概念。
- 控制器解析当前登录用户 ID 并传入 Service；Service 入口加 `@RequirePermission(resource = "AGENT", action = "READ")`。
- 新增 `agent_conversations` / `agent_messages` 两张表与对应实体、Repository、Converter、DTO/VO，按用户隔离会话数据。
- 多轮上下文：同一会话的后续请求会把历史（用户提问 + 最终回答）回填进 agent 的消息上下文后再执行。

### 前端

- 新增 `frontend/src/api/agent.ts`：`fetch` + `ReadableStream` 解析 SSE，事件分发与会话增删查。
- 新增 `frontend/src/views/Agent/index.vue`：独立页面，与 `/qa` 问答页分离。会话列表在左，右侧消息区对每条助手消息渲染**可折叠的步骤时间线**（步骤序号、名称、进展文案、工具执行结果），而不是把步骤流水当作正文。
- `/agent` 路由由 `views/QAM/index.vue` 改为 `views/Agent/index.vue`（[router/index.ts:52](../../frontend/src/router/index.ts#L52)）；`MainLayout` 的导航项已存在，无需改动。

### 不在本次范围

- 知识库工具（`knowledge_search` / `document_read`）——当前 agent 只有网页类工具，无法检索企业知识库。
- `agent_tasks` / `agent_steps` 的任务级持久化与审计。
- Plan-and-Execute 规划器、HITL 人工确认。

## Capabilities

### New Capabilities

- `super-agent-chat`: 超级智能体的对话能力——流式对话入口、结构化执行事件推送、鉴权用户上下文、多轮上下文与对话历史持久化，以及配套的前端页面。

### Modified Capabilities

无。现有 `user-auth` 规格的鉴权行为不变（本次只是让 agent 入口复用同一套 JWT 校验链路）。

## Impact

**后端**

| 文件 | 变更 |
|------|------|
| `agent/controller/AgentController.java` | GET→POST，入参改 DTO，取当前用户 ID |
| `agent/service/SuperAgentService.java` | 负责 SSE 发射器、事件写出、会话解析与消息落库 |
| `agent/base/BaseAgent.java` | 移除内建 SSE 逻辑，改为事件回调驱动 |
| `agent/base/ToolCallAgent.java` | 上报思考 / 工具调用 / 工具结果三类中间事件 |
| `agent/base/ReActAgent.java` | 无需改动 |
| `agent/model/`、`agent/repository/`、`agent/dto/`、`agent/converter/`、`agent/enums/` | 新增会话与消息的实体、仓储、DTO/VO、转换器、枚举 |
| `resources/db/migration/schema.sql` | 新增 `agent_conversations`、`agent_messages` 建表语句（与 `ddl-auto: update` 双轨） |

**前端**

| 文件 | 变更 |
|------|------|
| `frontend/src/api/agent.ts` | 新增 |
| `frontend/src/views/Agent/index.vue` | 新增 |
| `frontend/src/router/index.ts` | `/agent` 路由指向新页面 |

**依赖与配置**：不引入新依赖。`ChatModel`（DashScope）沿用现有配置。

**风险点**

- `agent_tasks` / `agent_steps` 两张表在 [schema.sql:118](../../backend/src/main/resources/db/migration/schema.sql#L118) 已存在但无实体，本次新增的表名与之不冲突。
- SSE 结束时的消息落库运行在异步线程，无登录态，不能带 `@RequirePermission`（与 `QaService.saveAssistantMessage` 同一约束）。
- 智能体多轮上下文采用「用户提问 + 最终回答」的文本回放，不还原工具调用与工具结果的原始结构——这是 MVP 级的取舍，需在设计文档中记录。
