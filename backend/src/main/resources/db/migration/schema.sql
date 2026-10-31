-- =====================================================================
-- 智知 AgentHub — 全量数据库结构脚本（MVP）
--
-- 依赖：PostgreSQL 16 + pgvector 扩展（vector / uuid-ossp）
-- 用法：本脚本为「全量重建」脚本，可直接执行；
--       脚本开头会先 DROP 全部业务表，再重新创建（幂等，可重复执行）。
--       若需保留数据，请勿执行开头的 DROP 段落。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. 清理旧表（重建用）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS qa_messages CASCADE;
DROP TABLE IF EXISTS qa_conversations CASCADE;
DROP TABLE IF EXISTS qa_feedback CASCADE;
DROP TABLE IF EXISTS qa_sessions CASCADE;
DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS agent_steps CASCADE;
DROP TABLE IF EXISTS agent_tasks CASCADE;
DROP TABLE IF EXISTS agent_messages CASCADE;
DROP TABLE IF EXISTS agent_conversations CASCADE;
DROP TABLE IF EXISTS doc_permissions CASCADE;
DROP TABLE IF EXISTS document_chunks CASCADE;
DROP TABLE IF EXISTS documents CASCADE;
DROP TABLE IF EXISTS vector_store CASCADE;
DROP TABLE IF EXISTS users CASCADE;

DROP SEQUENCE IF EXISTS qa_conversations_id_seq;
DROP SEQUENCE IF EXISTS documents_id_seq;
DROP SEQUENCE IF EXISTS document_chunks_id_seq;
DROP SEQUENCE IF EXISTS doc_permissions_id_seq;
DROP SEQUENCE IF EXISTS agent_tasks_id_seq;
DROP SEQUENCE IF EXISTS agent_steps_id_seq;
DROP SEQUENCE IF EXISTS agent_messages_id_seq;
DROP SEQUENCE IF EXISTS audit_logs_id_seq;
DROP SEQUENCE IF EXISTS qa_sessions_id_seq;
DROP SEQUENCE IF EXISTS qa_feedback_id_seq;
DROP SEQUENCE IF EXISTS users_id_seq;

