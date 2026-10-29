# Tasks

## 1. LLM 供应商切换（DeepSeek → 阿里云 DashScope）

- [x] 1.1 修改 `backend/src/main/resources/application.yml`：删除 `spring.ai.openai`（DeepSeek）整块，保留 `spring.ai.dashscope`，确认 `chat.options.model=qwen-plus`、`embedding.options.model=text-embedding-v3`、`dimensions=1024`；将 `agenthub.llm` 的 `chat-model` 改为 `qwen-plus`。验证：应用启动成功，日志无 DeepSeek/openai 相关 Bean 缺失或二义报错。

- [x] 1.2 恢复 `common/llm/ChatService.java` 的 `@Service` 注解，确认其通过 `ChatClient.Builder` 注入唯一 ChatModel（DashScope qwen）。验证：`mvn compile` 通过，注入测试中 `ChatService.chat("你好")` 返回非空文本。

- [x] 1.3 检查 `common/config/SpringAiConfig.java`：若删除 DeepSeek 后已无多 ChatModel 二义，移除或简化被注释的 `@Primary`/`@Qualifier` 代码，保持配置清晰。验证：启动无 `NoUniqueBeanDefinitionException`。

## 2. 权限感知向量检索

- [x] 2.1 抽取统一权限判定：将 `DocumentController.hasDocumentAccess` 的规则沉淀为单一服务（如 `permission/service/DocumentAccessChecker`），供检索与文档接口共用。验证：单测覆盖 ADMIN/GUEST/VIEWER/EDITOR × PUBLIC/INTERNAL/CONFIDENTIAL 组合，结果与现有行为一致。

- [x] 2.2 在 `knowledge/repository/DocumentChunkRepository` 新增原生 SQL 检索方法：按 `embedding <=> CAST(:queryVector AS vector)` 余弦距离排序，JOIN `documents` 过滤 `deleted=0`、`status=1` 与权限谓词，返回 top-K 分块（含 document_id、title、content、chunk_index、similarity）。验证：集成测试插入有权限/无权限两类分块，确认只返回有权限的前 K 条。

- [x] 2.3 新增 `qa/service/KnowledgeRetriever`：调用 `DashScopeEmbeddingService.embed` 对问题向量化，调用 2.2 的检索方法，组装 `QaSource` 列表（documentId、title、chunkIndex、snippet）。验证：单测 mock 检索结果，确认 `QaSource` 字段映射正确、片段截断生效。

## 3. 问答生成与 SSE 流式接口

- [x] 3.1 新增 `qa/service/QaService`：组装带引用标注（`[n]`）的 prompt，调用 `ChatService.chatStream` 生成答案；检索为空时返回「知识库中未找到相关内容」兜底文案。验证：单测 mock `KnowledgeRetriever` 与 `ChatService`，确认 prompt 含引用片段、空结果走兜底。

- [x] 3.2 新增 `qa/controller/QaController`：`POST /qa/ask`（入参 `QaRequest{question}`），加 `@RequirePermission(resource="QA", action="READ")`，返回 `SseEmitter`，按序发送 `source`、`delta`、`done`/`error` 事件。验证：用 curl 调接口观察 SSE 事件顺序正确，超时/异常时发送 `error` 事件。

- [x] 3.3 新增 `qa/dto/`：`QaRequest`、`QaSource`、`QaFeedbackRequest`。验证：DTO 编译通过，字段与 spec 要求一致。

## 4. 答案反馈

- [x] 4.1 新增 Flyway V5 迁移 `backend/src/main/resources/db/migration/V5__add_qa_feedback.sql`：创建 `qa_feedback` 表（id、qa_id、user_id、useful、created_at）。验证：`mvn flyway:migrate` 成功，查询表结构确认字段存在。

- [x] 4.2 新增 `qa/model/QaFeedback` 实体与 `qa/repository/QaFeedbackRepository`。验证：编译通过，JPA 自动映射表字段。

- [x] 4.3 在 `QaController` 新增 `POST /qa/feedback`：校验 qa_id 与 useful 非空，调用 `QaFeedbackRepository.save` 持久化，返回 `R<Void>`。验证：curl 提交反馈，查询 `qa_feedback` 表确认记录落库，缺字段时返回 400。

