package com.leon.agenthub.knowledge.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PgVector 向量存储配置 — 将 Spring AI 的 VectorStore 指向 PostgreSQL + pgvector。
 *
 * <p>使用项目中已定义的 document_chunks 表存储向量，维度 1024（匹配 bge-large-zh 嵌入模型）。
 */
@Configuration
public class PgVectorStoreConfig {

    /**
     * 创建 PgVectorStore Bean，用于向量相似度搜索。
     *
     * @param jdbcTemplate            JDBC 模板（自动注入）
     * @param dashscopeEmbeddingModel DashScope 嵌入模型
     * @return VectorStore 实例
     */
    @Bean
    public VectorStore pgVectorStore(JdbcTemplate jdbcTemplate,
                                     EmbeddingModel dashscopeEmbeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1024) // 匹配 bge-large-zh / text-embedding-v3 输出维度
                .distanceType(PgDistanceType.COSINE_DISTANCE)
                .indexType(PgIndexType.HNSW)
                .initializeSchema(true)
                .schemaName("public")
                .vectorTableName("vector_store") // Spring AI PgVectorStore 自有表
                .maxDocumentBatchSize(10000)
                .build();
    }
}
