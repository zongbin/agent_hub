package com.leon.agenthub.knowledge.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PgVector 向量存储配置 — 将 Spring AI 的 VectorStore 指向 PostgreSQL + pgvector。
 *
 * <p>向量存储在 Spring AI 自有的 {@code vector_store} 表中（由 {@code initializeSchema(true)} 维护），
 * 维度与 {@code spring.ai.dashscope.embedding.options.dimensions} 保持一致，避免两处配置漂移。
 */
@Configuration
public class PgVectorStoreConfig {

    /**
     * 创建 PgVectorStore Bean，用于向量相似度搜索。
     *
     * @param jdbcTemplate            JDBC 模板（自动注入）
     * @param dashscopeEmbeddingModel DashScope 嵌入模型
     * @param dimensions              向量维度（与嵌入模型输出一致）
     * @return VectorStore 实例
     */
    @Bean
    public VectorStore pgVectorStore(JdbcTemplate jdbcTemplate,
                                     EmbeddingModel dashscopeEmbeddingModel,
                                     @Value("${spring.ai.dashscope.embedding.options.dimensions:1024}") int dimensions) {
        return PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(dimensions)
                .distanceType(PgDistanceType.COSINE_DISTANCE)
                .indexType(PgIndexType.HNSW)
                .initializeSchema(true)
                .schemaName("public")
                .vectorTableName("vector_store") // Spring AI PgVectorStore 自有表
                .maxDocumentBatchSize(10000)
                .build();
    }
}
