# Design

## Context

当前项目状态：
- `knowledge` 包仅有 `package-info.java` 骨架，无任何实现类
- 数据库已通过 Flyway V1 迁移创建了 `documents` 和 `document_chunks` 表
- `documents` 表已有字段：id, title, file_key, file_type, file_size, security_level, owner_id, department, summary, tags, status, created_at, updated_at, deleted
- `document_chunks` 表已有字段：id, document_id, chunk_index, content, token_count, embedding(vector(1024)), created_at
- 前端 `Upload.vue` 已有完整 UI 骨架，`handleUpload` 中有 TODO 待对接
- 已有基础设施：`@RequirePermission` 权限注解、`@AuditLog` 审计注解、`R<T>` 统一响应、`SecurityContextUtil` 当前用户获取
- 已有依赖：Apache Tika 2.9.2、PostgreSQL + pgvector、Redis、MyBatis-Plus 3.5.7
- 已有配置：`agenthub.storage.root-path`、`agenthub.llm.base-url`、`agenthub.chunk.size/overlap`
- 包结构使用 `com.leon.agenthub`（不是 CLAUDE.md 中示例用的 `com.company.agenthub`）
- API 上下文路径为 `/api`（`server.servlet.context-path`），Knife4j 已配置

## Goals / Non-Goals

**Goals:**
- 实现完整的文档上传→解析→分块→向量化→AI 摘要链路
- 实现文档列表/详情/删除接口
- 前端 Upload.vue 对接真实接口
- 复用现有权限和审计基础设施
- 异步处理摘要生成，不阻塞上传响应

**Non-Goals:**
- 扫描型 PDF OCR 解析（MVP 不支持）
- MinIO 集成（MVP 使用本地文件系统，`agenthub.storage.root-path`）
- 文档版本管理
- 批量上传进度条实时推送（MVP 仅返回 PROCESSING 状态）
- 语义分块（MVP 使用滑动窗口分块）

## Decisions

### 决策 1：上传链路同步 vs 异步处理

**选择**：文件接收 + 元数据写入同步执行（立即返回 201 + PROCESSING 状态），Tika 解析→分块→向量化→AI 摘要通过 Spring `@Event` + `@Async` 异步处理。

**理由**：
- 同步全链路（含 Embedding 调用 + LLM 调用）可能耗时 30-60 秒，会触发 HTTP 超时
- 异步处理让上传接口快速返回，前端可轮询状态或后续通过列表页查看
- Spring 内置 `@Async` 足够，无需引入消息队列（MVP 阶段）

**备选方案**：
- 纯同步：实现简单但用户体验差，大文件上传会超时
- MQ（RabbitMQ/Kafka）：过度设计，MVP 不需要

### 决策 2：Embedding 服务选型 — 灵积平台（DashScope）

**选择**：使用阿里云**灵积平台（DashScope）**的通用文本向量接口 `text-embedding-v3`，通过 OpenAI 兼容模式调用。

**理由**：
- 灵积平台 `text-embedding-v3` 默认输出 1024 维向量，与 pgvector 的 `vector(1024)` 列和 bge-large-zh 维度完全对齐
- 支持可配置的 dimensions 参数（1024/512/256），方便后续降维实验
- 支持单次请求批量输入（最多 10 条文本），减少分块上传时的 API 调用次数
- 通过 OpenAI 兼容协议（`/compatible/v1/embeddings`）调用，后续切换其他兼容服务（如 DeepSeek、OpenAI）时只需改 base-url，无需改代码
- 所有连接参数（base-url、api-key、model、dimensions、timeout）均通过配置文件注入，运行时可通过环境变量覆盖

**API 规格**：
- 端点：`POST {base-url}/compatible/v1/embeddings`
- 请求头：`Authorization: Bearer {api-key}`
- 请求体：`{"model": "text-embedding-v3", "input": ["文本1", "文本2"], "dimensions": 1024}`
- 响应：`{"data": [{"embedding": [0.001, ...], "index": 0}], "usage": {"total_tokens": 100}}`

**备选方案**：
- 灵积原生 API（`/api/v1/services/embeddings/text-embedding/text-embedding`）：功能相同，但 OpenAI 兼容模式更通用
- 本地部署 bge-large-zh 模型：需要 GPU 或大量 CPU 资源，MVP 阶段不划算

### 决策 3：文件存储策略

**选择**：MVP 使用本地文件系统（`agenthub.storage.root-path`），文件以 `{docId}/{originalFilename}` 结构存储。

**理由**：
- 与 `package-info.java` 中预留的设计一致："MVP 使用本地文件系统，后续迁移到 MinIO / S3 时仅替换 service 层存储实现"
- 不引入 MinIO 依赖，简化部署
- 后续迁移只需替换 `FileStorageService` 实现

### 决策 4：分块 token 计数方式

**选择**：使用简单的字符/空格分词估算 token 数（中文约 1.5 字符/token，英文约 4 字符/token），而非引入完整 tokenizer。

**理由**：
- MVP 阶段不需要精确 token 计数，512 token 的滑动窗口用字数估算足够
- 引入 HuggingFace tokenizer 会增加 Python 依赖或 JNI 调用复杂度
- Embedding 服务本身会自动处理超长文本的截断

### 决策 5：数据库字段补充

**选择**：通过 Flyway V3 迁移补充 `documents` 表的 `chunk_count` 和 `error_message` 字段。

**理由**：
- V1 迁移中 `documents` 表缺少 `chunk_count`（记录分块数量）和 `error_message`（记录失败原因）
- Flyway 已是项目标准迁移方式，保持一致性
- 不修改 V1（已执行），新增 V3 追加字段

### 决策 6：API 路径与上下文路径

**选择**：Controller 使用 `@RequestMapping("/documents")`，配合 `server.servlet.context-path: /api`，最终路径为 `/api/documents/upload` 等。

**理由**：
- 与前端 `Upload.vue` 中 `action="/api/knowledge/upload"` 不一致——需统一
- 技术设计文档 §7.1 定义路径为 `/api/documents/upload`
- 决定将前端 action 改为 `/api/documents/upload` 以对齐技术设计

## Risks / Trade-offs

- **[大文件解析内存溢出]** → Tika 解析大 PDF/PPTX 时可能占用大量内存。缓解：配置 Tika 的 `maxStringLength` 限制，超过阈值的文件标记为 FAILED 并提示用户。
- **[Embedding API 限流]** → 批量上传时 Embedding 调用可能触发限流。缓解：异步队列串行处理，单文档内分块批量调用 Embedding API（支持 batch 接口时）。
- **[本地磁盘空间耗尽]** → 100 篇文档 × 平均 5MB = 500MB，MVP 可控。缓解：监控磁盘使用，后续迁移到 MinIO。
- **[Flyway 迁移失败]** → V3 迁移如果字段已存在会报错。缓解：使用 `IF NOT EXISTS` 条件判断。
- **[异步任务丢失]** → 应用重启时正在处理的异步任务会丢失。缓解：MVP 可接受，后续引入持久化任务队列。

## Migration Plan

1. 执行 Flyway V3 迁移（补充 `chunk_count`、`error_message` 字段）
2. 部署后端代码
3. 部署前端更新（Upload.vue action 路径修改）
4. 验证：上传一篇测试 PDF，检查 documents + document_chunks 表数据
5. 回滚策略：Flyway 迁移可回滚（V3 undo 脚本），后端代码可回滚到上一版本

## Open Questions

- LLM 摘要生成使用的具体 Prompt 模板是否需要产品确认？（MVP 可使用通用模板）
