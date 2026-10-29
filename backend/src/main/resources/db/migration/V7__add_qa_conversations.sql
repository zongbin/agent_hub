-- =====================================================================
-- 问答会话与消息表：支撑多轮对话与历史会话管理
-- =====================================================================

-- 会话表
CREATE TABLE IF NOT EXISTS qa_conversations (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_qa_conv_user ON qa_conversations(user_id, updated_at DESC);

-- 消息表
CREATE TABLE IF NOT EXISTS qa_messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT      NOT NULL,
    role            VARCHAR(16) NOT NULL,          -- user / assistant
    content         TEXT        NOT NULL,
    sources         JSONB,                          -- 引用来源（assistant 消息）
    feedback        SMALLINT,                      -- 1 有帮助 / 0 无帮助 / NULL 未反馈
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_qa_msg_conv ON qa_messages(conversation_id, id);
