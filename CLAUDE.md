# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**智知 AgentHub** — enterprise knowledge management platform for SMEs (50–500 users). Combines a document knowledge base with RAG-based Q&A and a Plan-and-Execute "SuperAgent" that autonomously decomposes complex tasks into tool-calling steps. Target: natural-language answers over scattered corporate documents, plus AI-generated reports/analyses in minutes.

Full product context: `doc/产品文档/企业知识管理Agent_PRD_MVP.md`
Full technical design: `doc/技术设计/智知AgentHub_技术设计文档_MVP.md`
Interactive prototype: `doc/产品文档/企业知识管理Agent_原型_MVP.html`

## Code Conventions

- **Java file naming**: 所有 Java 文件名必须使用**驼峰命名法（CamelCase）**，例如 `BusinessException.java`、`GlobalExceptionHandler.java`、`AgentHubApplication.java`。禁止使用下划线、连字符或全小写拼写（如 `business_exception.java` 是错误的）。此规则适用于本项目及所有后续 Java 项目。

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Vue 3 + TypeScript + Element Plus + TailwindCSS 3 |
| Backend API | Spring Boot 3.x + Java 17, MyBatis-Plus 3 |
| ORM / DB | PostgreSQL 16 + pgvector extension (vector search inside the same DB as business data — this is a deliberate choice, not an oversight) |
| Cache / State / SSE | Redis 7 |
| File storage | MinIO (S3-compatible, self-hosted) |
| Agent orchestration | LangGraph (Python microservice, called over HTTP from Java backend) |
| LLM | DeepSeek-V3 (chat), DeepSeek-Embedding / BAAI/bge-large-zh (embeddings) |
| Reverse proxy | Nginx 1.24 |
| Deployment | Docker Compose |

## Key Architectural Decisions

1. **pgvector over dedicated vector DB** — vectors live in PostgreSQL alongside document metadata. Permission filtering happens naturally via SQL JOIN to the `documents` table. Zero extra ops cost. HNSW index (target < 50ms query, < 50M vectors). Migrate to Milvus/Qdrant only if vector count exceeds ~1M.

2. **LangGraph as a separate Python service** — the SuperAgent (Planner + Executor + Tool Layer) runs in Python. The Java backend communicates with it over HTTP. This is intentional: LangGraph provides Plan-and-Execute, checkpointing, and streaming natively.

3. **Plan-and-Execute (not ReAct)** — the agent produces a full structured JSON plan *before* executing. Users see and can edit the plan. Steps are auditable and resumable. This is a requirement for enterprise use (predictability, traceability, intervention).

4. **HITL (Human-in-the-Loop)** — read tools (knowledge_search, document_read) auto-execute. Write tools (api_call) pause for user confirmation before running.

5. **Unified permission layer** — `@RequirePermission` annotation on service methods. JWT → role + department → resource security level check. 4 roles (ADMIN, EDITOR, VIEWER, GUEST) × 3 security levels (PUBLIC, INTERNAL, CONFIDENTIAL) + per-document grants in `doc_permissions`.

6. **Model routing for cost control** — strong model (DeepSeek-V3) for planning and content generation; cheap options (Qwen-mini, rules) for intent classification and data analysis. Target: ~¥0.05–0.10 per 5-step agent task.

## Core Data Flows

**Document upload**: Multipart → MinIO → Tika/docx/pptx/xlsx parse → chunk (512 tokens, 50 overlap) → Embedding → pgvector (`document_chunks` table) + metadata in `documents` → AI summary + tags via LLM.

**Q&A (simple question)**: Question → intent classifier → Embedding → pgvector top-K=5 (with permission JOIN) → ReRanker → build prompt with citations → DeepSeek stream (SSE) → answer + citations + feedback buttons.

**SuperAgent (complex task)**: Task → Task Router detects complexity → Planner LLM generates JSON plan → user confirms/modifies → Executor runs steps via LangGraph → each step calls a tool (knowledge_search, document_read, content_generation, data_analysis, api_call) → permission check per step → SSE progress → finalize → result + citations.

## Repository Structure (planned, from tech design)

```
agent_hub/
├── backend/                    # Spring Boot 3 + Java 17
│   └── src/main/java/com/company/agenthub/
│       ├── auth/               # JWT login, SSO integration point
│       ├── qa/                 # RAG pipeline: query processing, retrieval, generation
│       ├── knowledge/          # Document upload, parsing, chunking, indexing
│       ├── agent/              # SuperAgent HTTP client, task/step tracking
│       ├── permission/         # @RequirePermission, RBAC, security levels
│       ├── audit/              # AuditAspect, audit log recording
│       ├── connector/          # Feishu / WeCom document sync
│       └── common/             # LLM client, embedding service, shared config
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

## Database Schema Notes

- `document_chunks` uses `vector(1024)` — dimension must match the embedding model (bge-large-zh = 1024).
- Vector search uses cosine distance operator `<=>` with HNSW index (`m=16, ef_construction=64`).
- Permission-aware retrieval always JOINs `document_chunks` → `documents` and filters on `security_level` + `doc_permissions`. Never query chunks without this join.
- `agent_tasks` / `agent_steps` persist plan and execution state for resume-after-pause and audit.

## API Conventions

- All responses wrapped: `{"code": 0, "message": "success", "data": {...}}`
- Auth via `Authorization: Bearer {jwt}` header.
- SSE endpoints stream events: `agent.plan`, `agent.step.start`, `agent.step.progress`, `agent.step.complete`, `agent.step.skip`, `agent.complete`, `agent.error`.
- REST paths prefixed with `/api/` (see §7 of tech design for full list).

## MVP Scope Boundaries (do not implement in MVP)

- Knowledge graph, field-level redaction, IP whitelist, watermarking
- Multi-model provider abstraction (DeepSeek only for now)
- Data-source permission inheritance
- Agent marketplace / custom orchestration
- Mobile app

## Milestones

- **M1 (weeks 1–4)**: Document upload + AI parsing + basic Q&A (≥100 docs, ≥70% answer accuracy)
- **M2 (weeks 5–8)**: Permission system + SuperAgent v1 (competitor analysis scenario, ≥80% completion)
- **M3 (weeks 9–12)**: Connectors + audit logs + production launch
