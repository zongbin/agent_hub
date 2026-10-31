# 智知 AgentHub — 技术设计文档

> **版本**：V2.0（对齐实现现状版）
> **日期**：2026-10-31
> **状态**：As-Built（按 `backend/` 与 `frontend/` 实际代码校对）
> **适用对象**：中小型企业（50–500 人）
>
> **文档定位**：本文档描述**系统当前实际如何工作**，而不是「打算如何工作」。
> 与早期设计稿的差异集中在 §12「与 V1 设计稿的差异」与 §13「已知技术债」两节。
> 产品视角的对应文档见 `doc/产品文档/企业知识管理Agent_PRD_MVP.md`。

---

## 目录

1. [项目概述](#1-项目概述)
2. [架构总览](#2-架构总览)
3. [技术栈](#3-技术栈)
4. [分层与编码规约](#4-分层与编码规约)
5. [模块详细设计](#5-模块详细设计)
   - [5.1 认证与安全](#51-认证与安全)
   - [5.2 权限体系](#52-权限体系)
   - [5.3 知识库](#53-知识库模块)
   - [5.4 向量存储](#54-向量存储)
   - [5.5 智能问答 RAG](#55-智能问答-rag)
   - [5.6 超级智能体](#56-超级智能体)
   - [5.7 审计](#57-审计)
   - [5.8 公共设施](#58-公共设施)
6. [数据库设计](#6-数据库设计)
7. [API 设计](#7-api-设计)
8. [SSE 事件契约](#8-sse-事件契约)
9. [前端设计](#9-前端设计)
10. [部署架构](#10-部署架构)
11. [测试现状与策略](#11-测试现状与策略)
12. [与 V1 设计稿的差异](#12-与-v1-设计稿的差异)
13. [已知技术债与风险登记](#13-已知技术债与风险登记)
14. [下一步优化方向](#14-下一步优化方向)

---

## 1. 项目概述

### 1.1 一句话定位

企业知识管理平台——文档知识库 + RAG 问答 + ReAct 工具调用智能体，
面向 50–500 人规模企业的统一知识入口。

### 1.2 核心痛点与当前解决度

| 痛点 | 方案 | 解决度 |
|------|------|--------|
| 资料分散 | 统一上传 + 语义检索 | 完整 |
| 重复提问 | RAG 问答 + 引用溯源 | 完整 |
| 写报告费时 | 超级智能体工具调用 | **部分（未接知识库）** |
| 越权访问 | 角色 × 密级 × 文档级授权 | **完整但实现分散** |
| 操作不可追溯 | 审计日志 | **部分（仅写操作）** |

### 1.3 工程规模

| 指标 | 数值 |
|------|------|
| 后端 Java 源文件 | 146 |
| 后端测试类 | 14（约 81 个 `@Test`） |
| 前端视图 | 12（Vue SFC） |
| 数据库表 | 9 张业务表 + 1 张 `vector_store` |
| Maven 依赖 | Spring Boot 3.3.4 / Spring AI 1.0.0-M6 / Spring AI Alibaba 1.0.0-M6.1 |

---

## 2. 架构总览

### 2.1 运行时视图

```
┌─────────────────────────────────────────────────────────────────┐
│                       前端层 (Vue 3 + TS)                         │
│  Dashboard │ QAM │ Agent │ Knowledge │ Permission │ Profile      │
│  axios(普通请求) + fetch/ReadableStream(SSE 解析)                │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP / SSE，Authorization: Bearer
┌──────────────────────────┴──────────────────────────────────────┐
│                     后端服务层 (Spring Boot 3.3 / Java 21)         │
│                                                                  │
│  ┌──────────┐ ┌──────────┐ ┌───────────┐ ┌──────────────┐      │
│  │  auth    │ │   qa     │ │ knowledge │ │    agent     │      │
│  │ JWT/用户 │ │ RAG 问答 │ │ 文档索引  │ │ ReAct 智能体 │      │
│  └────┬─────┘ └────┬─────┘ └─────┬─────┘ └──────┬───────┘      │
│       │            │              │               │              │
│  ┌────┴────────────┴──────────────┴───────────────┴──────┐      │
│  │  permission (@RequirePermission 切面 + DocumentAccess*)   │      │
│  │  audit     (@AuditLog 切面)                              │      │
│  │  common    (R / 异常 / 分页 / ChatService / Advisor)     │      │
│  └─────────────────────────────────────────────────────────┘      │
└──────────────┬────────────────────────────┬──────────────────────┘
               │                            │
   ┌───────────┴────────────┐   ┌───────────┴────────────────┐
   │  PostgreSQL 16 + pgvector│   │  本地文件系统              │
   │  9 张业务表 + vector_store│   │  ./data/docs/{docId}/...  │
   │  （PgVectorStore 自维护） │   │  ./data/agent/{file,pdf,…}│
   └──────────────────────────┘   └───────────────────────────┘
               │
   ┌───────────┴──────────────────────────┐
   │  阿里云百炼 DashScope                    │
   │  chat: qwen-plus   embedding: text-embedding-v3 (1024 维) │
   │  外部: SearchAPI.io (网页搜索，需 Key)    │
   └────────────────────────────────────────┘
```

### 2.2 模块边界

| 包 | 职责 | 关键入口 |
|----|------|---------|
| `auth` | JWT 签发/校验、用户资料、Spring Security 配置 | `AuthController` / `JwtAuthenticationFilter` |
| `permission` | 角色 × 密级 × 操作判定、文档级授权、审计落库 | `PermissionAspect` / `DocumentAccessChecker` / `DocumentAccessFilter` |
| `knowledge` | 上传、解析、分块、索引、文档 CRUD | `DocumentController` / `DocumentUploadService` / `DocumentIndexListener` |
| `qa` | 会话、检索、问答编排、反馈 | `QaController` / `QaService` / `RagRetriever` |
| `agent` | 智能体运行时、工具层、SSE 事件、会话持久化 | `AgentController` / `SuperAgentService` / `SuperManus` |
| `audit` | 审计注解与切面 | `AuditLogAspect` |
| `common` | 响应包装、异常、分页、ChatService、Advisor、线程池 | `R` / `GlobalExceptionHandler` / `PageQuery` |
| `connector` | **占位**，飞书/企微同步未实现 | `package-info.java` |

---

## 3. 技术栈

| 层级 | 技术 | 版本 | 实际说明 |
|------|------|------|---------|
| 前端 | Vue 3 + TypeScript | 3.5 / 5.6 | `<script setup>` + Composition API |
| 前端 | Element Plus / TailwindCSS | 2.8 / 3.4 | 组件库 + 原子化样式 |
| 前端 | Pinia / vue-router / axios / markdown-it | — | 状态、路由、HTTP、Markdown 渲染 |
| 前端 | Vite | 5.4 | `npm run build` 前置 `vue-tsc --noEmit` |
| 后端 | Spring Boot | 3.3.4 | |
| 后端 | Java | **21** | `pom.xml` `<java.version>21</java.version>`，`openjdk:21-slim` |
| ORM | Spring Data JPA / Hibernate | 6.x | `ddl-auto: update`，**非 MyBatis** |
| 数据库 | PostgreSQL + pgvector | 16 | 业务表与向量同库 |
| 向量 | spring-ai-pgvector-store | 1.0.0-M6 | `PgVectorStore`，HNSW + 余弦距离 |
| AI 框架 | Spring AI | 1.0.0-M6 | `RewriteQueryTransformer` / `VectorStoreDocumentRetriever` / `ChatMemory` |
| AI 框架 | Spring AI Alibaba | 1.0.0-M6.1 | DashScope `ChatModel` / `EmbeddingModel` |
| 解析 | spring-ai-tika-document-reader | 1.0.0-M6 | Apache Tika |
| 文档 | spring-ai-markdown-document-reader | 1.0.0-M6 | 已在 classpath |
| 认证 | jjwt + Spring Security | 0.12.6 / 6.3 | HS256 + BCrypt |
| PDF | iText core + font-asian | 9.1.0 | 智能体 PDF 工具（中文） |
| 爬虫 | jsoup | 1.19.1 | 智能体网页抓取工具 |
| 工具 | hutool-all | 5.8.46 | 文件 / HTTP 工具方法 |
| JSON Schema | victools jsonschema-generator | 4.37.0 | **显式锁版本**，dashscope-sdk 传递的 4.31.1 缺 `AnnotationHelper` 会导致启动期 `NoClassDefFoundError` |
| 接口文档 | Knife4j / springdoc | 4.5.0 | `/doc.html` |
| **未启用** | Redis | — | pom 与 application.yml 中依赖块均被注释 |
| **未启用** | MinIO / S3 SDK | — | 无依赖，存储为本地文件系统 |
| **未启用** | Flyway | — | `db/migration/schema.sql` 是**手工执行的全量重建脚本**，非迁移工具 |
| **未启用** | LangGraph / Python 服务 | — | 仓库中无 `agent-service` |

---

## 4. 分层与编码规约

### 4.1 分层职责

| 层 | 允许 | 禁止 |
|----|------|------|
| Controller (`*/controller/`) | 绑定 DTO、调 Service、包装 `R` | 注入 Repository、业务判断、拼 Map、开事务 |
| Service (`*/service/`) | 全部业务逻辑、权限判定、事务边界 | 把事务下放到 Controller |
| Repository (`*/repository/`) | 数据访问、声明式查询 | 业务判断、跨表编排 |
| Converter (`*/converter/`) | 实体 ↔ DTO/VO 映射 | 访问数据库、抛业务异常 |

### 4.2 事务规约

- 事务边界只在 Service 方法上：`@Transactional(rollbackFor = Exception.class)`。
- Controller 上**严禁** `@Transactional`。
- 一次业务的多步写库必须在同一事务内。典型案例：`DocumentService.delete` 把「软删除标记」与
  `vectorStoreService.deleteByDocumentId` 放进同一事务，避免「文档已删但仍可检索命中」的中间态。
- 事务方法内不做不可回滚的外部副作用（发 HTTP、调 LLM、写文件、起异步任务）。
  `DocumentUploadService` 通过 `TransactionSynchronizationManager.registerSynchronization(...).afterCommit()`
  在提交后才发布索引事件。
- 独立事务必须拆到另一个 Bean，否则同类自调用绕过代理。
  `DocumentIndexFailureHandler` 用 `REQUIRES_NEW` 标失败态即为此。

### 4.3 枚举规约

所有状态 / 类型常量用枚举类，枚举名即接口与前端使用的字符串，`code` 为数据库存储值。

| 枚举 | 取值 | 存储值 |
|------|------|--------|
| `knowledge/enums/DocumentStatus` | PROCESSING / ACTIVE / FAILED / DELETED | 2 / 1 / 0 / -1 |
| `auth/enums/UserStatus` | ENABLED / DISABLED | 1 / 0 |
| `permission/enums/UserRole` | ADMIN / EDITOR / VIEWER / GUEST | 同名 |
| `permission/enums/SecurityLevel` | PUBLIC / INTERNAL / CONFIDENTIAL | 0 / 1 / 2 |
| `permission/enums/PermissionAction` | READ / WRITE / DELETE | 同名 |
| `permission/enums/DocumentPermissionType` | READ / WRITE | 同名 |
| `agent/enums/AgentStateEnum` | IDLE / RUNNING / FINISHED / ERROR | — |
| `agent/enums/AgentEventType` | STEP_START / STEP_PROGRESS / STEP_COMPLETE / COMPLETE / ERROR | — |

### 4.4 分页规约

所有分页 DTO 继承 `common/dto/PageQuery`：`page`（1 起）、`size`（默认 20，上限 200），
提供 `getPageIndex()` / `getPageSize()` / `toPageRequest(sort)`。
响应统一为 `common/dto/PageResult` 的 `{list, total, page, size}`，不直接返回 Spring `Page`。

> ⚠️ **现存偏差**：`DocumentController.list` 返回的是 `R<Map<String, Object>>`（手写 Map），
> 而 `PermissionController` 返回 `R<PageResult<...>>`。两者不一致，应统一到 `PageResult`。

### 4.5 RAG 实现规约

项目内所有 RAG 能力**优先使用 Spring AI 已封装的能力**，仅在 Spring AI 无对应实现时才自研，
且必须在类注释中写明原因。

| 能力 | Spring AI 对应 | 本项目使用情况 |
|------|---------------|--------------|
| 查询改写 | `RewriteQueryTransformer` | ✅ 已用 |
| 向量召回（含过滤） | `VectorStoreDocumentRetriever` | ✅ 已用 |
| 对话记忆 | `ChatMemory` + `MessageChatMemoryAdvisor` | ✅ 已用（自实现 `DatabaseChatMemory` 落 PostgreSQL） |
| 上下文组装 | `QuestionAnswerAdvisor` / `RetrievalAugmentationAdvisor` | ⚠️ **未用**，当前在 `RagRetriever.buildContextText` 手写组装 |
| 引用溯源 | Advisor 元数据 | ⚠️ 改为自建 `ChatCitation` 记录 |

> **自研原因说明**：上下文组装未使用 `QuestionAnswerAdvisor`，
> 是因为需要把「引用来源」与「喂给模型的上下文文本」按同一顺序编号对齐，
> 并在召回为空时注入固定话术，Spring AI 默认 Advisor 不直接暴露这两者的一一对应关系。
> 该自研点已被 `RetrievalAugmentationAdvisor` 的导入但未使用所印证——属可优化项（见 §14 P2-2）。

---

## 5. 模块详细设计

### 5.1 认证与安全

#### 5.1.1 JWT

`auth/util/JwtUtil.java`（jjwt 0.12.6，HS256）

| 项 | 值 |
|----|-----|
| 密钥 | `${JWT_SECRET:change-me-in-production-please-use-a-long-random-string}` |
| 有效期 | `${JWT_EXPIRATION_HOURS:168}` = **7 天** |
| 请求头 | `Authorization: Bearer {token}` |
| Claims | `userId`、`username`、`role`、`department`（可选）、`sub`=username |

> ⚠️ 生产环境必须覆盖默认密钥。当前 `application.yml` 的兜底值会让服务在未配置密钥时**静默启动**，
> 属于安全反模式（见 §14 P0-5）。

#### 5.1.2 Security 配置

`auth/config/SecurityConfig.java`

- 无状态（`SessionCreationPolicy.STATELESS`），CSRF 禁用。
- `JwtAuthenticationFilter` 注册在 `UsernamePasswordAuthenticationFilter` 之前。
- 白名单：`/auth/login`、`/auth/register`、`/actuator/**`、`/v3/api-docs/**`、`/swagger-ui/**`、
  `/doc.html`、`/webjars/**`、`/favicon.ico`、`/swagger-resources/**`、`/swagger-ui.html`、`/permissions/test/whoami`。
- `dispatcherTypeMatchers(ASYNC, ERROR).permitAll()` —— SSE 异步派发不重跑认证过滤器，避免流结束被判为匿名。

> ⚠️ 白名单中的 `/auth/register` **无对应接口**；`/actuator/**` **无 actuator 依赖**。均为无效放行。

#### 5.1.3 密码

BCrypt（`BCryptPasswordEncoder` 默认 cost 10）。种子管理员 `admin` / `admin123`（hash 写在 `schema.sql`）。

#### 5.1.4 登出

`AuthService.logout()` 为**空实现**（无状态 JWT，客户端自行清除 Token）。
类注释已预留 Redis 黑名单扩展点，但 Redis 未启用。

---

### 5.2 权限体系

#### 5.2.1 注解与切面

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String resource();                      // DOCUMENT / QA / AGENT / USER ...
    PermissionAction action();              // READ / WRITE / DELETE
    SecurityLevel minLevel() default SecurityLevel.PUBLIC;
    boolean adminOnly() default false;      // 角色×密级矩阵无法表达「仅 ADMIN」时的补充开关
}
```

`permission/aspect/PermissionAspect.java`（`@Before`）判定顺序：

```
1. 角色 == ADMIN          → 直接放行
2. adminOnly == true      → 非 ADMIN 拒绝
3. canPerformAction(role, minLevel, action.isWrite()) == false → 拒绝
4. 拒绝时写一条 DENIED 审计日志（resource = "资源:操作:密级"），抛 PermissionDeniedException
```

#### 5.2.2 角色 × 密级 × 操作矩阵（`PermissionService`）

```
canAccess(role, level):
    ADMIN  → true
    EDITOR → level != CONFIDENTIAL || canEditConfidential()   // canEditConfidential() 恒为 true ⇒ 实际为 true
    VIEWER → level ∈ {PUBLIC, INTERNAL}
    GUEST  → level == PUBLIC

canPerformAction(role, level, write):
    !canAccess                     → false
    write && 只读角色(VIEWER/GUEST) → false
    write && EDITOR && CONFIDENTIAL → false
    否则                            → true
```

#### 5.2.3 四条判定路径（**当前不一致，需收敛**）

| # | 位置 | 用途 | EDITOR 读 CONFIDENTIAL |
|---|------|------|------------------------|
| A | `PermissionService`（切面调用） | 注解判定 | **允许** |
| B | `DocumentAccessChecker` | 单文档读写 | 拒绝 |
| C | `DocumentAccessFilter` | 向量召回过滤 | 拒绝 |
| D | `DocumentRepository.findPageWithPermission`（原生 SQL） | 文档列表 | 拒绝 |

A 与 B/C/D 结论相反。实际对外表现由 B/C/D 决定（编辑者**读不到**机密文档），
但注解口径与实际行为矛盾，一旦未来新增一个只挂注解、不走 checker 的机密接口即会**越权**。

**B 的精确规则**（`DocumentAccessChecker`）：

```
canAccess(userRole, securityLevel):           // 角色口径
    ADMIN  → true
    GUEST  → level == PUBLIC
    VIEWER|EDITOR → level != CONFIDENTIAL
    其他   → false

canAccess(userId, userRole, documentId, level): // 资源口径
    角色口径通过 → true
    否则存在 doc_permissions 记录（任意权限类型）→ true   // 授权可突破密级
    否则 → false

canWrite(userId, userRole, documentId, level):
    ADMIN → true
    非写角色（VIEWER/GUEST）→ false               // 只读角色永不可写，即使有 WRITE 授权
    存在 WRITE 授权 → true
    EDITOR && level != CONFIDENTIAL → true
    否则 → false
```

**C 的精确规则**（`DocumentAccessFilter.buildFilter`，下推向量库）：

```
ADMIN  → null（不加过滤）
GUEST  → securityLevel IN (PUBLIC)
其他    → (securityLevel IN (PUBLIC, INTERNAL)) OR (documentId IN 被授权文档ID列表)
```

授权 ID 列表在 Java 侧查 `doc_permissions` 后物化为 `IN` 列表下推——
是**过滤下推**（不是「先召回后过滤」），但授权列表是急切加载的。

**D 的精确规则**（列表原生 SQL）：

```sql
d.deleted = 0 AND (:keyword IS NULL OR 标题/摘要/类型 LOWER LIKE ...)
AND (:securityLevel IS NULL OR d.security_level = :securityLevel)
AND (:statusCode IS NULL OR d.status = :statusCode)
AND (:userRole = 'ADMIN'
     OR d.id IN (SELECT dp.document_id FROM doc_permissions dp WHERE dp.grantee_id = :userId)
     OR d.security_level = 'PUBLIC'
     OR (:userRole IN ('EDITOR','VIEWER') AND d.security_level = 'INTERNAL'))
```

#### 5.2.4 文档级授权

`doc_permissions(document_id, grantee_id, permission, created_at)`，`UNIQUE(document_id, grantee_id)`。

- **只支持用户级授权**，表中没有 department 列（`DocPermission` 的 Javadoc 写「支持按用户或按部门授权」是错的）。
- `DocumentPermissionService.grant` 采用 upsert 语义（存在则更新，避免违反唯一约束）。
- 授予 `WRITE` 给只读角色会被拒绝：`BusinessException(400, "查看者与访客为只读角色，不能授予读写权限")`。

---

### 5.3 知识库模块

#### 5.3.1 上传与索引流程

```
POST /documents/upload  (multipart: file + request)
  │
  ├─ @RequirePermission(DOCUMENT, WRITE, PUBLIC)
  ├─ @AuditLog(UPLOAD, DOCUMENT)
  ├─ @Transactional
  │
  ├─ 1. 建 documents 记录（status=PROCESSING(2), fileKey=原始文件名, fileType=扩展名, ownerId, department）
  ├─ 2. FileStorageService.store(file, docId) → {rootPath}/{docId}/{清洗后的文件名}
  └─ 3. afterCommit → 发布 DocumentUploadedEvent
        │
        ▼  @Async(documentIndexExecutor) @EventListener
     DocumentIndexListener.handleDocumentUploaded
        ├─ 4. DocumentParser.loadDocument(path)      ← Apache Tika
        ├─ 5. DocumentSpliter.split(docs)            ← TokenTextSplitter
        ├─ 6. enrichQuietly()                       ← LLM 关键词/摘要（失败降级跳过）
        ├─ 7. VectorStoreService.addChunks(...)      ← 写 vector_store（内含 embedding）
        ├─ 8. 回写 chunk_count + status=ACTIVE(1)
        ├─ 9. extractSummaryAndTags()               ← 写 documents.summary / tags
        └─ 异常 → status=FAILED(0) + errorMessage（截断 500 字）
```

**为什么事件在 `afterCommit` 才发布**：异步索引线程会立即读 `documents` 表。
PostgreSQL 默认 READ COMMITTED 下，未提交的行不可见，会导致「Document not found」而把正常文档误判为失败。

**队列满的处理**：`documentIndexExecutor` 队列 200、`AbortPolicy`。
`TaskRejectedException` 被捕获后，在 `afterCompletion` 中调 `DocumentIndexFailureHandler.markFailed`，
避免文档永久停留在「处理中」。

#### 5.3.2 文件存储（本地文件系统）

`knowledge/service/FileStorageService.java`，路径 `{agenthub.storage.root-path}/{docId}/{filename}`，默认 `./data/docs`。

文件名清洗（跨平台安全）：

| 规则 | 实现 |
|------|------|
| 剥离目录成分 | `Paths.get(filename).getFileName()`（防 `../../etc/passwd`） |
| 非法字符 | `[\\/:*?"<>|\x00-\x1f]` → `_` |
| 首尾点号 | trim 后去前导 `.`（防隐藏文件） |
| Windows 保留名 | `CON/PRN/AUX/NUL/COM1-9/LPT1-9` 前缀 `_` |
| 长度 | 截断至 200 字符（保留扩展名），避免 Windows MAX_PATH 260 |

> ⚠️ **无扩展名白名单**：任何扩展名都能上传，非法格式要到 Tika 解析阶段才失败，
> 用户体验上应提前拒绝（见 §14 P2-6）。

#### 5.3.3 解析与分块

| 项 | 实现 |
|----|------|
| 解析 | `TikaDocumentReader`（`spring-ai-tika-document-reader`），支持 docx/pdf/pptx/xlsx/txt/md |
| 分块器 | Spring AI `TokenTextSplitter` |
| 块大小 | `agenthub.chunk.size` = **1000 token** |
| 最小块长 | `agenthub.chunk.min-size` = **400 字符**（`Math.min(minSize, size)` 兜底，防无限合并） |
| 最小可嵌入长度 | 5 字符（`MIN_CHUNK_LENGTH_TO_EMBED`） |
| 单文档上限 | 6000 块（`MAX_NUM_CHUNKS`） |
| 保留分隔符 | true |
| **重叠** | **不支持** —— Spring AI `TokenTextSplitter` 无 overlap 参数（已在类注释说明） |

> ⚠️ `knowledge/package-info.java` 仍写「512 token + 50 重叠」，是过期描述。

#### 5.3.4 LLM 增强（摘要 / 标签）

`knowledge/enricher/KnowlegeDocumentEnricher` 调用 LLM 产出 `excerpt_keywords` 与 `section_summary` 写入分块 metadata。

- 开关：`agenthub.knowledge.enrich-with-llm`（默认 true）。
- 摘要调用分块数：`agenthub.knowledge.summary-chunks`（默认 1，即只对首块调一次 LLM，其余复用）。
- **降级策略**：关键词增强与摘要增强各自 `try/catch`，失败只告警不阻断索引。
- 摘要回写 `documents.summary`（截断 200 字），标签回写 `documents.tags`（`TEXT[]`）。

> ⚠️ **元数据丢失**：`VectorStoreService.addChunks` 用全新的 `HashMap` 重建 metadata，
> 只写 `{documentId, securityLevel, title, chunkIndex}` 四项，
> **把 enricher 写入的 `excerpt_keywords` / `section_summary` 丢弃了**。
> 摘要与标签仍通过内存中的分块对象回写到 `documents` 表，但向量库侧的富元信息不可用
> （详见 §5.4）。

---

### 5.4 向量存储

#### 5.4.1 配置

`knowledge/config/PgVectorStoreConfig.java`

```java
PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
    .dimensions(1024)                              // = spring.ai.dashscope.embedding.options.dimensions
    .distanceType(PgDistanceType.COSINE_DISTANCE)
    .indexType(PgIndexType.HNSW)
    .initializeSchema(true)
    .schemaName("public")
    .vectorTableName("vector_store")
    .maxDocumentBatchSize(10000)
```

维度 1024 与 `text-embedding-v3` 输出一致。表结构由 `PgVectorStore` 自维护，
`schema.sql` 中的同名建表语句仅供「全量重建」时使用。

#### 5.4.2 元数据契约

每个分块写入 `vector_store.metadata`（JSONB）：

| 键 | 类型 | 用途 |
|----|------|------|
| `documentId` | Long | 权限过滤（`documentId IN (...)`）、删除定位、按文档查分块 |
| `securityLevel` | String | 权限过滤（`securityLevel IN (...)`） |
| `title` | String | 引用来源展示 |
| `chunkIndex` | int | 引用来源定位分块、文档内排序 |

**分块 ID**：`UUID.nameUUIDFromBytes((documentId + "-" + i).getBytes())` —— 确定性 UUID，
同一文档重复索引时 upsert 覆盖而非重复插入。

**删除**：`deleteByDocumentId(documentId)` → `pgVectorStore.delete(eq("documentId", documentId))`。

#### 5.4.3 文档分块读取

**没有 `document_chunks` 表**（已在 `schema.sql` 中 DROP）。
`DocumentChunk` 不是 JPA 实体，而是 `vector_store` 的只读投影 POJO；
`DocumentChunkRepository` 用 `JdbcTemplate` 直查：

```sql
SELECT id, content, metadata->>'chunkIndex' AS chunk_index
FROM vector_store
WHERE metadata->>'documentId' = ?
ORDER BY CASE WHEN metadata->>'chunkIndex' ~ '^[0-9]+$'
               THEN (metadata->>'chunkIndex')::int ELSE 999999 END, id
LIMIT 500
```

> ⚠️ 单文档最多返回 500 个分块，超出部分静默截断。

---

### 5.5 智能问答 RAG

#### 5.5.1 链路

```
POST /qa/ask  (SSE)
  │
  ├─ QaService.ask(question, conversationId, userId)
  │    @RequirePermission(QA, READ)
  │    ├─ resolveConversation() → 新建或校验归属（他人会话 → 404）
  │    └─ chatService.chatStream(question, conversationId)
  │
  │         ChatService.chatStream
  │           ├─ 1. RagRetriever.retrieve(question, userId, userRole)
  │           │     ├─ 1a. RewriteQueryTransformer.transform()   ← 查询改写（可关）
  │           │     ├─ 1b. VectorStoreDocumentRetriever.retrieve()
  │           │     │       topK=5, similarityThreshold=0.7
  │           │     │       filterExpression = DocumentAccessFilter.buildFilter(role, userId)
  │           │     └─ 1c. buildContextText() → "[1] 标题（第 N 段）：正文" 编号上下文
  │           ├─ 2. ChatClient.prompt()
  │           │       .system(getQaSystemPrompt(question, contextText))   ← 上下文注入 system
  │           │       .user(question)
  │           │       .advisors(CHAT_MEMORY_CONVERSATION_ID = conversationId,
  │           │                CHAT_MEMORY_RETRIEVE_SIZE = 10)
  │           │       .stream().content()
  │           └─ 3. onErrorMap → BusinessException(500, "模型服务调用失败：…")
  │
  └─ SSE 事件：delta* → done（conversationId, messageId, sources）→ （error）
```

#### 5.5.2 关键参数

| 参数 | 配置项 | 值 |
|------|--------|-----|
| 召回条数 | `agenthub.qa.top-k` | 5 |
| 相似度阈值 | `agenthub.qa.similarity-threshold` | **0.7**（yml 值；代码默认值 0.8 被覆盖） |
| 查询改写 | `agenthub.qa.query-rewrite-enabled` | true |
| 记忆回灌条数 | `MessageChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY` | 10 |

#### 5.5.3 引用溯源

`common/llm/ChatCitation` = `record(Long documentId, String title, Integer chunkIndex)`。

- 召回时由 `QaConverter.toCitations(documents)` 生成，与上下文编号同序。
- `done` 事件携带 `sources`；前端渲染为可点击引用，点击打开文档抽屉并定位到分块。
- 落库在 `qa_messages.sources`（JSONB）。

> ⚠️ 引用只到**分块级**，无页码 / 章节路径。

#### 5.5.4 对话记忆

`qa/service/DatabaseChatMemory` 实现 Spring AI `ChatMemory` 接口：

| 操作 | 实现 |
|------|------|
| `add` | 每条消息插一行 `qa_messages`（role = USER/ASSISTANT/SYSTEM，sources=null） |
| `get` | `findByConversationIdOrderByIdDesc` 取最后 N 条后反转 |
| `clear` | `deleteByConversationId` |

**助手消息落库**：`QaService.saveAssistantMessage` 在 SSE 完成回调（异步线程，无 SecurityContext）中执行，
因此**刻意不加** `@RequirePermission`（否则切面抛「未登录」）。它按回答文本匹配已落库的助手消息并回填 `sources`，
匹配不到时兜底新建一条。

> ⚠️ 记忆**无窗口裁剪**：存储行数无限增长，仅靠 advisor 的 retrieve-size 10 控制回灌量。

#### 5.5.5 反馈

`POST /qa/feedback` → `qa_messages.feedback`（1 有帮助 / 0 无帮助 / NULL 未反馈）。
**无任何聚合或指标看板**，反馈目前是「只进不出」。

---

### 5.6 超级智能体

#### 5.6.1 运行时

```
BaseAgent（抽象：状态、上下文、事件回调、执行循环）
  └─ ReActAgent（抽象：think() / act()）
       └─ ToolCallAgent（工具调用：思考 → 选工具 → 执行 → 回填上下文）
            └─ SuperManus（系统提示词 + maxSteps=20）
```

`BaseAgent.run(userPrompt)`：

```
state = RUNNING；messageList += UserMessage
for i in 0..maxSteps:
    if state == FINISHED: break
    currentStep = i + 1
    step()                      // think() → act()
if currentStep >= maxSteps && state == RUNNING:
    state = FINISHED; emitStepProgress("执行结束: 达到最大步骤 (N)")
emitComplete(resolveFinalResult())
finally: cleanup()
```

`ToolCallAgent` 关键点：

- 用 `DashScopeChatOptions.builder().withProxyToolCalls(true)` **禁用** Spring AI 内置工具执行，
  由 Agent 自己执行，以便逐步上报事件。
- `think()`：`chatClient.prompt(messages).system(systemPrompt).tools(availableTools).call()` →
  上报 `STEP_START`（步骤名 = 工具名，多个用 ` / ` 连接；无工具则「回答」）、
  `STEP_PROGRESS`（模型思考文本 + 工具调用说明）。
  无工具调用时追加助手消息、上报 `STEP_COMPLETE`、返回 false。
- `act()`：`ToolCallingManager.executeToolCalls(...)` → 用 `conversationHistory` 替换 `messageList` →
  上报 `STEP_COMPLETE`（工具名 + 结果）。若调用了 `doTerminate` 则置 `state = FINISHED`。

#### 5.6.2 工具层

`agent/config/AgentToolConfig` 注册 6 个工具：

| 工具类 | `@Tool` 方法 | 产物目录 | 实现要点 |
|--------|--------------|---------|---------|
| `FileOperationTool` | `readFile(fileName)` / `writeFile(fileName, content)` | `<work-dir>/file/` | hutool `FileUtil`；文件名经 `AgentToolSupport.sanitizeFileName`（取最后一段、剥离非法字符、200 字上限）防路径穿越 |
| `PDFGenerationTool` | `generatePDF(fileName, content)` | `<work-dir>/pdf/` | iText 9.1.0 + `font-asian`，中文字体 `STSongStd-Light` / `UniGB-UCS2-H` |
| `ResourceDownloadTool` | `downloadResource(url, fileName)` | `<work-dir>/download/` | hutool `HttpUtil.downloadFile`；仅允许 http/https |
| `WebScrapingTool` | `scrapeWebPage(url)` | 返回文本 | jsoup 1.19.1，UA `Mozilla/5.0 (compatible; AgentHub-SuperAgent)`，超时 `web-scrape-timeout-ms`（15000） |
| `WebSearchTool` | `searchWeb(query)` | 返回文本 | SearchAPI.io（engine=bing）；**未配置 `web-search-api-key` 时返回「工具暂不可用」提示** |
| `TerminateTool` | `doTerminate()` | — | 返回「任务结束」；`ToolCallAgent.act()` 通过工具名 `doTerminate` 判定结束 |

配置前缀 `agenthub.agent.tool`：`work-dir`（`./data/agent`）、`web-search-api-key`、`web-search-api-url`、
`web-search-limit`（5）、`web-scrape-timeout-ms`（15000）。

> 🔴 **关键缺口**：**没有 `knowledge_search` / `document_read` 工具**。
> 智能体无法检索企业知识库，只能访问外部网络与本地沙箱目录。
> 这是与「智能问答」的产品断层，也是 §14 P0-1 的由来。

#### 5.6.3 会话与持久化

| 表 | 内容 |
|----|------|
| `agent_conversations(id VARCHAR(64) PK, user_id, title, created_at, updated_at)` | 会话容器，标题取首问截断 |
| `agent_messages(id, conversation_id, role USER/ASSISTANT, content, steps_json JSONB, created_at)` | 消息 + 步骤轨迹 |

- 每次任务**新建 `SuperManus` 实例**（Agent 持有运行期状态，不可单例共享）。
- **多轮上下文**：`loadHistory` 只回放「用户提问 + 最终回答」的文本，
  **不还原工具调用与工具结果的原始结构** —— MVP 级取舍，已在 `openspec/changes/super-agent-chat/design.md` D4 记录。
- 步骤轨迹以 JSONB 存 `steps_json`，前端回看时还原时间线。

#### 5.6.4 已知问题

| 问题 | 位置 | 影响 |
|------|------|------|
| 用 `CompletableFuture.runAsync`（默认 ForkJoinPool） | `SuperAgentService.chat` | 智能体长任务与文档索引抢公共线程池；`AsyncConfig` 只覆盖了 `documentIndexExecutor` |
| `MyLoggerAdvisor` 未加进 Agent 的 ChatClient | `SuperManus` 构造 | Agent 侧无请求/响应日志（同时意味着 `SensitiveWordAdvisor` 也未覆盖 Agent） |
| 工作目录不按用户隔离 | `AgentToolProperties.workDir` | 同一服务实例内，跨用户的中间文件互相可见 |
| 产物不可取回 | 全部工具 | 「生成 PDF / 报告」的价值闭环断在最后一步 |

---

### 5.7 审计

#### 5.7.1 机制

```java
@Target(ElementType.METHOD) @Retention(RUNTIME)
public @interface AuditLog {
    String action();             // 必填
    String resource() default "";
}
```

`audit/aspect/AuditLogAspect`：`@AfterReturning` → `success=1`；`@AfterThrowing` → `success=0` 且 `detail.error = ex.getMessage()`。

记录字段：`userId`、`username`（从 `CustomUserDetails` 取）、`action`、`resource`、`success`、
`ipAddress`（优先取方法参数中的 `HttpServletRequest`，否则 `RequestContextHolder`）、`detail`（JSONB：`timestamp` + `error?`）。

#### 5.7.2 已标注位置

| 位置 | 动作 |
|------|------|
| `DocumentUploadService#upload` | UPLOAD / DOCUMENT |
| `DocumentService#delete` | DELETE / DOCUMENT |
| `PermissionController`（**Controller 层**） | CREATE_USER / UPDATE_USER / DELETE_USER / TOGGLE_STATUS / RESET_PASSWORD / UPDATE_ROLE / GRANT / REVOKE |
| `PermissionTestController` | VIEW_PUBLIC / VIEW_INTERNAL / VIEW_CONFIDENTIAL |

此外 `PermissionAspect` 在拒绝时**直接写**一条 `DENIED` 记录。

#### 5.7.3 缺口

- 未覆盖：登录 / 登出、文档查看 / 下载 / 分块阅读、问答调用、智能体调用、角色列表、用户列表、审计查询本身。
- `AuditLogEntity` 与 `AuditLogRepository` 放在 **`permission` 包**而非 `audit` 包 —— 分层异味。
- 审计注解标注在 Controller 上，违反项目「注解标在 Service」的规约。

---

### 5.8 公共设施

| 组件 | 说明 |
|------|------|
| `common/response/R<T>` | `{code, message, data}`，`code==0` 为成功。业务码**同时以 HTTP 200 返回**，前端按 `code` 判断 |
| `common/exception/BusinessException` | `RuntimeException` + `int code`，单参构造默认 400 |
| `common/exception/GlobalExceptionHandler` | `@RestControllerAdvice`：BusinessException / PermissionDeniedException → `R.fail(code,msg)`；其他 → `R.fail(500,"服务器内部错误")` |
| `common/dto/PageQuery` | `page`(1起) / `size`(默认 20，上限 200) / `getPageIndex()` / `getPageSize()` / `toPageRequest(sort)` |
| `common/dto/PageResult<T>` | `{list, total, page, size}`，`of(Page)` / `of(Page, mapper)` |
| `common/llm/ChatService` | 封装 `ChatClient`；Advisor 链：`MessageChatMemoryAdvisor` → `MyLoggerAdvisor` → `SensitiveWordAdvisor` |
| `common/llm/SystemPromptFactory` | QA 系统提示词（`{user_question}` / `{rag_context}`）与查询改写提示词 |
| `common/advisor/MyLoggerAdvisor` | order 0，INFO 打印完整提示词与模型输出 |
| `common/advisor/SensitiveWordAdvisor` | order 900，`CallAroundAdvisor` + `StreamAroundAdvisor`，5 条硬编码词库；命中即返回固定拒绝话术，**不调用模型**；仅检查用户输入 |
| `common/config/AsyncConfig` | `documentIndexExecutor`：core 4 / max 8 / queue 200 / `AbortPolicy` / `waitForTasksToCompleteOnShutdown(true)` / 线程前缀 `doc-index-` |
| `common/util/IpUtils` | 客户端 IP 提取（供审计） |
| `common/HealthController` | 健康检查 |
| `common/config/Knife4jConfig` | OpenAPI 文档配置 |

> ⚠️ **异常处理缺口**：`GlobalExceptionHandler` 没有 `MethodArgumentNotValidException` 的处理器，
> 参数校验失败会落到通用 500 分支，而不是 400。

---

## 6. 数据库设计

### 6.1 表清单

> schema 由 Hibernate `ddl-auto: update` 自动维护；`backend/src/main/resources/db/migration/schema.sql`
> 是**全量重建脚本**（开头 DROP 全部表再重建），需手工执行。**项目未引入 Flyway。**

| # | 表 | 用途 | 实体 |
|---|----|----|------|
| 1 | `users` | 用户 | `auth/model/User` |
| 2 | `documents` | 文档元信息 | `knowledge/model/Document` |
| 3 | `doc_permissions` | 文档级授权 | `permission/model/DocPermission` |
| 4 | `qa_conversations` | 问答会话 | `qa/model/Conversation` |
| 5 | `qa_messages` | 问答消息（含记忆与引用） | `qa/model/ChatMemoryMessage` |
| 6 | `agent_conversations` | 智能体会话 | `agent/model/AgentConversation` |
| 7 | `agent_messages` | 智能体消息（含步骤轨迹） | `agent/model/AgentMessage` |
| 8 | `audit_logs` | 审计日志 | `permission/model/AuditLogEntity` |
| 9 | `vector_store` | 分块 + 向量 | Spring AI `PgVectorStore` 自维护 |

**已废弃并被 DROP 的表**：`document_chunks`（被 `vector_store` 取代）、
`agent_tasks` / `agent_steps`（Plan-and-Execute 规划器未启用）。

#### 6.1.1 users

```sql
id BIGSERIAL PK, username VARCHAR(64) UNIQUE NOT NULL, password VARCHAR(128) NOT NULL,
real_name VARCHAR(64), email VARCHAR(128), role VARCHAR(16) NOT NULL DEFAULT 'VIEWER',
department VARCHAR(64), status SMALLINT NOT NULL DEFAULT 1,   -- 1 启用 / 0 禁用
created_at/updated_at TIMESTAMPTZ, deleted SMALLINT DEFAULT 0
```

#### 6.1.2 documents

```sql
id BIGSERIAL PK, title VARCHAR(512) NOT NULL, file_key VARCHAR(256) NOT NULL,  -- 原始文件名
file_type VARCHAR(16) NOT NULL, file_size BIGINT NOT NULL DEFAULT 0,
security_level VARCHAR(16) NOT NULL DEFAULT 'INTERNAL',
owner_id BIGINT NOT NULL, department VARCHAR(64),
summary TEXT, tags TEXT[],
status SMALLINT NOT NULL DEFAULT 1,      -- FAILED=0 / ACTIVE=1 / PROCESSING=2
chunk_count INT NOT NULL DEFAULT 0, error_message TEXT,
created_at/updated_at TIMESTAMPTZ, deleted SMALLINT DEFAULT 0
```
索引：`idx_doc_owner(owner_id)`、`idx_doc_dept(department)`、`idx_doc_security(security_level)`。

> ⚠️ `schema.sql` 中 `status` 列的注释「1 正常 0 删除 2 处理中」与 `DocumentStatus`
> （FAILED=0 / ACTIVE=1 / PROCESSING=2）**不一致**，注释是错的。

#### 6.1.3 doc_permissions

```sql
id BIGSERIAL PK, document_id BIGINT NOT NULL, grantee_id BIGINT NOT NULL,
permission VARCHAR(16) NOT NULL DEFAULT 'READ',   -- READ / WRITE
created_at TIMESTAMPTZ NOT NULL, UNIQUE(document_id, grantee_id)
```

#### 6.1.4 qa_conversations / qa_messages

```sql
qa_conversations(id VARCHAR(64) PK, user_id BIGINT NOT NULL, title VARCHAR(200) NOT NULL,
                 created_at/updated_at TIMESTAMPTZ)
  idx_qa_conv_user(user_id, updated_at DESC)

qa_messages(id BIGSERIAL PK, conversation_id VARCHAR(64) NOT NULL, role VARCHAR(16) NOT NULL,
            content TEXT NOT NULL, sources JSONB, feedback SMALLINT, created_at TIMESTAMPTZ)
  idx_qa_msg_conv(conversation_id, id)
```

#### 6.1.5 agent_conversations / agent_messages

```sql
agent_conversations(id VARCHAR(64) PK, user_id BIGINT NOT NULL, title VARCHAR(200) NOT NULL,
                    created_at/updated_at TIMESTAMPTZ)
  idx_agent_conv_user(user_id, updated_at DESC)

agent_messages(id BIGSERIAL PK, conversation_id VARCHAR(64) NOT NULL, role VARCHAR(16) NOT NULL,
               content TEXT NOT NULL, steps_json JSONB, created_at TIMESTAMPTZ)
  idx_agent_msg_conv(conversation_id, id)
```

#### 6.1.6 audit_logs

```sql
id BIGSERIAL PK, user_id BIGINT, username VARCHAR(64), action VARCHAR(64) NOT NULL,
resource VARCHAR(128), detail JSONB, ip_address VARCHAR(64),
success SMALLINT NOT NULL DEFAULT 1, created_at TIMESTAMPTZ
  idx_audit_user(user_id), idx_audit_time(created_at)
```

#### 6.1.7 vector_store

```sql
id UUID PK DEFAULT uuid_generate_v4(),
content TEXT NOT NULL,
metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
embedding vector(1024) NOT NULL
```
索引：`idx_vector_store_embedding USING hnsw (embedding vector_cosine_ops) WITH (m=16, ef_construction=64)`

#### 6.1.8 种子数据

`schema.sql` 末尾插入管理员 `admin`（BCrypt cost 10，密码 `admin123`），`ON CONFLICT (username) DO NOTHING`。

### 6.2 ER 关系

```
users ──1:N──> documents (owner_id)
users ──1:N──> doc_permissions (grantee_id)
documents ──1:N──> doc_permissions (document_id)

users ──1:N──> qa_conversations ──1:N──> qa_messages
users ──1:N──> agent_conversations ──1:N──> agent_messages
users ──1:N──> audit_logs

documents ──(metadata.documentId)──> vector_store     ← 逻辑外键，非 FK
```

> `vector_store` 与 `documents` 之间**没有数据库外键**，靠 metadata 关联。
> 好处是删除文档无需级联；代价是可能产生孤儿向量（当前靠 `DocumentService.delete` 同事务清理保证）。

---

## 7. API 设计

统一前缀 `/api`（`server.servlet.context-path: /api`）。
响应包装 `{"code":0,"message":"success","data":{...}}`；**业务码以 HTTP 200 返回**，前端按 `code` 判定。

### 7.1 认证 `/auth`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 登录，返回 JWT |
| POST | `/auth/logout` | 登出（无状态，空实现） |
| GET | `/auth/me` | 当前用户信息 |
| PUT | `/auth/me` | 修改资料（realName / email / department） |
| PUT | `/auth/me/password` | 修改密码（校验旧密码） |

### 7.2 知识库 `/documents`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/documents/upload` | 上传（multipart：`file` + `request` JSON part） |
| GET | `/documents` | 分页列表（`page` / `size` / `keyword` / `securityLevel` / `status`） |
| GET | `/documents/{id}` | 详情 |
| GET | `/documents/{id}/chunks` | 分块内容（最多 500 条） |
| DELETE | `/documents/{id}` | 删除（软删除 + 同事务清理向量） |

### 7.3 问答 `/qa`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/qa/ask` | **SSE** 流式问答 |
| POST | `/qa/feedback` | 提交反馈 `{messageId, useful}` |
| GET | `/qa/conversations` | 会话列表 |
| GET | `/qa/conversations/{id}/messages` | 消息历史 |
| DELETE | `/qa/conversations/{id}` | 删除会话 |

### 7.4 智能体 `/agent`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/agent/manus/chat` | **SSE** 流式执行 `{message, conversationId}` |
| GET | `/agent/conversations` | 会话列表 |
| GET | `/agent/conversations/{id}/messages` | 消息 + 步骤轨迹 |
| DELETE | `/agent/conversations/{id}` | 删除会话 |

### 7.5 权限 `/permissions`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/permissions/users` | 用户列表 |
| POST | `/permissions/users` | 创建用户 |
| PUT | `/permissions/users/{userId}` | 更新用户 |
| DELETE | `/permissions/users/{userId}` | 删除用户（软删） |
| PUT | `/permissions/users/{userId}/status` | 启用 / 禁用 |
| PUT | `/permissions/users/{userId}/password` | 重置密码 |
| GET | `/permissions/roles` | 角色列表（4 个，写死） |
| PUT | `/permissions/users/{userId}/role` | 修改角色 |
| GET | `/permissions/documents/grants` | 授权列表（分页） |
| GET | `/permissions/documents/{documentId}/permissions` | 某文档的授权列表 |
| POST | `/permissions/documents/{documentId}/grant` | 授予权限 |
| DELETE | `/permissions/documents/{documentId}/revoke` | 撤销权限 |
| GET | `/permissions/audit-logs` | 审计日志查询（分页） |

### 7.6 其他

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/permissions/test/{public\|internal\|confidential}` | 权限验证端点（开发用，白名单仅放行 `whoami`） |
| GET | `/permissions/test/whoami` | 当前身份（白名单） |
| GET | `/health` | 健康检查 |
| GET | `/v3/api-docs`、`/doc.html` | 接口文档 |

### 7.7 关键请求 / 响应示例

#### 上传文档

```http
POST /api/documents/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: <binary>
request: {"title":"差旅费用报销管理制度","securityLevel":"INTERNAL","tags":"财务,报销"}
```

```json
{
  "code": 0, "message": "success",
  "data": {
    "id": 12, "title": "差旅费用报销管理制度", "fileName": "差旅费用报销管理制度_2026版.md",
    "fileType": "md", "fileSize": 20481, "securityLevel": "INTERNAL",
    "status": "PROCESSING", "chunkCount": 0,
    "createdAt": "2026-10-31T10:30:00Z"
  }
}
```

#### 简单问答

```http
POST /api/qa/ask
Content-Type: application/json
Authorization: Bearer {token}

{"question":"报销流程是什么？","conversationId":null}
```

SSE 响应（事件名见 §8）：

```text
event: delta
data: {"content":"根据《差旅费用报销管理制度》，报销流程如下："}

event: delta
data: {"content":"1. 填写报销单…"}

event: done
data: {"conversationId":"b2f1...","messageId":87,
       "sources":[{"documentId":12,"title":"差旅费用报销管理制度_2026版","chunkIndex":2}]}
```

#### 智能体

```http
POST /api/agent/manus/chat
Content-Type: application/json
Authorization: Bearer {token}

{"message":"帮我整理一份本周行业动态简报","conversationId":null}
```

```text
event: agent.step.start
data: {"eventType":"STEP_START","stepIndex":1,"stepName":"searchWeb"}

event: agent.step.progress
data: {"eventType":"STEP_PROGRESS","stepIndex":1,"message":"先搜索本周行业动态…"}

event: agent.step.complete
data: {"eventType":"STEP_COMPLETE","stepIndex":1,"toolName":"searchWeb","toolResult":"找到 5 条结果…"}

event: agent.complete
data: {"conversationId":"a91c...","messageId":12,"answer":"本周行业动态简报如下…"}
```

---

## 8. SSE 事件契约

### 8.1 问答事件（`/qa/ask`）

| 事件名 | 载荷 | 说明 |
|--------|------|------|
| `delta` | `{content}` | 答案增量 |
| `done` | `{conversationId, messageId, sources[]}` | 完成 + 引用来源 |
| `error` | `{message}` | 失败 |

### 8.2 智能体事件（`/agent/manus/chat`）

| 事件名 | 载荷 | 说明 |
|--------|------|------|
| `agent.step.start` | `{eventType, stepIndex, stepName}` | 步骤开始（步骤名 = 工具名） |
| `agent.step.progress` | `{eventType, stepIndex, message}` | 思考内容 / 工具调用说明 |
| `agent.step.complete` | `{eventType, stepIndex, toolName, toolResult}` | 工具执行结果 |
| `agent.complete` | `{conversationId, messageId, answer}` | 最终回答 |
| `agent.error` | `{message}` | 失败 |

> **未实现的事件**（早期设计稿中定义）：
> `agent.plan`（Plan-and-Execute 计划）、`agent.step.skip`（HITL 跳过）——
> `AgentEventType` 的类注释已明确说明当前 ReAct 运行时**不产生**这两类事件。

### 8.3 传输细节

| 项 | 约定 |
|----|------|
| 编码 | UTF-8，`text/event-stream` |
| 认证 | `Authorization` 头（**不能**用 `EventSource`，前端用 `fetch` + `ReadableStream` 手工解析） |
| 超时 | `SseEmitter(300_000)` = 5 分钟 |
| Nginx | `proxy_buffering off`、`proxy_cache off`、`proxy_read_timeout 600s`、`Connection ""` |
| 异步派发 | Security 放行 `DispatcherType.ASYNC/ERROR`，避免流结束时被判为匿名 |

---

## 9. 前端设计

### 9.1 技术栈与结构

Vue 3.5 + TypeScript + Element Plus 2.8 + Tailwind 3.4 + Pinia 2 + vue-router 4 + axios + markdown-it，Vite 5.4 构建。

```
frontend/src/
├── api/         request.ts(axios) auth.ts knowledge.ts qa.ts agent.ts permission.ts
├── components/  ConversationList.vue ProfileDrawer.vue ThemeToggle.vue
├── composables/ useProfile.ts useResponsive.ts
├── layouts/     MainLayout.vue
├── router/      index.ts
├── stores/      auth.ts theme.ts
├── types/       auth.ts permission.ts
├── utils/       permission.ts storage.ts
└── views/       Dashboard/ QAM/ Agent/ Knowledge/{List,Upload,Detail}
                 Permission/{index,UserManagement,DocumentPermission,AuditLog}
                 Profile/ Login/
```

### 9.2 路由与守卫

| 路径 | 组件 | meta |
|------|------|------|
| `/login` | Login | `public: true` |
| `/dashboard` | Dashboard | — |
| `/knowledge` | Knowledge/List | — |
| `/knowledge/upload` | Knowledge/Upload | `writeRequired: true` |
| `/knowledge/:id` | Knowledge/Detail | — |
| `/qa` | QAM | — |
| `/agent` | Agent | — |
| `/permission` | Permission | `adminOnly: true` |
| `/profile` | Profile | — |

守卫：无 token → `/login?redirect=…`；非 ADMIN 访问 `adminOnly` → `/dashboard`；
`writeRequired` 且 `canManageDocuments(role)` 为 false（VIEWER/GUEST）→ `/knowledge`。

### 9.3 SSE 消费方式

`api/qa.ts` 与 `api/agent.ts` 使用同一套**手工 SSE 解析器**：

```ts
const response = await fetch('/api/qa/ask', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
  body: JSON.stringify({ question, conversationId }),
  signal
})
const reader = response.body.getReader()
const decoder = new TextDecoder('utf-8')
let buffer = ''
while (true) {
  const { done, value } = await reader.read()
  if (done) break
  buffer += decoder.decode(value, { stream: true })
  let boundary = buffer.indexOf('\n\n')
  while (boundary !== -1) {
    /* 解析 event: / data: 行 → dispatchEvent */
    boundary = buffer.indexOf('\n\n')
  }
}
```

> **注意**：`EventSource` 无法携带 `Authorization` 头且只支持 GET，因此必须走 `fetch` + `ReadableStream`。

### 9.4 客户端权限镜像

`utils/permission.ts` 定义 `READ_ONLY_ROLES = ['VIEWER','GUEST']`、`isReadOnlyRole`、`canManageDocuments`，
与服务端规则保持人工同步 —— **客户端权限只用于 UI 显隐，服务端才是裁决方**。
收敛权限规则时需同步更新此处（见 §14 P0-2）。

### 9.5 已知前端问题

| 问题 | 位置 | 说明 |
|------|------|------|
| 统计卡数值写死 `'—'` | `Dashboard/index.vue` | 无统计接口，展示不出真实运营数据 |
| 能力标语与实现不符 | `Dashboard/index.vue` | 展示「计划先行 / 人在回路」，但两者均未实现 |
| `components/README.md` 描述了不存在的 `PlanStep/` 组件 | 文档 | 计划编辑 UI 尚未开工 |
| 无任何前端测试 | `package.json` | 无 vitest / jest |
| `vite.config.ts` 硬编码代理目标 | 配置 | `.env.development` 的 `VITE_PROXY_TARGET` 未被使用 |

---

## 10. 部署架构

### 10.1 现状

| 组件 | 形态 |
|------|------|
| 后端 | `backend/docker/Dockerfile`：`FROM openjdk:21-slim`，`COPY target/*.jar app.jar`，`EXPOSE 8080`，`CMD java -jar app.jar --spring.profiles.active=prod` |
| 前端 | `frontend/Dockerfile`：两阶段（`node:20-alpine` 构建 → `nginx:alpine` 托管 `/app/dist`） |
| Nginx | `frontend/nginx.conf`：SPA 回退 `try_files … /index.html`；`location ^~ /api/` 反代到**硬编码的云托管地址**；SSE 免缓冲配置；静态资源缓存；50x 错误页 |
| 数据库 | 外部托管 PostgreSQL（阿里云 RDS）+ pgvector |
| 编排 | **无 `docker-compose.yml`** |

### 10.2 配置与密钥

| 文件 | 是否入库 | 内容 |
|------|---------|------|
| `application.yml` | ✅ 入库 | 默认配置 + 环境变量占位；**含 JWT 密钥兜底默认值** |
| `application-dev.yml` | ❌ 已被 `.gitignore` 排除 | 真实 DashScope Key、阿里云 RDS 连接与口令、SearchAPI Key |
| `application-prod.yml` | ✅ 入库 | 仅 `flyway.clean-disabled` 与日志级别；**未覆盖 DashScope Key / DB / JWT 密钥 / 存储路径** |

> ✅ 真实密钥**未提交到仓库**（`.gitignore` 已排除 `application-dev.yml` / `application-prod.yml`）。
> 但 `application-prod.yml` 已被跟踪，且生产 profile 缺少关键项覆盖与启动校验——
> 未注入环境变量时会**带着默认 JWT 密钥启动**，这是必须修的安全问题（§14 P0-5）。

### 10.3 目标部署形态（规划）

```
┌────────────────────────────────────────────────────────────┐
│  Docker Compose（单机 8C16G，500G SSD）                     │
│  ┌──────────┐  ┌───────────┐  ┌──────────────────────┐    │
│  │ nginx    │→ │ backend   │  │ postgres + pgvector  │    │
│  │ :80/443  │  │ :8080     │  │ :5432                │    │
│  │ 前端静态  │  │           │  └──────────────────────┘    │
│  └──────────┘  └───────────┘  ┌──────────────────────┐    │
│                                │ minio（可选）        │    │
│                                └──────────────────────┘    │
│  共享卷：pgdata / minio_data / agent-data（工作目录）        │
└────────────────────────────────────────────────────────────┘
```

---

## 11. 测试现状与策略

### 11.1 现有覆盖（14 个测试类 / 约 81 个 `@Test`）

| 测试类 | 数量 | 覆盖 |
|--------|------|------|
| `DocumentAccessCheckerTest` | 16 | 角色×密级读矩阵、文档级授权突破密级、`canWrite` 优先级 |
| `DocumentPermissionServiceTest` | 15 | grant upsert、revoke、只读角色拒授 WRITE、防 N+1、adminOnly |
| `QaConversationServiceTest` | 10 | 会话创建 / 解析、归属隔离（他人 → 404）、消息追加、级联删除 |
| `AuditLogServiceTest` | 7 | Specification 过滤（userId / action / 时间范围）、多种日期格式、分页排序 |
| `PermissionAspectTest` | 6 | ADMIN 直通、adminOnly 拒绝、只读角色写拒绝、CONFIDENTIAL minLevel 拒绝、DENIED 审计写入 |
| `ToolCallAgentEventTest` | 5 | 事件触发顺序与类型（stub ChatModel） |
| `BaseAgentRunTest` | 4 | 执行循环、最大步数截断、状态流转、异常路径 |
| `DocumentConverterTest` | 4 | Document→VO、chunk→VO、状态解析（含 DELETED 覆盖） |
| `AsyncConfigTest` | 4 | 线程池参数、拒绝策略、关机等待 |
| `UserStatusTest` | 3 | 枚举 code / label / isEnabled |
| `QaServiceTest` | 3 | 会话创建、会话 ID 透传、答案流 |
| `DocumentIndexFailureHandlerTest` | 2 | 标记 FAILED + 错误信息 |
| `AgentToolConfigTest` | 1 | 6 个工具全部注册 |
| `ToolCallSmokeTest` | 1 | 工具调用冒烟 |

### 11.2 未覆盖的关键路径

| 未覆盖 | 风险 |
|--------|------|
| `RagRetriever`（召回 / 过滤下推 / 查询改写） | RAG 核心链路无回归保护 |
| `ChatService` | Advisor 装配、上下文注入无测试 |
| `DocumentIndexListener`（AFTER_COMMIT 发布链路） | 并发可见性这类已修过的 bug 可能复发 |
| `DocumentUploadService` / `FileStorageService` | 文件名清洗逻辑无测试（安全相关） |
| `JwtUtil` / `AuthService` | 认证链路无测试 |
| `AuditLogAspect` | 审计写入无测试 |
| `GlobalExceptionHandler` | 异常映射无测试（且已知缺参数校验分支） |
| `SuperAgentService` / `AgentController` | SSE 编排与落库无测试 |
| 6 个 agent 工具 | 路径穿越 / SSRF 防护无测试 |
| `DocumentAccessFilter` / `PermissionService` | 权限四路不一致的根因 |
| 集成测试 / Testcontainers | **完全没有** |
| 前端 | **完全没有测试框架** |

### 11.3 策略建议

1. 引入 **Testcontainers（PostgreSQL + pgvector）** 做集成测试，覆盖上传→索引→召回→引用全链路。
2. 权限矩阵用**参数化测试**锁定一张表（见 §14 P0-2）。
3. agent 工具补 **SSRF / 路径穿越** 安全用例。
4. 前端引入 Vitest，至少覆盖 SSE 解析器与权限工具函数。

---

## 12. 与 V1 设计稿的差异

| # | V1 设计稿 | 实际实现 | 原因 |
|---|-----------|---------|------|
| 1 | Plan-and-Execute 规划器（JSON 计划 + 用户编辑） | **ReAct 工具调用循环**，无计划对象 | 计划器未开工；`agent_tasks`/`agent_steps` 表已在 schema 中 DROP |
| 2 | LangGraph Python 微服务 | 无此模块，智能体为 Java 侧实现 | 改为先在 Java 侧跑通闭环 |
| 3 | HITL 人工确认 | 无 | 写类工具尚未定义 |
| 4 | 工具：knowledge_search / document_read / content_generation / data_analysis / api_call | **readFile / writeFile / generatePDF / downloadResource / scrapeWebPage / searchWeb / doTerminate** | 工具集整体重定位为「网络 + 文件」能力 |
| 5 | Milvus / 独立向量库 | pgvector（与业务表同库） | 与 V1 决策一致，**保持** |
| 6 | MinIO 对象存储 | 本地文件系统 | 部署环境无 MinIO；接口已抽象在 `FileStorageService` |
| 7 | Redis（会话 / 缓存 / 限流） | 未启用 | 依赖与配置被注释，无代码 |
| 8 | DeepSeek（chat + bge-large-zh embedding） | 阿里云百炼 DashScope（qwen-plus + text-embedding-v3） | 统一到同一模型平台 |
| 9 | Java 17 | **Java 21** | — |
| 10 | MyBatis-Plus | **Spring Data JPA / Hibernate** | — |
| 11 | Flyway 迁移 | Hibernate `ddl-auto: update` + 手工全量脚本 | — |
| 12 | 分块 512 token + 50 重叠 | **1000 token / 最小 400 字符 / 无重叠** | Spring AI `TokenTextSplitter` 不支持 overlap |
| 13 | 独立 `document_chunks` 表 | **并入 `vector_store`（Spring AI 自维护）** | 与 V1 的部分决策一致 |
| 14 | 权限过滤 SQL JOIN | **过滤表达式下推向量库** | 与 V1 决策一致，**保持** |
| 15 | JWT 有效期 8 小时 + Refresh Token | **7 天，无 Refresh Token** | — |
| 16 | 意图自动路由 | 无 | — |
| 17 | 连接器（飞书 / 企微） | 仅占位包 | — |
| 18 | docker-compose 编排 | 无 | — |

---

## 13. 已知技术债与风险登记

| # | 债务 / 风险 | 位置 | 影响 | 严重度 |
|---|------------|------|------|--------|
| T1 | 权限判定四路不一致 | `PermissionService` / `DocumentAccessChecker` / `DocumentAccessFilter` / 列表 SQL | 越权风险；未来新增机密接口只挂注解即会漏 | 🔴 |
| T2 | 智能体无法检索知识库 | `AgentToolConfig` | 超级智能体核心价值缺失 | 🔴 |
| T3 | 智能体产物不可取回 | agent tools | 「生成报告」闭环断裂 | 🔴 |
| T4 | 生产密钥无启动校验 | `application.yml` / `application-prod.yml` | 默认 JWT 密钥可上线 | 🔴 |
| T5 | 审计不覆盖读操作 | `AuditLogAspect` | 合规目标未达成 | 🔴 |
| T6 | 审计注解标在 Controller | `PermissionController` | 违反分层规约 | 🟠 |
| T7 | 智能体跑在默认 ForkJoinPool | `SuperAgentService` | 长任务与索引任务抢资源 | 🟠 |
| T8 | Agent 的 ChatClient 缺 `SensitiveWordAdvisor` | `SuperManus` | 智能体输入无内容安全拦截 | 🟠 |
| T9 | `MyLoggerAdvisor` 打印完整提示词与模型输出 | `common/advisor` | 敏感内容进日志 | 🟠 |
| T10 | 敏感词词库硬编码且只查输入 | `SensitiveWordAdvisor` | 不可配置；上下文与输出未覆盖 | 🟠 |
| T11 | enricher 元数据被丢弃 | `VectorStoreService.addChunks` | 向量库缺富元信息 | 🟡 |
| T12 | 参数校验异常落到 500 | `GlobalExceptionHandler` | 前端拿到错误状态码 | 🟡 |
| T13 | 文档列表返回手写 Map | `DocumentController` | 分页响应结构与约定不符 | 🟡 |
| T14 | 记忆无窗口裁剪 | `DatabaseChatMemory` | 表无限增长 | 🟡 |
| T15 | 单文档分块最多返回 500 条 | `DocumentChunkRepository` | 超长文档静默截断 | 🟡 |
| T16 | 上传无扩展名白名单 | `DocumentUploadService` | 非法文件延迟失败 | 🟡 |
| T17 | 无文档指纹去重 / 重新索引 | 知识库模块 | 重复上传产生重复向量 | 🟡 |
| T18 | 智能体工作目录不按用户隔离 | `AgentToolProperties` | 跨用户中间文件可见 | 🟡 |
| T19 | `AuditLogEntity` 在 `permission` 包 | 包结构 | 分层异味 | 🟢 |
| T20 | 过期注释（`schema.sql` status 注释、`package-info` 分块说明、`file_key` 注释） | 多处 | 误导后续开发 | 🟢 |
| T21 | 前端无测试 | `frontend/` | 回归风险 | 🟡 |
| T22 | 无集成测试 | `backend/src/test` | RAG / 上传 / 索引全链路无保护 | 🟠 |

---

## 14. 下一步优化方向

### P0（1–2 周内）

**P0-A　权限单一事实源**
- 抽取 `PermissionPolicy`（角色 × 密级 × 操作 → 判定 + 文档级授权合并），提供
  `canRead(role, level, grantedDocs)` / `canWrite(...)` / `visibleSecurityLevels(role)`。
- `PermissionService`、`DocumentAccessChecker`、`DocumentAccessFilter`、列表查询**全部改为调用它**。
- 列表查询由原生 SQL 改为 `Specification`（可复用同一套规则），消除 SQL 里的规则副本。
- **验收**：一张矩阵表 + 一份参数化单元测试锁定；四路结论完全一致。

**P0-B　智能体接入知识库**
- 新增 `KnowledgeSearchTool` / `DocumentReadTool` 两个 `@Tool`，内部复用 `RagRetriever` 与
  `DocumentAccessChecker`；过滤表达式沿用 `DocumentAccessFilter`，与问答链路共享权限语义。
- 工具返回值携带 `documentId` / `title` / `chunkIndex`，前端引用可跳转。
- **验收**：智能体回答中出现企业文档引用；无权文档在工具层被拒。

**P0-C　智能体产物可交付**
- 产物路径改为 `data/agent/output/{conversationId}/{文件名}`，工具返回值带产物相对路径。
- 新增 `GET /agent/conversations/{id}/artifacts`（列表）与 `GET /agent/artifacts/{id}`（下载，带权限校验）。
- **验收**：前端可下载生成的 PDF / Markdown。

**P0-D　审计补齐读操作 + 注解下沉**
- 为登录 / 登出 / 文档详情 / 分块阅读 / 问答 / 智能体调用补 `@AuditLog`。
- 把 `PermissionController` 上的 `@AuditLog` 迁到 `UserAdminService` / `DocumentPermissionService`。
- 把 `AuditLogEntity` / `AuditLogRepository` 迁到 `audit` 包。
- **验收**：审计页可回答「谁在什么时候看了哪份文档 / 问了什么问题」。

**P0-E　生产基线加固**
- `application-prod.yml` 补齐 DashScope / DB / JWT / 存储路径项，并**在启动时校验关键密钥非空**（缺失则拒绝启动）。
- 提供 `.env.example` 与 `docker-compose.yml`（postgres+pgvector / backend / frontend+nginx / 可选 minio）。
- 引入 `spring-boot-starter-actuator` 或移除白名单里的 `/actuator/**`；移除不存在的 `/auth/register` 白名单。
- **验收**：一条命令拉起全栈；无默认密钥无法启动。

### P1（3–6 周）

| 编号 | 事项 | 要点 |
|------|------|------|
| P1-A | Plan-and-Execute 规划器 | LLM 生成 JSON 计划 → 前端展示 → 可改单步 / 删步 / 重排 → 执行；复用 schema 中已 DROP 的 `agent_tasks` / `agent_steps` 表结构 |
| P1-B | HITL 人工确认 | 写类工具暂停等待确认；补 `agent.pending.confirm` 事件与 `agent.step.skip` |
| P1-C | 意图路由 | 单一入口自动分流「简单问答 / 复杂任务」，省去用户选择 |
| P1-D | 文档增量索引 | 内容哈希去重 + 「重新索引」入口 + 失败可重试 |
| P1-E | 执行可干预 | 暂停 / 跳过 / 单步重跑 |
| P1-F | 成本可控 | 规划用强模型、执行用廉价模型；步骤级 token 与费用统计 |
| P1-G | 智能体线程池 | 在 `AsyncConfig` 增加专用 `agentExecutor`，替换 `CompletableFuture.runAsync` |
| P1-H | 统一分页响应 | `DocumentController.list` 改用 `PageResult`；补 `MethodArgumentNotValidException` → 400 |
| P1-I | 集成测试 | Testcontainers（PostgreSQL + pgvector），覆盖上传→索引→召回→引用 |

### P2（6 周以后）

| 编号 | 事项 | 要点 |
|------|------|------|
| P2-A | 检索质量 | 混合检索（BM25 + 向量）+ 重排序 + 父子分块（子块召回、父块喂模型） |
| P2-B | 上下文组装归位 | 评估改用 `QuestionAnswerAdvisor` / `RetrievalAugmentationAdvisor`，消除手写组装 |
| P2-C | 富元数据 | `VectorStoreService.addChunks` 合并 enricher 元数据而非重建 |
| P2-D | 引用精细化 | 记录页码 / 章节路径，前端定位到段落 |
| P2-E | 反馈闭环 | 点赞率 / 索引成功率 / 命中率在工作台聚合展示 |
| P2-F | 存储抽象 | `FileStorageService` 抽接口，本地 + MinIO 双实现，配置切换 |
| P2-G | 内容安全加固 | 敏感词词库配置化；覆盖 RAG 上下文与模型输出；`MyLoggerAdvisor` 脱敏 |
| P2-H | 记忆窗口 | `DatabaseChatMemory` 增加保留条数上限 |
| P2-I | 文档下载 | 提供原始文件下载接口（带权限校验） |
| P2-J | 连接器 | 飞书 / 企微同步（择一） |
| P2-K | 前端测试 | Vitest + 关键路径用例 |

### 附：文档同步待办

以下文件仍描述旧形态，需随代码一并修订：

| 文件 | 需修订内容 |
|------|-----------|
| `CLAUDE.md` | 技术栈表（Java 17→21、MinIO→本地存储、Redis→未启用、MyBatis→JPA）；「Plan-and-Execute」段落改为「ReAct 运行时」；「HITL」段落标注为规划中；工具清单段落替换为实际 6 个工具；包路径 `com/company` → `com/leon` |
| `README.md` | 技术栈表、目录结构（包名、缺失的 `agent-service`）、里程碑状态 |
| `doc/产品文档/企业知识管理Agent_原型_MVP.html` | 原型含计划编辑 / 人工确认等未实现交互，需在 P1-A / P1-B 落地后再作为交付依据 |
| `knowledge/package-info.java` | 分块策略说明（512+50 → 1000 token / 无重叠） |
| `schema.sql` | `documents.status` 注释；`documents.file_key` 注释（「MinIO 对象键」→ 本地相对路径） |
| `permission/model/DocPermission.java` | Javadoc「支持按用户或按部门授权」→ 仅用户级 |

---

> 📌 **文档维护**：本文档描述系统的**实际状态**，与代码不一致时**以代码为准**并回写本文档。
> 每次架构性变更（如引入 Plan-and-Execute、切换存储、接入连接器）需同步更新 §3 技术栈、§12 差异表与 §13 技术债表。
