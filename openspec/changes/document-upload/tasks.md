# Tasks

## 1. 基础设施与数据模型

- [x] 1.1 创建 Flyway V3 迁移脚本，为 `documents` 表补充 `chunk_count`（INT, 默认 0）和 `error_message`（TEXT, 可空）字段，并在 `backend/src/main/resources/db/migration/` 下创建文件 `V3__add_document_chunk_count.sql`。验证：执行 `mvn flyway:migrate` 成功，查询表结构确认字段存在。

- [x] 1.2 在 `knowledge/dto/` 下创建数据传输对象：`DocumentUploadRequest`（securityLevel、tags）、`DocumentVO`（id、title、fileType、fileSize、securityLevel、summary、tags、status、chunkCount、createdAt）、`DocumentQuery`（page、size、securityLevel、tag、status）。验证：DTO 类编译通过，字段与 spec 要求一致。

- [x] 1.3 在 `knowledge/config/` 下创建 `KnowledgeProperties` 配置类，绑定 `agethub.*` 前缀的 chunk.size、chunk.overlap、storage.root-path 等配置项。验证：配置类编译通过，`@ConfigurationProperties` 注解正确。

## 2. 文档存储与解析

- [x] 2.1 实现 `FileStorageService`：提供 `store(file, docId)` 方法将 MultipartFile 保存到 `{rootPath}/{docId}/{filename}`，提供 `getFilePath(docId, filename)` 方法。验证：单元测试验证文件写入和读取路径正确。

- [x] 2.2 实现 `DocumentParser`：封装 Apache Tika，提供 `parse(filePath)` 方法返回纯文本字符串，支持 docx/pdf/pptx/xlsx/txt/md 六种格式，解析失败抛出 `DocumentParseException`。验证：分别上传各格式测试文件，确认能提取出非空文本。

- [x] 2.3 实现 `TextChunker`：提供 `chunk(text, chunkSize, overlap)` 方法返回 `List<String>`，优先按段落/Markdown 标题边界切分，超长段落按句子切分。验证：单元测试验证短文本返回单块、长文本返回多块且有重叠。

## 3. Embedding 与向量化

- [x] 3.1 在 `common/client/` 下创建 `EmbeddingClient`：通过 HTTP 调用灵积平台 DashScope 通用文本向量接口（OpenAI 兼容模式），提供 `embed(texts)` 方法返回 `List<float[]>`，支持单条和批量调用（单次最多 10 条）。请求路径 `{base-url}/compatible/v1/embeddings`，使用 Bearer Token 认证。验证：mock HTTP 服务测试返回正确维度（1024）的向量，批量调用正确拆分为多请求。

- [x] 3.2 创建 `DocumentChunkMapper`（MyBatis-Plus BaseMapper 扩展），提供 `insertBatch` 批量写入 chunks 的能力。验证：集成测试验证批量插入 chunks 成功，embedding 列正确写入。

- [x] 3.3 在 `application.yml` 中补充 灵积平台 Embedding 服务配置项：`agethub.embedding.base-url`（默认 `https://dashscope.aliyuncs.com`）、`agethub.embedding.api-key`（默认空，通过环境变量 `DASHSCOPE_API_KEY` 注入）、`agethub.embedding.model`（默认 `text-embedding-v3`）、`agethub.embedding.dimensions`（默认 1024）、`agethub.embedding.batch-size`（默认 10）、`agethub.embedding.connect-timeout-ms`（默认 5000）、`agethub.embedding.read-timeout-ms`（默认 30000）。创建 `EmbeddingProperties` 配置类通过 `@ConfigurationProperties` 绑定，环境变量可覆盖默认值。验证：配置加载正确，注入可用。

## 4. AI 摘要与标签

- [x] 4.1 在 `common/client/` 下创建 `LlmSummaryClient`：调用 LLM 生成文档摘要和标签，提供 `generateSummaryAndTags(text)` 方法返回 `SummaryResult(summary, tags)`。验证：mock LLM 服务测试返回格式正确的摘要和标签。

