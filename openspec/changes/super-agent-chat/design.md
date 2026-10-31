# Design

## Context

见 proposal.md 的 Why。此处只记录影响技术选型的现状与约束。

**后端现状**

- [BaseAgent.java](../../backend/src/main/java/com/leon/agenthub/agent/base/BaseAgent.java) 直接持有 `SseEmitter`，在 `run()` 里用 `CompletableFuture.runAsync` 起线程，逐轮 `emitter.send(String)` 推送 `"Step N: <结果>"`。agent 运行时与 HTTP 传输层耦合。
- [ToolCallAgent.java](../../backend/src/main/java/com/leon/agenthub/agent/base/ToolCallAgent.java) 的 `think()` 内部能拿到三样东西，但都只写进日志、没有外传：模型的思考文本、工具调用列表（名称 + 参数）、工具执行结果。
- `SuperAgentService` 每次调用 `new SuperManus(...)`，实例内的 `messageList` 用完即弃——多轮上下文在架构上不存在。
- `agent_tasks` / `agent_steps` 两张表在 schema.sql 中已定义，但无对应实体，状态取值 `PENDING/RUNNING/PAUSED/COMPLETED/FAILED` 与 `PENDING/RUNNING/COMPLETED/SKIPPED/FAILED`。
- `qa` 模块已有一套成熟的多轮会话持久化：`qa_conversations`（UUID 字符串主键 + user_id + title）、`qa_messages`（conversation_id + role + content）、`DatabaseChatMemory`（实现 Spring AI `ChatMemory` 接口）。
- `QaController` 的 SSE 写法是项目内的既有范式：`SseEmitter` + `emitter.send(SseEmitter.event().name("delta").data(vo))`，事件名固定，载荷是 VO。

**前端现状**

- `api/qa.ts` 已实现 `fetch` + `ReadableStream` 的 SSE 解析（含 `event:` / `data:` 行拆分、以 `\n\n` 分块），是可直接参照的范式。
- `/qa` 与 `/agent` 两个路由当前指向同一个 `views/QAM/index.vue`。`MainLayout` 的导航里「超级智能体」项已存在并指向 `/agent`。
- `views/QAM/index.vue` 已有会话列表、消息气泡、引用来源 chip、反馈按钮的完整实现。

**项目规约约束**

- CLAUDE.md 要求：Controller 只转发 + 包 `R`；业务与事务在 Service；实体 ↔ DTO/VO 映射放 `converter/`；状态常量一律用 `enums` 包下的枚举类；分页 DTO 继承 `PageQuery`。
- 技术设计 §7.3 已定义 `agent.plan` / `agent.step.start` / `agent.step.progress` / `agent.step.complete` / `agent.step.skip` / `agent.complete` / `agent.error` 七个事件名。CLAUDE.md 的「API Conventions」一节也复述了这套事件名。

## Goals / Non-Goals

**Goals:**

- 打通 `/agent/manus/chat` 从前端页面到 agent 运行时的完整链路，事件结构化。
- 智能体具备多轮对话能力，会话按用户隔离并可回看。
- agent 运行时与 HTTP/SSE 传输层解耦，未来可换 LangGraph 或别的传输。
- 复用 `qa` 模块已验证的会话持久化与 SSE 解析范式，不另造轮子。

**Non-Goals:**

- 不引入 `knowledge_search` / `document_read` 等知识库工具（需要 pgvector 召回与权限过滤下推，是独立的一块）。
- 不实现 Plan-and-Execute 的 `agent.plan` 事件与规划器，不实现 `agent.step.skip`（跳过语义来自 HITL 人工确认，本次不做）。
- 不落库 `agent_tasks` / `agent_steps` 的任务级审计——本次只做会话与消息持久化。
- 不改动 `qa` 模块的问答链路与既有接口。

## Decisions

### D1：GET 改 POST，请求体携带 `conversationId`

**选择**：`POST /agent/manus/chat`，请求体 `{ message, conversationId }`。

**理由**：

1. 任务描述是长文本（"分析一下我们和竞品 X 的差异并出一份对比报告"这类），URL 长度不可靠。`QaController.ask` 已经是 POST + `QaRequest`。
2. 需要携带 `conversationId` 支撑多轮，query param 会让签名越来越长。
3. **关键约束**：`EventSource` 无法设置 `Authorization` 头。本项目所有接口走 JWT Bearer，前端只能用 `fetch` + `ReadableStream` 手写 SSE 解析（`api/qa.ts` 已这么做）。既然前端必须用 `fetch`，用 GET 也拿不到 `EventSource` 的便利，反而要额外处理 query 编码。

**备选方案**：保持 GET + query param。可行但长文本不可靠，且与 `qa` 模块的 POST 范式不一致。

**影响**：这是 **BREAKING** 变更。当前无任何前端代码调用该接口（已 grep 确认），所以无存量调用方需要迁移。

