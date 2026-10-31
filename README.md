# 智知 AgentHub

企业知识管理平台 — 文档知识库 + RAG 问答 + 工具调用型 SuperAgent。

面向中小企业（50–500 人）：把散落的企业文档变成可问答的知识源，并让智能体自主调用工具完成复杂任务。

> 产品文档：`doc/产品文档/企业知识管理Agent_PRD_MVP.md`
> 技术设计：`doc/技术设计/智知AgentHub_技术设计文档_MVP.md`
> 原型：`doc/产品文档/企业知识管理Agent_原型_MVP.html`
> 开发规范（给 AI 助手）：`CLAUDE.md`

---
线上体验地址：https://agent-hub-fronted-337702-13-1520070688.sh.run.tcloudbase.com

<img width="1920" height="919" alt="image" src="https://github.com/user-attachments/assets/592ce7e7-2ea7-48ce-9663-cd8e4cc0814d" />

---

## 当前完成情况

| 模块 | 状态 | 说明 |
|------|------|------|
| 认证 Auth | ✅ 已完成 | JWT 登录/登出、当前用户信息、修改资料与密码、BCrypt 加密 |
| 知识库 Knowledge | ✅ 已完成 | 上传 → Tika 解析 → 分块 → Embedding → pgvector 入库 → LLM 摘要/关键词；列表/详情/分块查看/软删除 |
| 权限 Permission | ✅ 已完成 | 4 角色（ADMIN/EDITOR/VIEWER/GUEST）× 3 密级（PUBLIC/INTERNAL/CONFIDENTIAL）+ 文档级授权 `doc_permissions` |
| 审计 Audit | ✅ 已完成 | `@AuditLog` 注解 + AOP 切面自动写 `audit_logs` |
| RAG 问答 QA | ✅ 已完成 | 查询改写 → 权限过滤下推 → top-K 召回 → SSE 流式回答 + 引用来源 + 会话历史 |
| SuperAgent | ✅ 已完成（ReAct 版） | Java 侧 ReAct 循环，模型自主选择工具；6 个工具；SSE 推送步骤时间线；会话持久化 |
| Plan-and-Execute 规划器 | ⏳ 规划中 | `agent.plan` / `agent.step.skip` 事件未产生，LangGraph Python 微服务尚未落地 |
| 连接器 Connector | ⏳ 未开始 | `connector/` 目前只有 `package-info.java` 占位 |
| Redis / MinIO | ⏳ 未启用 | Redis 依赖与配置已注释；文件存储走本地磁盘（`agenthub.storage.root-path`），迁移 MinIO 时只替换 `FileStorageService` |

### SuperAgent 工具集

模型自行决定调用哪些工具、调用顺序，最多 20 步：

| 工具 | 能力 |
|------|------|
| `FileOperationTool` | 工作目录内读写文件 |
| `WebSearchTool` | 网页搜索（需配置 `SEARCH_API_KEY`，留空则该工具不可用） |
| `WebScrapingTool` | 网页正文爬取（Jsoup） |
| `ResourceDownloadTool` | 资源下载 |
| `PDFGenerationTool` | 生成 PDF（iText，含中文字体包） |
| `TerminateTool` | 终止协议，模型主动结束 |

---

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3 + TypeScript + Element Plus 2.8 + TailwindCSS 3 + Vite 5 + Pinia + markdown-it |
| 后端 | Spring Boot 3.3.4 + Java 21 + Spring Data JPA (Hibernate) |
| AI | Spring AI 1.0.0-M6 / Spring AI Alibaba 1.0.0-M6.1 |
| LLM | 阿里云百炼 DashScope：`qwen-plus`（对话）/ `text-embedding-v3`（嵌入，1024 维） |
| 数据库 | PostgreSQL 16 + pgvector（向量与业务数据同库，HNSW 索引 + 余弦距离） |
| 文件存储 | 本地磁盘（后续迁移 MinIO / S3） |
| 接口文档 | Knife4j 4.5（OpenAPI 3） |
| 智能体运行时 | Java 侧自研 ReAct 循环（`ToolCallAgent` → `SuperManus`），非 LangGraph |

---

## 目录结构

