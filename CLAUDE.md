# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**智知 AgentHub** — enterprise knowledge management platform for SMEs (50–500 users). Combines a document knowledge base with RAG-based Q&A and a Plan-and-Execute "SuperAgent" that autonomously decomposes complex tasks into tool-calling steps. Target: natural-language answers over scattered corporate documents, plus AI-generated reports/analyses in minutes.

Full product context: `doc/产品文档/企业知识管理Agent_PRD_MVP.md`
Full technical design: `doc/技术设计/智知AgentHub_技术设计文档_MVP.md`
Interactive prototype: `doc/产品文档/企业知识管理Agent_原型_MVP.html`

## Code Conventions

- **Java file naming**: 所有 Java 文件名必须使用**驼峰命名法（CamelCase）**，例如 `BusinessException.java`、`GlobalExceptionHandler.java`、`AgentHubApplication.java`。禁止使用下划线、连字符或全小写拼写（如 `business_exception.java` 是错误的）。此规则适用于本项目及所有后续 Java 项目。
- **状态常量一律用枚举类**：所有状态/类型常量必须定义在 `enums` 包下的枚举类里，禁止在代码、SQL 参数、接口出参中出现 `"PROCESSING"` 这类字符串字面量或 `0/1/2` 魔法数字。
  - 已有枚举：文档处理状态 `knowledge/enums/DocumentStatus`（PROCESSING / ACTIVE / FAILED / DELETED）、用户账号状态 `auth/enums/UserStatus`（ENABLED / DISABLED）、角色与密级 `permission/enums/UserRole`、`permission/enums/SecurityLevel`。
  - 约定：**枚举名即接口与前端使用的字符串**（Jackson 按枚举名序列化）；`code` 为数据库存储值，`label` 为中文标签。
  - 实体层保留数据库列的数值字段（如 `Document.statusCode`），并提供枚举读写方法（`getStatus()` / `setStatus(DocumentStatus)`），业务代码只操作枚举。
  - 查询参数直接用枚举类型接收（如 `DocumentQuery.status`），前端传枚举名；服务层负责转成 `code` 传给 Repository。

## 分层与事务规约（强制）

### 各层职责

| 层 | 允许做 | 禁止做 |
|----|--------|--------|
| **Controller**（`*/controller/`） | 接收并绑定 DTO 参数、调用 Service、包装 `R` 响应 | 注入 Repository、写业务判断、拼装 VO/Map 业务结构、开事务 |
| **Service**（`*/service/`） | 全部业务逻辑、权限判定、事务边界、编排多个 Repository | 把事务边界下放到 Controller |
| **Repository**（`*/repository/`） | 数据访问、声明式查询 | 业务判断、跨表编排 |
| **Converter**（`*/converter/`） | 实体 ↔ DTO/VO 映射 | 访问数据库、抛业务异常 |

细则：

- Controller 里**禁止出现业务代码**：不查库、不判断权限、不做状态流转、不手写 `HashMap` 拼装领域结果；实体转换统一放到 `converter/`（如 `DocumentConverter.toVO`）。
- 业务异常一律抛 `BusinessException` / 领域异常，由 `GlobalExceptionHandler` 统一转成 `R.fail(code, message)`；Service 与 Controller 都**不要 `return R.fail(...)`**（`R` 只用于 Controller 出参）。
- `@RequirePermission` 标在 **Service 方法**上（与注解自身文档一致），Controller 不重复标注。

### 事务规约