### D2：agent 运行时用回调接口解耦 SSE

**选择**：定义 `AgentEventListener` 接口（`onStepStart` / `onStepProgress` / `onStepComplete` / `onComplete` / `onError`），`BaseAgent` 只依赖该接口；`SuperAgentService` 实现它并把回调翻译成 SSE 事件。

**理由**：`BaseAgent` 现在直接 `new SseEmitter(...)` 并 `send(String)`，导致两个问题——(a) 传输格式写死在 agent 基类里，换协议要改基类；(b) `emitter.send(String)` 只能推纯文本，前端拿不到结构化的步骤序号、工具名、参数。解耦后 `BaseAgent` 是纯运行时，`SuperAgentService` 是传输适配层。

**备选方案**：(a) 保留 `SseEmitter`，把 `send(String)` 换成 `send(SseEmitter.event().name(...).data(vo))`——改动最小，但 SSE 事件名会渗进 agent 基类，且 `BaseAgent` 仍持有 Web MVC 类型；(b) 用 Spring 的 `ApplicationEventPublisher` 发布领域事件——需要额外的 `@EventListener` 桥接层，对于单一消费者是过度设计。选回调接口，因为它就是一对一的场景。

**事件类型定义**：新增枚举 `AgentEventType`（`agent/enums/AgentEventType.java`），值为 `STEP_START` / `STEP_PROGRESS` / `STEP_COMPLETE` / `COMPLETE` / `ERROR`，各自携带技术设计 §7.3 对应的事件名常量。按 CLAUDE.md「状态常量一律用枚举类」与「枚举名即接口与前端使用的字符串」约定，前端收到的事件名由服务端枚举下发，不在前端硬编码字符串。

### D3：会话存储新建 `agent_conversations` / `agent_messages` 两张表，不复用 qa 表

**选择**：新增两张表 + 实体 + Repository + Converter。

**理由**：直接复用 `qa_conversations` / `qa_messages` 看似省事，实则有两个问题——(a) 智能体消息与 RAG 问答消息的语义不同（智能体消息要存步骤轨迹，RAG 消息要存 `sources` 引用来源与 `feedback`），塞进同一张表会产生大量互为 NULL 的列；(b) `qa` 模块的会话列表是给「智能问答」页用的，混入智能体会话会污染问答页的历史记录。

**表结构**：

```
agent_conversations
  id          VARCHAR(64) PRIMARY KEY   -- UUID 字符串，与 qa_conversations 对齐
  user_id     BIGINT       NOT NULL
  title       VARCHAR(200) NOT NULL     -- 取首问截断
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()

agent_messages
  id              BIGSERIAL PRIMARY KEY
  conversation_id VARCHAR(64) NOT NULL
  role            VARCHAR(16) NOT NULL  -- USER / ASSISTANT
  content         TEXT        NOT NULL  -- 用户提问 / 智能体最终回答
  steps_json      JSONB                 -- 助手消息的步骤轨迹（事件序列）
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
```

`steps_json` 用 JSONB 存步骤事件序列，前端回看历史会话时能还原步骤时间线，而不必重跑智能体。

**角色枚举**：新增 `AgentMessageRole`（`agent/enums/AgentMessageRole.java`），值 `USER` / `ASSISTANT`。不复用 `qa/enums/MessageRole`——跨模块依赖会让两个模块的枚举变更互相牵连。

**备选方案**：给 `qa_conversations` 加 `source` 字段做区分。省两张表，但污染问答页历史列表，且步骤轨迹无处安放。

**schema 双轨**：CLAUDE.md 说 schema 由 Hibernate `ddl-auto: update` 管理，但仓库里存在 `backend/src/main/resources/db/migration/schema.sql`（全量重建脚本，含 `CREATE TABLE agent_tasks` / `agent_steps`）。两张表都要同步写入 schema.sql，保持双轨一致。注意该脚本开头是 `DROP TABLE IF EXISTS`，是「全量重建」脚本，不能在有数据的库上随手执行。

### D4：多轮上下文的回填方式

**选择**：新建会话时把该会话的历史消息（用户提问 + 智能体最终回答）读出，作为 `UserMessage` / `AssistantMessage` 序列填充到 agent 的 `messageList` 开头，再追加本次提问。

**理由**：`BaseAgent.messageList` 本身就是 agent 维护的完整上下文（`ToolCallAgent` 依赖 `conversationHistory` 的连续性），把历史塞进这个列表是唯一能让模型真正「看到」前文的方式。`DatabaseChatMemory` 那套 `ChatMemory` + `MessageChatMemoryAdvisor` 是给 `ChatService` 用的 RAG 问答链路，`ToolCallAgent` 走的是自己维护 `messageList` 的路子，把 advisor 混进来会与 `chatOptions` 的代理式工具调用冲突。