```
agent_hub/
├── backend/                       # Spring Boot 3 后端
│   ├── pom.xml
│   ├── docker/Dockerfile          # 后端镜像（openjdk:21-slim）
│   └── src/main/
│       ├── java/com/leon/agenthub/
│       │   ├── AgentHubApplication.java
│       │   ├── auth/              # JWT 认证：登录、Security 配置、JWT 工具
│       │   ├── knowledge/         # 文档：parser 解析 / spliter 分块 / enricher 摘要 / service 编排
│       │   ├── qa/                # 问答：rag 召回 / rag 上下文 / service 会话与流式
│       │   ├── agent/             # 智能体：base 运行时 / superagent / tool 工具集 / controller
│       │   ├── permission/        # 注解 @RequirePermission、Aspect、角色密级、文档授权、审计日志
│       │   ├── audit/             # @AuditLog 注解 + AOP 切面
│       │   ├── connector/         # 连接器（占位，未实现）
│       │   └── common/            # R 响应、业务异常、LLM 封装、advisor、分页 DTO
│       ├── resources/
│       │   ├── application.yml    # 主配置（默认 profile=dev）
│       │   ├── application-dev-example.yml  # dev 配置模板（已隐去凭据，复制成 application-dev.yml 使用）
│       │   └── db/migration/schema.sql   # 全量重建脚本（含初始管理员，手动执行）
│       └── test/                  # 14 个测试类
├── frontend/                      # Vue 3 + TS 前端
│   ├── Dockerfile + nginx.conf    # 前端镜像 + 反向代理（含 SSE 配置）
│   ├── .env.development           # VITE_PROXY_TARGET 等环境变量
│   └── src/
│       ├── api/                   # axios 封装，按域拆分 auth/knowledge/qa/agent/permission
│       ├── views/                 # Login / Dashboard / Knowledge / QAM / Agent / Permission / Profile
│       ├── components/            # ConversationList、ProfileDrawer、ThemeToggle
│       ├── composables/           # useResponsive（移动端适配）、useProfile
│       ├── stores/                # Pinia：auth、theme
│       ├── router/                # 路由 + 登录守卫
│       └── utils/                 # permission 权限判断、storage 持久化
├── doc/
│   ├── 产品文档/                   # PRD + 可交互原型
│   ├── 技术设计/                   # 技术设计文档
│   └── 知识库示例文档/             # 可直接上传体验的样例文档
├── openspec/                      # 规格驱动开发：specs + changes（proposal/design/specs/tasks）
└── CLAUDE.md                      # 开发规约（分层、事务、枚举、RAG）
```

---

## 快速开始

### 前置条件

- JDK 21、Maven 3.8+
- Node.js 18+、npm 9+
- PostgreSQL 16（需安装 `pgvector` 与 `uuid-ossp` 扩展）
- 阿里云百炼 API Key（对话 + 嵌入）

### 1. 准备数据库

```bash
createdb agent_hub
psql agent_hub -f backend/src/main/resources/db/migration/schema.sql
```

`schema.sql` 是**全量重建脚本**（开头会 DROP 业务表），仅用于全新搭建或推倒重来。
日常开发表结构由 Hibernate 自动维护（`spring.jpa.hibernate.ddl-auto: update`），无需执行脚本。

初始管理员：`admin` / `admin123`（登录后请立即改密）。

### 2. 配置

仓库提供配置模板 `backend/src/main/resources/application-dev-example.yml`，**已隐去所有真实凭据**。复制一份并填入自己的值：

```bash
cd backend/src/main/resources
cp application-dev-example.yml application-dev.yml
```

`application-dev.yml` 已被 `.gitignore` 忽略，不会误提交。模板里全部字段都支持环境变量覆盖，例如：

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:sk-your-api-key-here}   # 阿里云百炼 Key
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/${DB_NAME:agent_hub}?stringtype=unspecified
    username: ${DB_USER:agenthub}
    password: ${DB_PASS:agenthub}
```

也可全部用环境变量覆盖，无需改文件：

| 变量 | 说明 | 默认 |
|------|------|------|
| `DASHSCOPE_API_KEY` | 百炼 API Key | 空 |
| `DB_HOST` / `DB_NAME` / `DB_USER` / `DB_PASS` | 数据库连接 | localhost / agent_hub / agenthub / agenthub |
| `JWT_SECRET` | JWT 签名密钥 | **必须改**，生产环境强随机串 |
| `STORAGE_ROOT_PATH` | 文档存储根目录 | `./data/docs` |
| `AGENT_TOOL_WORK_DIR` | 智能体工具工作目录 | `./data/agent` |
| `SEARCH_API_KEY` | 网页搜索 Key（留空则该工具不可用） | 空 |
| `KNOWLEDGE_ENRICH_WITH_LLM` | 是否调 LLM 生成摘要/关键词 | true |
| `SPRING_PROFILES_ACTIVE` | 激活 profile | dev |

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
# 或
mvn clean package -DskipTests && java -jar target/agent-hub-0.1.0-SNAPSHOT.jar
```

- 服务地址：`http://localhost:8080`，context-path 为 `/api`
- 接口文档：`http://localhost:8080/api/doc.html`（Knife4j）
- 健康检查：`http://localhost:8080/api/health`

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev    # http://localhost:5173
```

Vite 把 `/api` 代理到后端：目标地址配置在 `frontend/vite.config.ts` 的 `proxyTarget`（当前写死为 `http://localhost:8080`，`.env.development` 的 `VITE_PROXY_TARGET` 暂未被读取）。
若后端不在本机，改 `vite.config.ts` 的 `proxyTarget`；注意**只填主机名+端口，不要带 `/api`**，否则会拼成 `/api/api/...`。

