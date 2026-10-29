# Proposal

## Why

智知 AgentHub 的核心价值是"让企业散落各处的文档能在一个入口秒问秒答"，但当前知识库模块仅有 `package-info.java` 骨架，没有任何实现。文档上传是整个 RAG 管线的起点——没有文档入库，后续的语义检索和智能问答都无法工作。MVP 里程碑 M1（第 1-4 周）明确要求"上传 100 篇文档"，这是当前最高优先级的阻塞项。

## What Changes

- **新增后端文档上传接口**：`POST /documents/upload`，接收 Multipart 文件 + 元信息（密级、标签），完成文件存储、文本解析、分片、向量化入库。
- **新增文档列表/详情/删除接口**：`GET /documents`、`GET /documents/{id}`、`DELETE /documents/{id}`，提供文档管理能力。
- **新增文本解析能力**：基于 Apache Tika 实现 docx/pdf/pptx/xlsx/txt/md 六种格式的纯文本提取。
- **新增文本分块能力**：按 512 token + 50 token 重叠的滑动窗口分块，优先按段落/标题边界切分。
- **新增向量化入库**：调用 Embedding 服务（bge-large-zh），将文本分块写入 `document_chunks` 表的 `vector(1024)` 列。
- **新增 AI 摘要与标签生成**：上传完成后异步调用 LLM 生成文档摘要和自动标签，回写 `documents` 表。
- **新增前端上传页对接**：替换 `Upload.vue` 中的 TODO，对接真实后端接口。
- **复用现有基础设施**：`@RequirePermission` 权限注解、`@AuditLog` 审计注解、`R<T>` 统一响应、Flyway 数据库迁移、Redis 状态缓存。

## Capabilities

### New Capabilities

- `document-management`：文档上传、解析、分片、向量化入库、元信息管理（密级/标签/摘要）、文档列表/详情/删除，以及前端上传页对接。涵盖知识库模块从文件接收到可被 RAG 检索的完整链路。

### Modified Capabilities

- 无。当前没有已存在的知识库规格需要修改。

## Impact

- **代码变更范围**：
  - 新增 `backend/src/main/java/com/leon/agenthub/knowledge/` 下的 controller、service、parser、chunker、dto、config、mapper 等子包
  - 新增 Flyway 迁移脚本（如 `documents` 表需补充 `status=PROCESSING` 字段或 `chunk_count` 字段，视 V1 迁移现状而定）
  - 修改 `frontend/src/views/Knowledge/Upload.vue` 对接真实接口
  - 新增 Embedding 服务客户端（`common/client/EmbeddingClient.java`）和 LLM 摘要服务（`common/client/LlmSummaryClient.java`）
- **依赖新增**：无外部新依赖，Apache Tika、pgvector JDBC 驱动、Redis 已存在于 `pom.xml`。
- **API 新增**：`POST /documents/upload`、`GET /documents`、`GET /documents/{id}`、`DELETE /documents/{id}`。
- **数据库**：复用已有的 `documents`、`document_chunks` 表，可能需要通过 Flyway 迁移补充字段（如 `chunk_count`、`parse_status`）。
- **配置变更**：`application.yml` 可能需要新增 Embedding/LLM 服务地址配置（部分已有占位）。