- **事务边界只在 Service 方法上声明**：`@Transactional(rollbackFor = Exception.class)`；纯查询可用 `@Transactional(readOnly = true)`。
- **Controller 上严禁标注 `@Transactional`**：自调用/非 Spring 代理调用会让事务失效，且事务语义应属于业务层。
- 一次业务操作的**多步写库必须在同一个事务方法内完成**，禁止拆到多个方法或散落在 Controller 里。例如「软删除文档 + 清理 `vector_store` 分块」必须原子完成，任一步失败整体回滚，避免出现「文档已标记删除但仍能被检索命中」的中间态。
- 事务方法内**不要做不可回滚的外部副作用**：发 HTTP、调用 LLM、写 MinIO、启动异步任务等。应放在事务提交之后（`@TransactionalEventListener(phase = AFTER_COMMIT)`、或由事件监听器异步处理）。
- **禁止自调用依赖事务**：同类内部方法直接调用不经过代理，`@Transactional` 失效；需要独立事务时拆到另一个 Bean。
- 跨表一致性依赖数据库事务，而非「先删 A 再删 B」的手工补偿顺序。

### 分页查询规约

- **所有分页查询参数 DTO 必须继承 `com.leon.agenthub.common.dto.PageQuery`**：统一 `page`（从 1 开始）、`size`（默认 20，上限 200），并提供 `getPageIndex()` / `getPageSize()` / `toPageRequest(sort)`。
- Service 层统一用 `query.getPageIndex()`、`query.getPageSize()` 或 `query.toPageRequest(sort)` 构造 `PageRequest`，**禁止各处手写 `page - 1`**（越界、空值都要在基类里兜住）。
- 分页响应统一为 `{list, total, page, size}`（如文档列表、审计日志），**不要直接把 Spring 的 `Page` 对象作为响应体**（序列化结构不稳定，且与前端约定不一致）。
- 前端分页参数命名统一为 `page` / `size`，与后端 `PageQuery` 保持一致。

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentQuery extends PageQuery {
    private String securityLevel;
}
```

### 正反例

反例（业务写在 Controller + 事务缺失）：

```java
@DeleteMapping("/{id}")
public R<Void> delete(@PathVariable Long id) {
    Document doc = documentRepository.findById(id).orElse(null);        // Controller 注入 Repository
    if (doc == null) return R.fail(404, "文档不存在");                   // 业务判断写在 Controller
    if (!documentAccessChecker.canAccess(...)) return R.fail(403, "…");   // 权限判定写在 Controller
    doc.setDeleted(1);
    documentRepository.save(doc);                                       // 无事务：save 与下一行不在同一事务
    vectorStoreService.deleteByDocumentId(id);
    return R.ok();
}
```

正例（业务与事务下沉到 Service）：

```java
// Controller —— 只做转发与包装
@DeleteMapping("/{id}")
public R<Void> delete(@PathVariable Long id) {
    documentService.delete(id);
    return R.ok();
}

