# Design

## Context

当前项目状态与约束（动机见 proposal.md - Why）：

- 文档向量已通过 `DocumentIndexListener` + `DocumentChunkRepository.insertSingle` 以原生 SQL 写入 `document_chunks` 表（`embedding` 列为 `vector(1024)`）。
- `PgVectorStoreConfig` 创建的是 Spring AI 自带的 `VectorStore`，指向独立的 `vector_store` 表，与实际数据所在的 `document_chunks` **脱节**——检索若走 `VectorStore` 会查空。
- `ChatService` 被 `//@Service` 注释，未注册为 Bean；`SpringAiConfig` 整体被 `//@Configuration` 注释，无 `@Primary` ChatModel。
- `application.yml` 同时配置了 `spring.ai.openai`（DeepSeek）与 `spring.ai.dashscope`（qwen + text-embedding-v3），存在两个 ChatModel 来源。
- `DashScopeEmbeddingService` 已可用（注入 `EmbeddingModel`），`DocumentChunkRepository` 目前只有写入方法，无检索方法。
- 前端路由已指向 `QAM/index.vue`，该页为占位实现，留有 SSE 对接 TODO。
- 基础设施：`@RequirePermission`、`R<T>`、`SecurityContextUtil`、`server.servlet.context-path: /api`。

## Goals / Non-Goals

**Goals:**
- 打通「提问 → 权限感知检索 → qwen 生成 → SSE 流式返回 → 反馈」的完整问答链路。
- 将对话与向量统一到阿里云 DashScope，移除 DeepSeek 依赖。
- 检索与文档列表/详情共用同一套权限判定，避免越权与不一致。

**Non-Goals:**
- 多轮对话上下文记忆（MVP 仅单轮）。
- SuperAgent 复杂任务分解（属于 `agent` 模块 / M2 里程碑）。
- 重排序模型（rerank）与意图分类模型（MVP 可用规则或直接检索，后续再加）。
- 引用内容的字段级脱敏、知识图谱。

## Decisions

### 决策 1：检索数据源 — 自定义原生 SQL 直查 `document_chunks`

**选择**：在 `DocumentChunkRepository` 新增原生 SQL 向量检索方法，直接对 `document_chunks` 表按余弦距离排序，并 JOIN `documents` 过滤权限。不使用 Spring AI `VectorStore`。

**理由**：
- 实际向量在 `document_chunks`，而 `PgVectorStoreConfig` 指向 `vector_store`，两者脱节。改造 `VectorStore` 指向 `document_chunks` 需要其表结构与 Spring AI 约定对齐（含 id/content/metadata 列），成本高且无法自然地做权限 JOIN。
- CLAUDE.md 明确要求「权限感知检索始终 JOIN `document_chunks` → `documents`」，自定义 SQL 才能完全掌控过滤条件。

**备选方案**：
- 重配 `PgVectorStore` 指向 `document_chunks`：可复用 Spring AI 抽象，但表结构不匹配、权限过滤需绕过框架，得不偿失。
- 迁移到 Milvus/Qdrant：MVP 向量量级远未到阈值，违背 pgvector 选型初衷。

### 决策 2：检索 SQL 参数化与向量转换

**选择**：查询向量用 `CAST(:queryVector AS vector)` 进行类型转换，所有参数（queryVector、userId、k）用 `@Param` 绑定。

**理由**：此前写入曾因 `:embedding::vector` 触发 Hibernate 命名参数解析错误（`::` 被并入参数名）。`CAST(... AS vector)` 语义等价且不触发解析歧义；参数绑定同时规避 SQL 注入。

### 决策 3：权限过滤统一到 SQL 谓词

**选择**：抽取 `DocumentController.hasDocumentAccess` 的规则为单一权限谓词（如 `PermissionService`），检索 SQL 的 WHERE 与文档列表/详情接口共用同一判定。

**理由**：避免「列表看得到、问答答不了」或反向的越权。权限规则：PUBLIC 全员可见；INTERNAL 需登录（非 GUEST）；CONFIDENTIAL 仅 ADMIN 或有 `doc_permissions` 授权的用户。

