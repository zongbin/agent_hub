-- =====================================================================
-- 智知 AgentHub — 初始数据库结构（MVP）
-- 依赖：PostgreSQL 16 + pgvector 扩展（CREATE EXTENSION IF NOT EXISTS vector）
-- =====================================================================

-- 1. 用户表
CREATE TABLE IF NOT EXISTS users (
    id           BIGSERIAL PRIMARY KEY,
    username     VARCHAR(64)  NOT NULL UNIQUE,
    password     VARCHAR(128) NOT NULL,
    real_name    VARCHAR(64),
    email        VARCHAR(128),
    role         VARCHAR(16)  NOT NULL DEFAULT 'VIEWER',  -- ADMIN/EDITOR/VIEWER/GUEST
    department   VARCHAR(64),
    status       SMALLINT     NOT NULL DEFAULT 1,         -- 1 启用 0 禁用
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted      SMALLINT     NOT NULL DEFAULT 0
);

-- 2. 文档表
CREATE TABLE IF NOT EXISTS documents (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(512) NOT NULL,
    file_key        VARCHAR(256) NOT NULL,                -- MinIO 对象键
    file_type       VARCHAR(16)  NOT NULL,                -- docx/pdf/pptx/xlsx/txt/md
    file_size       BIGINT       NOT NULL DEFAULT 0,
    security_level  VARCHAR(16)  NOT NULL DEFAULT 'INTERNAL', -- PUBLIC/INTERNAL/CONFIDENTIAL
    owner_id        BIGINT       NOT NULL,
    department      VARCHAR(64),
    summary         TEXT,                                 -- AI 生成摘要
    tags            TEXT[],                               -- AI 生成标签
    status          SMALLINT     NOT NULL DEFAULT 1,      -- 1 正常 0 删除 2 处理中
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX idx_doc_owner ON documents(owner_id);
CREATE INDEX idx_doc_dept ON documents(department);
CREATE INDEX idx_doc_security ON documents(security_level);

-- 3. 文档分片表（含向量列）
CREATE TABLE IF NOT EXISTS document_chunks (
    id            BIGSERIAL PRIMARY KEY,
    document_id   BIGINT       NOT NULL,
    chunk_index   INT          NOT NULL,
    content       TEXT         NOT NULL,
    token_count   INT          NOT NULL DEFAULT 0,
    embedding     vector(1024),                           -- bge-large-zh = 1024 维
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE(document_id, chunk_index)
);
-- HNSW 向量索引（余弦距离，目标 <50ms）
CREATE INDEX idx_chunk_embedding ON document_chunks
    USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
CREATE INDEX idx_chunk_doc ON document_chunks(document_id);

-- 4. 文档细粒度授权表
CREATE TABLE IF NOT EXISTS doc_permissions (
    id           BIGSERIAL PRIMARY KEY,
    document_id  BIGINT      NOT NULL,
    grantee_id   BIGINT      NOT NULL,                    -- 用户 ID
    permission   VARCHAR(16) NOT NULL DEFAULT 'READ',     -- READ/WRITE
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(document_id, grantee_id)
);

-- 5. Agent 任务表
CREATE TABLE IF NOT EXISTS agent_tasks (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(256) NOT NULL,
    description TEXT,
    plan_json   JSONB,                                   -- Planner 生成的计划
    status      VARCHAR(16)  NOT NULL DEFAULT 'PENDING', -- PENDING/RUNNING/PAUSED/COMPLETED/FAILED
    result      TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_task_user ON agent_tasks(user_id);

-- 6. Agent 任务步骤表
CREATE TABLE IF NOT EXISTS agent_steps (
    id           BIGSERIAL PRIMARY KEY,
    task_id      BIGINT       NOT NULL,
    step_index   INT          NOT NULL,
    tool_name    VARCHAR(64)  NOT NULL,
    parameters   JSONB,
    status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING', -- PENDING/RUNNING/COMPLETED/SKIPPED/FAILED
    output       TEXT,
    error_msg    TEXT,
    requires_hitl SMALLINT    NOT NULL DEFAULT 0,         -- 是否需要人工确认
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_step_task ON agent_steps(task_id);

-- 7. 审计日志表
CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,
    username    VARCHAR(64),
    action      VARCHAR(64)  NOT NULL,                    -- 操作类型
    resource    VARCHAR(128),                             -- 目标资源
    detail      JSONB,
    ip_address  VARCHAR(64),
    success     SMALLINT     NOT NULL DEFAULT 1,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_time ON audit_logs(created_at);

-- 8. 会话/反馈表（问答历史）
CREATE TABLE IF NOT EXISTS qa_sessions (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    question     TEXT         NOT NULL,
    answer       TEXT,
    citations    JSONB,                                   -- 引用片段
    feedback     SMALLINT,                                -- 1 赞 0 踩 NULL 未反馈
    source       VARCHAR(16)  NOT NULL DEFAULT 'SIMPLE',  -- SIMPLE/AGENT
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_qa_user ON qa_sessions(user_id);
