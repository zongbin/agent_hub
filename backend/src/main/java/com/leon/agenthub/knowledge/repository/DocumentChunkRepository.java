package com.leon.agenthub.knowledge.repository;

import com.leon.agenthub.knowledge.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文档分块数据访问接口 — 批量写入 chunks，支持 pgvector embedding 列。
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    /**
     * 批量插入分块，使用 pgvector 的 vector 类型写入 embedding。
     *
     * <p>使用原生 SQL 因为 embedding 字段需要 CAST(... AS vector) 强制类型转换。
     * 循环单条插入，MVP 阶段可接受；生产环境可优化为 JDBC batch。
     *
     * @param chunks 分块列表（embedding 字段为字符串格式 "[0.1,0.2,...]"）     * @return 插入行数
     */
    @Modifying
    @Transactional
    default int insertBatch(List<DocumentChunk> chunks) {
        int count = 0;
        for (DocumentChunk chunk : chunks) {
            count += insertSingle(
                    chunk.getDocumentId(),
                    chunk.getChunkIndex(),
                    chunk.getContent(),
                    chunk.getTokenCount(),
                    chunk.getEmbedding()
            );
        }
        return count;
    }

    /**
     * 单条插入分块（原生 SQL，含 pgvector CAST(... AS vector) 转换）。
     */
    @Modifying
    @Query(value = "INSERT INTO document_chunks " +
           "(document_id, chunk_index, content, token_count, embedding, created_at) VALUES " +
           "(:documentId, :chunkIndex, :content, :tokenCount, CAST(:embedding AS vector), NOW())",
           nativeQuery = true)
    int insertSingle(@Param("documentId") Long documentId,
                     @Param("chunkIndex") Integer chunkIndex,
                     @Param("content") String content,
                     @Param("tokenCount") Integer tokenCount,
                     @Param("embedding") String embedding);
}