### 决策 4：LLM 供应商 — DeepSeek → 阿里云 DashScope qwen

**选择**：删除 `application.yml` 中的 `spring.ai.openai`（DeepSeek）整块，对话模型改用 `spring.ai.dashscope.chat`（`qwen-plus`），恢复 `ChatService` 的 `@Service`。

**理由**：
- 与 Embedding（`text-embedding-v3`）统一到单一平台，只需一个 `DASHSCOPE_API_KEY`，运维更简单。
- 删除 DeepSeek 后只剩一个 ChatModel Bean（`dashScopeChatModel`），`ChatClient.Builder` 可自动注入，无需 `@Primary`/`@Qualifier` 的二义处理。

**备选方案**：
- 保留 DeepSeek + `@Primary` 二义处理：增加配置复杂度，与「统一供应商」目标冲突。
- 生成用 qwen-plus，轻量意图分类可降级 qwen-turbo（成本控制，后续可选）。

### 决策 5：流式传输 — SseEmitter

**选择**：使用 Spring MVC `SseEmitter` 实现 SSE 流式返回，事件顺序为 `source`（引用来源）→ `delta`（答案增量）→ `done` / `error`。

**理由**：
- 项目为 Spring MVC 栈（非 WebFlux），`SseEmitter` 是原生、成熟的选择。
- 先推 `source` 再推 `delta`，让前端能在答案生成期间同步展示引用来源。

**备选方案**：
- `Flux<ServerSentEvent>`：适合 WebFlux 响应式栈，与当前 MVC 栈不匹配。
- `StreamingResponseBody`：更底层，需手动管理 SSE 格式，不如 `SseEmitter` 简洁。

### 决策 6：反馈存储

**选择**：新增 `qa_feedback` 表（Flyway V5），字段含 `qa_id`（或问答会话标识）、`user_id`、`useful`（布尔）、`created_at`。

**理由**：反馈用于后续评估答案质量，需持久化；逻辑简单，不需要引入独立评审服务。

## Risks / Trade-offs

- **[向量维度不一致]** → 若 query embedding 与 chunk 写入用的模型维度不同，余弦距离无意义。缓解：统一用 `text-embedding-v3` 且 `dimensions=1024`，检索前校验向量长度。
- **[SQL 注入]** → 原生 SQL 拼接危险。缓解：全部用 `@Param` 绑定，禁止字符串拼接。
- **[SSE 长连接超时]** → qwen 长回答可能超时。缓解：为 LLM 流式调用设置足够 `read-timeout`（参考 `super-agent.read-timeout-ms: 300000`），前端做好断流重连提示。
- **[token 成本]** → top-K 分块拼接过长会超预算。缓解：每段截断（如 500 字符）+ 总长上限。
- **[权限逻辑漂移]** → 检索与列表/详情权限规则分叉。缓解：统一权限谓词，单一实现多处复用。
- **[单一供应商]** → 对话与向量都依赖阿里云，存在单点。缓解：密钥走环境变量，留超时重试，接口抽象便于后续切换。

## Migration Plan

1. 修改 `application.yml`：移除 `spring.ai.openai`，`agenthub.llm` 指向 qwen-plus。
2. 恢复 `ChatService` 的 `@Service`，调整 `SpringAiConfig`（如需）。
3. 新增 `qa/` 模块：`QaController`、`QaService`、`KnowledgeRetriever`、DTO、反馈模型/仓库。
4. 扩展 `DocumentChunkRepository` 增加权限感知检索方法。
5. 新增 Flyway V5 迁移：`qa_feedback` 表。
6. 前端：新增 `api/qa.ts`，改造 `QAM/index.vue` 接入 SSE。
7. 回滚策略：后端/前端代码回滚到上一版本；Flyway V5 提供 undo 脚本。

## Open Questions

- 意图分类（简单问答 vs 复杂任务）在 MVP 是否必须？可先默认全部走简单问答，复杂任务留给 `agent` 模块。
- 重排序（rerank）是否在 MVP 引入？可先用余弦相似度 top-5，后续评估命中率再决定。
