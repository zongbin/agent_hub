-- =====================================================================
-- Spring AI PgVector VectorStore 默认表结构
-- 与 document_chunks 并列存在，Spring AI 原生管理此表，供 RAG 检索使用
-- 依赖：PostgreSQL 16 + pgvector + uuid-ossp 扩展
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content       TEXT NOT NULL,
    metadata      JSONB NOT NULL DEFAULT '{}'::jsonb,
    embedding     vector(1024) NOT NULL
);

-- HNSW 向量索引（余弦距离）
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding ON vector_store
    USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