- [x] 4.2 实现异步摘要处理：使用 Spring `@Async` 事件监听，文档向量入库后触发 `DocumentIndexedEvent`，监听器调用 `LlmSummaryClient` 回写 documents 表。验证：上传文档后等待异步处理完成，查询 documents 表确认 summary 和 tags 已填充。

## 5. 文档上传服务编排

- [x] 5.1 实现 `DocumentUploadService.upload(request, file, userId)` 方法，编排流程：权限校验 → 文件存储 → documents 记录创建（status=PROCESSING）→ 发布上传事件 → 返回 DocumentVO。验证：集成测试验证上传成功返回 201 + PROCESSING 状态。

- [x] 5.2 实现上传事件监听器 `DocumentUploadListener`：监听 `DocumentUploadedEvent`，依次调用 Parser → Chunker → Embedding → 写入 chunks → 更新 documents 状态为 ACTIVE，失败时更新为 FAILED + error_message。验证：上传一篇 PDF，等待异步处理完成后查询 documents 表确认 status=ACTIVE、chunks 表有数据。

- [x] 5.3 在 `DocumentUploadService` 上添加 `@AuditLog(action = "UPLOAD", resource = "DOCUMENT")` 注解，确保上传操作记录审计日志。验证：上传成功后查询 audit_logs 表确认有对应记录。

## 6. REST 接口

- [x] 6.1 实现 `DocumentController`：创建 `POST /documents/upload` 接口，接收 `@RequestPart MultipartFile file` + `@RequestPart DocumentUploadRequest request`，调用 `DocumentUploadService`。添加 `@RequirePermission(resource = "DOCUMENT", action = "WRITE")`。验证：通过 Knife4j 测试上传接口，确认 201 响应格式正确。

- [x] 6.2 实现 `DocumentController` 的 `GET /documents` 分页查询接口，支持按 securityLevel、tag、status 筛选，通过 JOIN 权限表过滤无权限文档。验证：创建多篇文档后分页查询，确认只返回有权限的文档。

- [x] 6.3 实现 `DocumentController` 的 `GET /documents/{id}` 详情查询接口，权限不足时返回 403，不存在时返回 404。验证：分别测试存在/不存在/无权限三种场景，确认返回正确状态码。

- [x] 6.4 实现 `DocumentController` 的 `DELETE /documents/{id}` 逻辑删除接口，设置 deleted=1。验证：删除后列表查询不返回该文档，数据库中 deleted 字段为 1。

## 7. 前端对接

- [x] 7.1 修改 `frontend/src/views/Knowledge/Upload.vue`：将 el-upload 的 action 改为 `/api/documents/upload`，handleUpload 方法改为收集选中的文件 + 表单数据，通过 `FormData` + `axios` 发送真实请求，成功后跳转到列表页，失败时显示错误提示。验证：手动测试上传流程，确认文件成功上传到后端并显示在列表中。

- [x] 7.2 在 Upload.vue 中添加上传进度指示：使用 el-upload 的 `:on-progress` 事件显示进度条，上传成功后显示后端返回的文档 ID 和状态。验证：上传大文件时进度条正常显示，完成后跳转列表页。

## 8. 集成验证

- [x] 8.1 端到端测试：从登录 → 上传一篇 PDF → 等待处理完成 → 查看文档列表 → 查看文档详情 → 删除文档，确认全流程打通。验证：手动执行完整流程，确认无错误。

- [x] 8.2 验证权限控制：分别用 ADMIN/EDITOR/VIEWER 角色测试上传和列表查询，确认权限规则生效。验证：VIEWER 上传返回 403，EDITOR 上传成功，列表仅返回有权限文档。

## Workflow follow-up

- 完成上传功能后，同步更新 `openspec/specs/user-auth` 或创建新的 main spec 以反映知识库模块的完整行为。