## 5. 前端对接

- [x] 5.1 新增 `frontend/src/api/qa.ts`：封装 `askQuestion(question, onSource, onDelta, onDone, onError)`，用 `fetch` + `ReadableStream` 解析 SSE（POST 无法用 EventSource）。验证：浏览器 Network 面板能看到流式响应分片。

- [x] 5.2 改造 `frontend/src/views/QAM/index.vue`：替换占位 `sendMessage`，接入 5.1 的 SSE 回调，逐字渲染答案；答案下方展示引用来源卡片（点击跳转 `knowledge/:id`）；每个回答配有帮助/无帮助按钮，调用 `/qa/feedback`。验证：手动提问，确认答案流式展示、引用可点击、反馈可提交。

## 6. 集成验证

- [x] 6.1 端到端测试：上传一篇文档并等待索引完成 → 提问相关问题 → 确认流式返回带引用答案 → 提交反馈。验证：全流程无报错，答案引用与来源列表对应。（实测：上传文档 22 → 状态 ACTIVE/chunk=1 → 提问「病假工资怎么算」返回 27 个 delta + done，来源含文档 22；反馈接口返回 code=0。）

- [x] 6.2 权限验证：分别用 ADMIN 与 VIEWER 角色对含 CONFIDENTIAL 文档的知识库提问，确认 VIEWER 的答案不引用 CONFIDENTIAL 内容。验证：对比两个角色的引用来源，无越权。（实测：ADMIN 来源含 CONFIDENTIAL 文档 23，VIEWER 不含；为 VIEWER 授予该文档 READ 权限后来源中即出现 23。）

## 7. 遗留问题修复（实现中发现，用户确认）

- [x] 7.1 将文档级授权纳入统一权限判定：`DocumentAccessChecker` 增加 `canAccess(userId, role, documentId, level)` 重载，`doc_permissions` 中被授权的用户可访问 CONFIDENTIAL 文档；检索 SQL、文档列表、详情、删除均使用同一规则（列表查询与详情/删除原先对 EDITOR × CONFIDENTIAL 判定不一致，本次统一）。验证：`DocumentAccessCheckerTest` 覆盖角色矩阵与授权/未授权/GUEST×INTERNAL 场景，12 项单测全部通过。

- [x] 7.2 放行 SSE 异步派发：`SecurityConfig` 增加 `DispatcherType.ASYNC/ERROR` 的 `permitAll`，避免 `SseEmitter` 完成/超时触发异步派发时因 JWT 上下文缺失被判为匿名拒绝。验证：后端编译通过；运行时行为需 6.1 端到端验证。

- [x] 7.3 修正配置前缀拼写：`EmbeddingProperties`（`agethub.embedding` → `agenthub.embedding`）与 `KnowledgeProperties`（`agethub` → `agenthub`），使 `application.yml` 中的 embedding/chunk/storage 配置真正完成绑定。验证：后端编译通过；绑定结果需启动应用确认。

- [x] 7.4 清理 `frontend/src/views/Knowledge/List.vue` 中未使用的导入与函数，解除既有 `vue-tsc` 报错。验证：`npx vue-tsc --noEmit` 通过。

- [x] 7.5 修复 prompt 内容截断：`snippet-length` 原先同时作用于传给 LLM 的分块内容，导致文档后半段被切掉、模型误判「无法回答」。改为 prompt 传完整分块，仅前端 `snippet` 截断。验证：端到端提问「病假工资怎么算」修复前拒答、修复后正确返回「3天」「80%」等细则。

## 8. 检索改用 Spring AI vector_store

- [x] 8.1 扩展 `VectorStoreService`：`addChunks`（确定性 UUID = documentId-chunkIndex，metadata 存 documentId/securityLevel/title/chunkIndex）、`search`（角色×密级 + 预查 doc_permissions 授权列表拼进 `documentId IN (...)` 过滤）、`deleteByDocumentId`。验证：编译通过，运行时验证见 8.5。

