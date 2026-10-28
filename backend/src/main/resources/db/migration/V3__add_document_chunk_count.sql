-- =====================================================================
-- 补充文档表字段：分块数量、错误信息
-- 支持文档上传处理链路的状态追踪
-- =====================================================================

-- 补充 chunk_count 字段：记录文档分块数量
ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS chunk_count INT NOT NULL DEFAULT 0;

-- 补充 error_message 字段：记录处理失败原因
ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS error_message TEXT;
