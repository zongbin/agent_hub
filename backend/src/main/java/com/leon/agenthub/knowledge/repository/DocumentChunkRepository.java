package com.leon.agenthub.knowledge.repository;

import com.leon.agenthub.knowledge.model.DocumentChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 文档分块数据访问 — 读取 vector_store 中归属于某文档的分块内容。
 *
 * <p>分块由 Spring AI 的 PgVectorStore 写入（metadata 中带 documentId / chunkIndex），
 * 这里只做只读查询，不参与向量化写入。
 */
@Repository
@RequiredArgsConstructor
public class DocumentChunkRepository {

    /** 单文档返回的分块上限，避免超大文档一次性返回过多内容 */
    private static final int MAX_CHUNKS = 500;

    private static final String SQL = """
            SELECT id,
                   content,
                   metadata->>'chunkIndex' AS chunk_index
            FROM vector_store
            WHERE metadata->>'documentId' = ?
            ORDER BY CASE WHEN metadata->>'chunkIndex' ~ '^[0-9]+$'
                          THEN (metadata->>'chunkIndex')::int
                          ELSE 999999 END, id
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * 查询某文档的全部分块（按分块序号升序）。
     *
     * @param documentId 文档 ID
     * @return 分块列表（最多 {@link #MAX_CHUNKS} 条）
     */
    public List<DocumentChunk> findByDocumentId(Long documentId) {
        List<DocumentChunk> chunks = jdbcTemplate.query(SQL,
                (rs, rowNum) -> {
                    DocumentChunk chunk = new DocumentChunk();
                    chunk.setId(rs.getObject("id", UUID.class));
                    chunk.setContent(rs.getString("content"));
                    chunk.setChunkIndex(parseChunkIndex(rs.getString("chunk_index")));
                    return chunk;
                },
                String.valueOf(documentId), MAX_CHUNKS);

        // metadata 中缺少 chunkIndex 时，按返回顺序兜底编号
        for (int i = 0; i < chunks.size(); i++) {
            if (chunks.get(i).getChunkIndex() == null) {
                chunks.get(i).setChunkIndex(i);
            }
        }
        return chunks;
    }

    /**
     * 解析分块序号（metadata 中的 chunkIndex 为数字文本）。
     */
    private Integer parseChunkIndex(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
