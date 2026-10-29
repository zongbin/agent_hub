-- =====================================================================
-- 问答反馈表：记录用户对问答答案的有用性评价
-- =====================================================================

CREATE TABLE IF NOT EXISTS qa_feedback (
    id          BIGSERIAL PRIMARY KEY,
    qa_id       VARCHAR(64)  NOT NULL,        -- 问答会话标识（与 SSE source 事件下发的 qaId 对应）
    user_id     BIGINT       NOT NULL,
    useful      BOOLEAN      NOT NULL,        -- true 有帮助 / false 无帮助
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_qa_feedback_qa ON qa_feedback(qa_id);
CREATE INDEX IF NOT EXISTS idx_qa_feedback_user ON qa_feedback(user_id);
