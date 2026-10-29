# Proposal

## Why

知识库模块（文档上传 → 解析 → 分块 → 向量化入库）已经落地，但用户目前只能浏览文档列表，无法就文档内容提问。智能问答是 RAG 管线的价值出口——让用户用自然语言直接获取答案并追溯出处。当前 `ChatService` 被注释未启用、`document_chunks` 表缺少向量检索方法、前端问答页仍是占位页，问答能力处于完全缺失状态。

## What Changes

- **新增权限感知向量检索**：基于 `document_chunks` 表 + pgvector 余弦距离，JOIN `documents` 表过滤密级与用户权限，返回 top-K 相关分块。
- **新增问答生成服务**：组装带引用标注（`[n]`）的 prompt，调用阿里云 DashScope qwen 模型生成答案。
- **新增 SSE 流式问答接口**：`POST /qa/ask`，流式返回答案增量、引用来源与完成/错误事件。
- **新增问答反馈**：`POST /qa/feedback`，记录答案是否有帮助。
- **LLM 供应商切换**：对话模型从 DeepSeek 迁移到阿里云 DashScope（qwen-plus），与 Embedding 统一到同一平台（text-embedding-v3），移除 DeepSeek 配置。
- **前端问答页对接**：改造 `QAM/index.vue`，接入 SSE 流式渲染、引用卡片与反馈按钮。
- **复用现有基础设施**：`@RequirePermission`、`R<T>`、`SecurityContextUtil`、pgvector、`document_chunks` 表。

## Capabilities

### New Capabilities

- `qa`：基于知识库的语义检索与智能问答，包括权限感知向量检索、带引用答案生成、SSE 流式返回、答案反馈。涵盖从用户提问到获得带出处答案的完整链路。

### Modified Capabilities

- 无。检索行为是新增的问答能力，不改变 `document-management` 已有的入库/列表/详情行为。

## Impact

- **代码变更范围**：
  - 新增 `backend/src/main/java/com/leon/agenthub/qa/` 下的 controller、service、dto、model、repository
  - 扩展 `DocumentChunkRepository`，新增向量相似度检索方法（原生 SQL + 权限 JOIN）
  - 恢复 `ChatService` 的 `@Service`，调整 `SpringAiConfig` 的 ChatModel 选择
  - 修改 `application.yml`：移除 `spring.ai.openai`（DeepSeek），对话走 `spring.ai.dashscope.chat`（qwen-plus）
  - 新增 Flyway V5 迁移：`qa_feedback` 表
  - 修改 `frontend/src/views/QAM/index.vue`，新增 `frontend/src/api/qa.ts`
- **API 新增**：`POST /api/qa/ask`（SSE 流）、`POST /api/qa/feedback`
- **数据库**：新增 `qa_feedback` 表；复用 `document_chunks`、`documents`
- **配置变更**：移除 DeepSeek 相关配置，`agenthub.llm` 指向 qwen-plus