### 5. 体验路径

1. 用 `admin / admin123` 登录 → 仪表盘
2. 知识库 → 上传文档（支持 docx / pptx / xlsx / pdf / txt / md），可先用 `doc/知识库示例文档/` 里的样例文件
3. 智能问答 → 提问，查看流式回答与右侧引用来源抽屉
4. 超级智能体 → 提交一个任务（如"分析 Q1 返点政策并生成 PDF 报告"），观察步骤时间线
5. 权限管理 → 用户角色 / 文档授权 / 审计日志三个标签页

---

## 主要接口

所有响应统一包装为 `{"code": 0, "message": "success", "data": {...}}`，认证用 `Authorization: Bearer {jwt}`。

| 模块 | 路径 | 说明 |
|------|------|------|
| 认证 | `POST /api/auth/login`、`POST /api/auth/logout`、`GET /api/auth/me`、`PUT /api/auth/me`、`PUT /api/auth/me/password` | 登录登出与个人资料 |
| 知识库 | `POST /api/documents/upload`、`GET /api/documents`、`GET /api/documents/{id}`、`GET /api/documents/{id}/chunks`、`DELETE /api/documents/{id}` | 上传/查询/分块/软删除 |
| 问答 | `POST /api/qa/ask`（SSE）、`POST /api/qa/feedback`、`GET /api/qa/conversations`、`GET /api/qa/conversations/{id}/messages`、`DELETE /api/qa/conversations/{id}` | 流式问答与会话 |
| 智能体 | `POST /api/agent/manus/chat`（SSE）、`GET /api/agent/conversations`、`GET /api/agent/conversations/{id}/messages`、`DELETE /api/agent/conversations/{id}` | 任务执行与会话 |
| 权限 | `GET/POST/PUT/DELETE /api/permissions/users*`、`PUT /api/permissions/users/{userId}/role`、`GET/POST/DELETE /api/permissions/documents/{id}/*`、`GET /api/permissions/audit-logs` | 用户、角色、文档授权、审计 |

### SSE 事件

问答 `POST /api/qa/ask`：`delta` → `done`（含引用来源）/ `error`。

智能体 `POST /api/agent/manus/chat`（事件名由后端 `AgentEventType` 统一下发）：

| 事件 | 含义 |
|------|------|
| `agent.step.start` | 步骤开始 |
| `agent.step.progress` | 步骤进展 |
| `agent.step.complete` | 步骤完成（含工具结果） |
| `agent.complete` | 整体完成 |
| `agent.error` | 出错 |

`agent.plan` 与 `agent.step.skip` 属于 Plan-and-Execute / HITL 语义，当前 ReAct 运行时不产生。

---

## 关键设计约定

开发前请先读 `CLAUDE.md`，几条最容易踩的：

- **分层**：Controller 只转发 + 包装 `R`；业务与事务全在 Service；实体转换在 `converter/`。业务异常抛 `BusinessException`，由 `GlobalExceptionHandler` 统一转 `R.fail`。
- **事务**：`@Transactional` 只标在 Service；一次业务的多步写库必须在同一事务方法内；事务内不做不可回滚的副作用（发 HTTP / 调 LLM / 写文件）——如文档索引走 `AFTER_COMMIT` 事件异步执行。
- **枚举**：状态常量一律用 `enums` 包下的枚举类，禁止字符串字面量与魔法数字。枚举名即接口/前端使用的字符串。
- **分页**：分页参数 DTO 继承 `common/dto/PageQuery`，响应统一 `{list, total, page, size}`，不直接返回 Spring `Page`。
- **RAG**：优先用 Spring AI 已封装的能力（`VectorStore`、`RewriteQueryTransformer`、`ChatClient`、`MessageChatMemoryAdvisor`、`QuestionAnswerAdvisor`）。权限过滤必须通过 `SearchRequest.filterExpression(...)` 下推到向量库，禁止先全量召回再在 Java 侧过滤。

---

## 测试

```bash
cd backend && mvn test        # 14 个测试类
```

覆盖智能体运行时（BaseAgent / ToolCallAgent / 工具配置）、权限切面、文档授权与访问判定、审计日志、问答服务、文档转换与索引失败降级。

---

## 规格驱动开发

项目采用 [OpenSpec](https://github.com/Fission-AI/OpenSpec) 流程，规格与实现同步维护：

```
openspec/
├── specs/          # 已生效规格（user-auth 等）
└── changes/        # 进行中的变更：proposal.md + design.md + specs/ + tasks.md
    ├── document-upload/
    ├── knowledge-qa/
    ├── permission-management/
    └── super-agent-chat/
```

新增功能先写 change（proposal → design → specs → tasks），实现时逐条勾选 tasks，完成后归档到 `changes/archive/` 并同步回 `specs/`。