// Service —— 业务逻辑 + 事务边界
@Transactional(rollbackFor = Exception.class)
@RequirePermission(resource = "DOCUMENT", action = "WRITE")
public void delete(Long id) {
    Document doc = documentRepository.findById(id)
            .orElseThrow(() -> new BusinessException(404, "文档不存在"));
    checkAccess(doc);
    doc.setDeleted(1);
    documentRepository.save(doc);
    vectorStoreService.deleteByDocumentId(id);   // 与软删除同一事务，失败整体回滚
}
```

### RAG 实现规约（Spring AI 优先）

项目里所有 **RAG 相关功能**（知识召回、查询改写、上下文组装、对话记忆、流式生成、引用溯源）
**必须优先使用 Spring AI 框架里已经封装好的能力**，禁止自己重写一套。

常用对应关系（Spring AI 1.0.x）：

| 能力 | 优先使用 |
|------|---------|
| 向量召回（含过滤表达式） | `VectorStoreDocumentRetriever` / `VectorStore.similaritySearch(SearchRequest)` |
| 查询改写 / 扩展 / 压缩 | `RewriteQueryTransformer`、`QueryExpander`、`QueryTransformer` |
| 上下文组装与提示词 | `PromptTemplate` / `Prompt` / `ChatClient` |
| 对话记忆 | `ChatMemory` + `MessageChatMemoryAdvisor` |
| 引用溯源 | `QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS` 等 Advisor 元数据 |
| 文档解析 | `spring-ai-tika-document-reader`、`spring-ai-markdown-document-reader` |

细则：

- 只有 **Spring AI 没有对应能力** 时才允许自定义实现，并在类注释中写明原因。
- 业务代码只负责「权限过滤 + 结果编排」（如 `DocumentAccessFilter`），不重复实现 Spring AI 已有的检索/组装逻辑。
- 权限过滤统一通过 `SearchRequest.filterExpression(...)` 下推到向量库，**禁止**先全量召回再在 Java 侧过滤。
- 返回给前端的结果用**结构化对象**（DTO/record），不要在 Controller 里手写 `HashMap` 拼装。

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Vue 3 + TypeScript + Element Plus + TailwindCSS 3 |
| Backend API | Spring Boot 3.x + Java 17, Hibernate + Spring Data JPA |
| ORM / DB | PostgreSQL 16 + pgvector extension (vector search inside the same DB as business data — this is a deliberate choice, not an oversight) |
| Cache / State / SSE | Redis 7 |
| File storage | MinIO (S3-compatible, self-hosted) |
| Agent orchestration | LangGraph (Python microservice, called over HTTP from Java backend) — 规划中，当前 SuperAgent 为 Java 侧 ReAct 实现 |
| LLM | 阿里云百炼 DashScope（对话 qwen-plus，嵌入 text-embedding-v3 / 1024 维） |
| Reverse proxy | Nginx 1.24 |
| Deployment | Docker Compose |

## Key Architectural Decisions

1. **pgvector over dedicated vector DB** — vectors live in PostgreSQL alongside business data. Permission filtering is pushed down into the vector store as a filter expression over chunk metadata (`securityLevel` / `documentId`), not by a SQL JOIN. Zero extra ops cost. HNSW index (target < 50ms query, < 50M vectors). Migrate to Milvus/Qdrant only if vector count exceeds ~1M.

2. **LangGraph as a separate Python service** — the SuperAgent (Planner + Executor + Tool Layer) runs in Python. The Java backend communicates with it over HTTP. This is intentional: LangGraph provides Plan-and-Execute, checkpointing, and streaming natively.

3. **Plan-and-Execute (not ReAct)** — the agent produces a full structured JSON plan *before* executing. Users see and can edit the plan. Steps are auditable and resumable. This is a requirement for enterprise use (predictability, traceability, intervention).

4. **HITL (Human-in-the-Loop)** — read tools (knowledge_search, document_read) auto-execute. Write tools (api_call) pause for user confirmation before running.

5. **Unified permission layer** — `@RequirePermission` annotation on service methods. JWT → role + department → resource security level check. 4 roles (ADMIN, EDITOR, VIEWER, GUEST) × 3 security levels (PUBLIC, INTERNAL, CONFIDENTIAL) + per-document grants in `doc_permissions`.

6. **Model routing for cost control** — 规划中：强模型（当前统一 qwen-plus）负责规划与内容生成；廉价模型（Qwen-mini、规则）用于意图分类与数据分析。Target: ~¥0.05–0.10 per 5-step agent task.

## Core Data Flows

**Document upload**: Multipart → MinIO → Tika/docx/pptx/xlsx parse → chunk (`agenthub.chunk.size` tokens) → Embedding → pgvector（Spring AI `PgVectorStore` 写入 `vector_store` 表，metadata 带 documentId/securityLevel/title/chunkIndex）+ 文档元信息在 `documents` → AI summary + tags via LLM。

**Q&A (simple question)**: Question → 查询改写（`RewriteQueryTransformer`）→ Embedding → pgvector top-K=5（权限过滤以 filter expression 下推）→ build prompt with citations → LLM stream (SSE) → answer + citations + feedback buttons.

**SuperAgent (complex task)**: Task → Task Router detects complexity → Planner LLM generates JSON plan → user confirms/modifies → Executor runs steps via LangGraph → each step calls a tool (knowledge_search, document_read, content_generation, data_analysis, api_call) → permission check per step → SSE progress → finalize → result + citations.

## Repository Structure (planned, from tech design)

```
agent_hub/
├── backend/                    # Spring Boot 3 + Java 17
│   └── src/main/java/com/company/agenthub/
│       ├── auth/               # JWT login, repository, security
│       ├── qa/                 # RAG pipeline: query processing, retrieval, generation
│       ├── knowledge/          # Document upload, parsing, chunking, indexing (repository pattern)
│       ├── agent/              # SuperAgent HTTP client, task/step tracking
│       ├── permission/         # @RequirePermission, RBAC, security levels, converters
│       ├── audit/              # AuditAspect, audit log recording
│       ├── connector/          # Feishu / WeCom document sync
│       └── common/             # LLM client, embedding service, converters, shared config
├── agent-service/              # Python LangGraph microservice
│   ├── planner/                # LLM plan generation
│   ├── executor/               # Step execution loop
│   ├── tools/                  # knowledge_search, document_read, content_generation, data_analysis, api_call
│   └── graphs/                 # LangGraph StateGraph definitions
├── frontend/                   # Vue 3 + TypeScript + Element Plus
│   └── src/views/
│       ├── Dashboard/
│       ├── QAM/                # Chat interface (simple + agent mode)
│       ├── Knowledge/          # Document list, upload, detail
│       └── Permission/         # Role/audit management
├── docker-compose.yml          # nginx + backend + postgres+pgvector + redis + minio
└── doc/                        # PRD, technical design, prototype (already present)
```

## Database Schema Management

- 数据库 schema 由 Hibernate 自动管理（`spring.jpa.hibernate.ddl-auto: update`），无需 Flyway 迁移脚本。
- 启动时 Hibernate 自动根据实体注解创建/更新表结构。
- **文档分块的唯一存储是 Spring AI 的 `vector_store` 表**（`PgVectorStore` + `initializeSchema(true)` 自维护），不再有独立的 `document_chunks` 表。
- `vector_store.embedding` 使用 `vector(1024)` — 维度由 `spring.ai.dashscope.embedding.options.dimensions` 决定，必须与嵌入模型输出一致（text-embedding-v3 = 1024）。
- 向量搜索使用余弦距离（`PgDistanceType.COSINE_DISTANCE`），配合 HNSW 索引（`m=16, ef_construction=64`）。
- 权限感知检索**不做 SQL JOIN**，而是把过滤表达式下推到向量库：分块 metadata 里存有 `documentId` / `securityLevel` / `title` / `chunkIndex`，过滤规则由 `DocumentAccessFilter` 统一构造（角色×密级 + `doc_permissions` 文档级授权）。禁止先全量召回再在 Java 侧过滤。
- 超级智能体的会话与步骤轨迹持久化在 `agent_conversations` / `agent_messages`（`steps_json`）；`agent_tasks` / `agent_steps` 属于 Plan-and-Execute 规划器，尚未启用。

## API Conventions

- All responses wrapped: `{"code": 0, "message": "success", "data": {...}}`
- Auth via `Authorization: Bearer {jwt}` header.
- SSE endpoints stream events: `agent.plan`, `agent.step.start`, `agent.step.progress`, `agent.step.complete`, `agent.step.skip`, `agent.complete`, `agent.error`.
- REST paths prefixed with `/api/` (see §7 of tech design for full list).

## MVP Scope Boundaries (do not implement in MVP)

- Knowledge graph, field-level redaction, IP whitelist, watermarking
- Multi-model provider abstraction (当前只有 DashScope 一套模型)
- Data-source permission inheritance
- Agent marketplace / custom orchestration
- Mobile app

## Milestones

- **M1 (weeks 1–4)**: Document upload + AI parsing + basic Q&A (≥100 docs, ≥70% answer accuracy)
- **M2 (weeks 5–8)**: Permission system + SuperAgent v1 (competitor analysis scenario, ≥80% completion)
- **M3 (weeks 9–12)**: Connectors + audit logs + production launch