- [x] 8.2 写入侧改走 vector_store：`DocumentIndexListener` 改调 `vectorStoreService.addChunks`，移除手动 embed 与 `document_chunks` 写入。验证：上传文档 25/26 后状态 ACTIVE，`vector_store` 有对应分块。

- [x] 8.3 检索侧改走 vector_store：`KnowledgeRetriever` 改调 `vectorStoreService.search`；`QaService` 改从 Spring AI `Document` 提取来源；删除 `DocumentChunk` 实体、`DocumentChunkRepository`、`ChunkHit` 投影。验证：提问命中文档 25，24 个 delta + done。

- [x] 8.4 文档删除同步清理 vector_store：`DocumentController.delete` 追加 `vectorStoreService.deleteByDocumentId`。验证：删除文档 26 后提问不再命中 26。

- [x] 8.5 端到端验证 vector_store 链路：ADMIN 命中 25+26（含 CONFIDENTIAL）；VIEWER 只命中 25；为 VIEWER 授予文档 26 后命中 26（细粒度授权生效）。修复 `vector_store.embedding` 列维度 1536→1024（项目未启用 Flyway，V4 未执行，表由 Spring AI 以默认 1536 建）。

## 9. 历史对话（完整会话管理）

- [x] 9.1 会话与消息数据模型：新增 `Conversation`（qa_conversations）与 `Message`（qa_messages）实体、`ConversationRepository`、`MessageRepository`，及 Flyway V7 迁移脚本。验证：编译通过；建表由 Hibernate ddl-auto 完成（项目未启用 Flyway）。

- [x] 9.2 多轮上下文：`QaService.buildPrompt` 携带最近 N 轮问答（`agenthub.qa.history-size` 默认 6、消息截断 200 字），使模型能理解指代。验证：单测通过；运行时行为需 9.5 验证。

- [x] 9.3 会话管理接口：`GET /qa/conversations`（会话列表）、`GET /qa/conversations/{id}/messages`（消息列表）、`DELETE /qa/conversations/{id}`（删除会话及消息），均校验会话归属（他人会话返回 404）。验证：编译通过。

- [x] 9.4 问答接口适配会话：`QaRequest` 增加 `conversationId`（空则新建会话并以首问为标题）；SSE `source` 事件下发 `conversationId`、`done` 事件下发 `messageId`；反馈改为按 `messageId` 更新 `Message.feedback`（废弃独立的 qa_feedback 表）。验证：编译通过。

- [x] 9.5 前端会话管理 UI：`QAM/index.vue` 左侧会话列表（新建/切换/删除），右侧聊天区；`api/qa.ts` 补充会话相关方法。验证：`npx vue-tsc --noEmit` 通过；运行时行为需端到端验证。

## 10. 文档索引异步治理

- [x] 10.1 修复事务竞态：`DocumentUploadService` 改为在事务提交后再发布上传事件（`TransactionSynchronization.afterCommit`），消除异步索引线程读不到未提交 `documents` 行的竞态（PostgreSQL READ COMMITTED 下未提交行不可见）。验证：编译通过；运行时验证见 10.4。

- [x] 10.2 自定义文档索引线程池：新增 `AsyncConfig`，提供 `documentIndexExecutor`（core=4 / max=8 / queue=200 / 线程名 `doc-index-` / `AbortPolicy` / 停机等待在途任务 120 秒），`DocumentIndexListener` 通过 `@Async("documentIndexExecutor")` 绑定。验证：`AsyncConfigTest` 校验池参数、拒绝策略、线程名前缀与停机等待行为。

- [x] 10.3 任务拒绝处理：队列满导致任务被拒时，捕获 `TaskRejectedException`，在事务完成后由 `DocumentIndexFailureHandler`（`REQUIRES_NEW`）把文档标记为 FAILED 并写入原因，避免永久卡在「处理中」。验证：`DocumentIndexFailureHandlerTest` 覆盖标记失败与仓储异常不外抛。

## Workflow follow-up

- 实现完成后运行 `openspec validate --change knowledge-qa` 确认产物合规。
- 评估是否需要将 `qa` 能力归档进 `openspec/specs/qa/`（`openspec archive`）。
