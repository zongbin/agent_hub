-- =====================================================================
-- 历史数据迁移：将 document_chunks 中已索引的分块灌入 vector_store
-- 说明：本项目未启用 Flyway，此脚本需手动执行（或通过一次性 runner）。
-- 幂等：依赖 vector_store 的 id 确定性（documentId-chunkIndex），重复执行会 upsert 覆盖；
--       但下面的 uuid_generate_v4() 每次生成不同 id，会重复插入，故建议执行前先清空 vector_store。
-- =====================================================================

-- 若需重复执行，先清空由本脚本迁移的数据（可选）
-- DELETE FROM vector_store;

INSERT INTO vector_store (id, content, metadata, embedding)
SELECT uuid_generate_v4(),
       c.content,
       jsonb_build_object(
           'documentId', c.document_id,
           'title',      d.title,
           'securityLevel', d.security_level,
           'chunkIndex', c.chunk_index
       ),
       c.embedding
FROM document_chunks c
JOIN documents d ON d.id = c.document_id
WHERE d.deleted = 0
  AND d.status = 1;