-- ---------------------------------------------------------------------
-- 1. 扩展
-- ---------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---------------------------------------------------------------------
-- 2. 用户表
-- ---------------------------------------------------------------------
CREATE TABLE users (
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

-- ---------------------------------------------------------------------
-- 3. 文档表
-- ---------------------------------------------------------------------
CREATE TABLE documents (
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
    chunk_count     INT          NOT NULL DEFAULT 0,      -- 分块数量
    error_message   TEXT,                                 -- 处理失败原因
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX idx_doc_owner ON documents(owner_id);
CREATE INDEX idx_doc_dept ON documents(department);
CREATE INDEX idx_doc_security ON documents(security_level);

-- ---------------------------------------------------------------------
-- 4. 文档分片表（含向量列）
-- ---------------------------------------------------------------------
CREATE TABLE document_chunks (
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

-- ---------------------------------------------------------------------
-- 5. 文档细粒度授权表
-- ---------------------------------------------------------------------
CREATE TABLE doc_permissions (
    id           BIGSERIAL PRIMARY KEY,
    document_id  BIGINT      NOT NULL,
    grantee_id   BIGINT      NOT NULL,                    -- 用户 ID
    permission   VARCHAR(16) NOT NULL DEFAULT 'READ',     -- READ/WRITE
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(document_id, grantee_id)
);

-- ---------------------------------------------------------------------
-- 6. Agent 任务表
-- ---------------------------------------------------------------------
CREATE TABLE agent_tasks (
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

-- ---------------------------------------------------------------------
-- 7. Agent 任务步骤表
-- ---------------------------------------------------------------------
CREATE TABLE agent_steps (
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

-- ---------------------------------------------------------------------
-- 7.1 Agent 会话表（超级智能体多轮对话容器，与 qa_conversations 物理隔离）
-- ---------------------------------------------------------------------
CREATE TABLE agent_conversations (
    id          VARCHAR(64) PRIMARY KEY,                 -- 会话 ID（UUID 字符串）
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,                   -- 取首问截断
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_agent_conv_user ON agent_conversations(user_id, updated_at DESC);

-- ---------------------------------------------------------------------
-- 7.2 Agent 消息表（步骤轨迹以 JSONB 存储，供前端回看时还原时间线）
-- ---------------------------------------------------------------------
CREATE TABLE agent_messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,                 -- 关联 agent_conversations.id
    role            VARCHAR(16) NOT NULL,                 -- USER / ASSISTANT
    content         TEXT        NOT NULL,                 -- 用户提问 / 智能体最终回答
    steps_json      JSONB,                                -- 步骤轨迹（助手消息）
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_agent_msg_conv ON agent_messages(conversation_id, id);

-- ---------------------------------------------------------------------
-- 8. 审计日志表
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
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

-- ---------------------------------------------------------------------
-- 9. 问答会话表（会话 ID 为 UUID 字符串）
-- ---------------------------------------------------------------------
CREATE TABLE qa_conversations (
    id          VARCHAR(64)  PRIMARY KEY,                 -- 会话 ID（UUID 字符串）
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_qa_conv_user ON qa_conversations(user_id, updated_at DESC);

-- ---------------------------------------------------------------------
-- 10. 问答消息表（conversation_id 为 VARCHAR(64)）
-- ---------------------------------------------------------------------
CREATE TABLE qa_messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,                  -- 关联 qa_conversations.id
    role            VARCHAR(16) NOT NULL,                 -- user / assistant
    content         TEXT        NOT NULL,
    sources         JSONB,                                -- 引用来源（assistant 消息）
    feedback        SMALLINT,                             -- 1 有帮助 / 0 无帮助 / NULL 未反馈
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_qa_msg_conv ON qa_messages(conversation_id, id);

-- ---------------------------------------------------------------------
-- 11. Spring AI PgVector VectorStore 表
--     与 document_chunks 并列存在，由 Spring AI 原生管理，供 RAG 检索使用
-- ---------------------------------------------------------------------
CREATE TABLE vector_store (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content       TEXT NOT NULL,
    metadata      JSONB NOT NULL DEFAULT '{}'::jsonb,
    embedding     vector(1024) NOT NULL
);
CREATE INDEX idx_vector_store_embedding ON vector_store
    USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);

-- ---------------------------------------------------------------------
-- 12. 遗留表（早期问答记录，当前问答链路已由 qa_conversations/qa_messages 承担）
-- ---------------------------------------------------------------------
CREATE TABLE qa_sessions (
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

CREATE TABLE qa_feedback (
    id          BIGSERIAL PRIMARY KEY,
    qa_id       VARCHAR(64)  NOT NULL,        -- 问答会话标识
    user_id     BIGINT       NOT NULL,
    useful      BOOLEAN      NOT NULL,        -- true 有帮助 / false 无帮助
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_qa_feedback_qa ON qa_feedback(qa_id);
CREATE INDEX idx_qa_feedback_user ON qa_feedback(user_id);

-- ---------------------------------------------------------------------
-- 13. 初始管理员账号
--     密码：admin123（BCrypt 哈希，cost factor 10）
-- ---------------------------------------------------------------------
INSERT INTO users (username, password, real_name, email, role, department, status, created_at, updated_at, deleted)
VALUES (
    'admin',
    '$2a$10$rhzKBRv1rJMA4n4KsTqey.BI.zaM9.Ir9Kqp2wMtHuT35VfmpcgjS',
    '系统管理员',
    'admin@company.com',
    'ADMIN',
    '管理部',
    1,
    NOW(),
    NOW(),
    0
)
ON CONFLICT (username) DO NOTHING;

-- ---------------------------------------------------------------------
-- 14. 历史数据迁移（可选）
--     将 document_chunks 中已索引的分块灌入 vector_store。
--     仅在「不清空 document_chunks 的历史数据」时才需要执行；
--     本脚本已 DROP 全部表，故默认注释。执行前建议先清空 vector_store，避免重复插入。
-- ---------------------------------------------------------------------
-- DELETE FROM vector_store;
-- INSERT INTO vector_store (id, content, metadata, embedding)
-- SELECT uuid_generate_v4(),
--        c.content,
--        jsonb_build_object(
--            'documentId', c.document_id,
--            'title',      d.title,
--            'securityLevel', d.security_level,
--            'chunkIndex', c.chunk_index
--        ),
--        c.embedding
-- FROM document_chunks c
-- JOIN documents d ON d.id = c.document_id
-- WHERE d.deleted = 0
--   AND d.status = 1;