**已知取舍**：回填只还原「用户提问 + 最终回答」的文本，不还原中间的工具调用与工具结果原始结构。这是有意的 MVP 简化——还原完整轨迹需要持久化 `ToolCall` / `ToolResponseMessage` 的结构化字段，成本与收益不匹配。代价是模型追问时看不到上一步具体调了哪个工具、返回了什么细节。

**备选方案**：(a) 完整还原工具调用轨迹——需要在 `agent_messages` 里额外存 `ToolCall` 与 `ToolResponseMessage` 的结构化数据，且跨轮重建 `messageList` 时要维持 Spring AI 的消息类型严格顺序（user → assistant(tool_calls) → tool → assistant…），出错风险高；(b) 只把上一次的最终回答作为一条 `UserMessage` 追加——最省事，但模型会误以为那是用户说的话。

### D5：SSE 事件载荷用 DTO，事件名由枚举下发

**选择**：`agent/dto/` 下定义事件载荷 DTO（如 `AgentStepEventDTO` / `AgentCompleteEventDTO` / `AgentErrorEventDTO`），Controller/Service 不手写 `HashMap`。

**理由**：CLAUDE.md 明确要求「返回给前端的结果用结构化对象（DTO/record），不要在 Controller 里手写 HashMap 拼装」，且实体/出参映射统一放 `converter/`。

**注意**：`@RequirePermission` 只能标在 Service 方法上。SSE 的完成回调运行在异步线程，此时 `SecurityContext` 为空——与 `QaService.saveAssistantMessage` 是同一约束。所以权限判定必须在**进入异步之前**（Service 的同步入口方法）完成，落库的异步方法不能再带该注解。

### D6：前端新增独立页面，复用 `api/qa.ts` 的 SSE 解析模式

**选择**：`frontend/src/api/agent.ts` + `frontend/src/views/Agent/index.vue`，`/agent` 路由指向新页面。

**理由**：用户已确认要独立页面。`api/qa.ts` 的 `fetch` + `ReadableStream` 解析逻辑（`\n\n` 分块、`event:` / `data:` 行拆分）已被验证可用，`api/agent.ts` 照此实现，但事件名与载荷结构不同。

**消息模型**：智能体的一条助手消息 = 最终回答文本 + 步骤轨迹数组。前端渲染为「正文 + 可折叠步骤时间线」，步骤轨迹来自本次会话实时收到的事件序列，或回看历史时来自 `agent_messages.steps_json`。

**样式**：复用 `views/QAM/index.vue` 的布局类名（`qa-layout` / `chat-panel` / `msg-bubble` / `sources-chip` 等）与 CSS 变量（`--surface-soft` / `--border` / `--header-height`），保持视觉一致。

## Risks / Trade-offs

- **[SSE 事件名与前端硬编码不一致]** → 事件名由服务端 `AgentEventType` 枚举随载荷下发，前端按枚举值分发，不写裸字符串。

- **[异步线程落库时无登录态]** → 权限在 Service 同步入口用 `@RequirePermission` 判定；异步落库方法不标该注解，用户 ID 在入口处取出后作为普通参数传下去（与 `QaService.saveAssistantMessage` 同款处理）。

- **[多轮上下文丢失工具细节]** → D4 已记录为 MVP 取舍。若后续发现模型追问质量不足，可扩展 `agent_messages` 增加工具轨迹列，`steps_json` 已预留了同方向的存储位。

- **[智能体步骤数不可控导致 SSE 超时]** → `BaseAgent` 的 `SseEmitter` 超时是 300 秒，`SuperManus` 的 `maxSteps` 是 10。改为事件驱动后，超时判定逻辑需从 `BaseAgent` 移到 `SuperAgentService`，沿用同样的 300 秒。

- **[`ddl-auto: update` 与 schema.sql 双轨不一致]** → 两张新表同时写入 schema.sql；schema.sql 的全量重建语义保持不变，不在本次改动其 DROP 段落。

- **[`agent_conversations` 与既有 `agent_tasks` 语义相近易混淆]** → 命名上明确区分：`*_conversations` 是多轮对话容器，`agent_tasks` 是任务级记录（本次不启用）。在 `package-info.java` 或类注释中写明。

## Migration Plan

1. 合并并部署后端：`ddl-auto: update` 自动创建两张新表（空表，无数据迁移）。
2. 部署前端：`/agent` 路由切换到新页面。`/qa` 路由与问答页完全不变。
3. 无存量数据需要迁移——`/agent/manus/chat` 此前无任何调用方。
4. 回滚：前端把 `/agent` 路由改回 `views/QAM/index.vue` 即可；后端新增的表留着不影响，两张新表无外键依赖。

## Open Questions

- 智能体最终回答是否需要像 RAG 问答那样支持「引用来源」跳转？当前 agent 工具集不含知识库检索，没有引用可给。后续引入 `knowledge_search` 工具时再一并设计——这不影响本次的接口形态（`steps_json` 可承载 citations）。
