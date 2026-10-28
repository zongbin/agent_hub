package com.leon.agenthub.common.embedding;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import com.leon.agenthub.knowledge.config.EmbeddingProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DashScope Embedding 服务 — 封装 Spring AI 的 {@link EmbeddingModel}。
 *
 * <p>
 * 替代原有的 {@code EmbeddingClient}（手写 RestTemplate），
 * 使用 Spring AI 统一的 Embedding 抽象，支持批量调用、自动分批。
 *
 * <p>
 * 通过 SpringAiConfig 中的 @Primary 注入 DashScope EmbeddingModel。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashScopeEmbeddingService {

    private final EmbeddingModel dashscopeEmbeddingModel;

    private final EmbeddingProperties embeddingProperties;

    /**
     * 对文本列表进行向量化。
     *
     * @param texts 待向量化的文本列表
     * @return 对应的向量列表，每个向量为 float 数组
     */
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        List<float[]> allEmbeddings = new ArrayList<>();
        int batchSize = embeddingProperties.getBatchSize();
        // 分批调用，避免单次请求过大
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);
            List<float[]> batchEmbeddings = embedBatch(batch);
            allEmbeddings.addAll(batchEmbeddings);
        }

        return allEmbeddings;
    }

    /**
     * 单批文本向量化。
     */
    private List<float[]> embedBatch(List<String> texts) {
        try {
            EmbeddingResponse response = dashscopeEmbeddingModel.embedForResponse(texts);
            List<float[]> embeddings = new ArrayList<>();
            response.getResults().forEach(result -> embeddings.add(result.getOutput()));
            log.debug("Embedded {} texts to vectors", texts.size());
            return embeddings;
        } catch (Exception e) {
            log.error("Failed to embed texts batch: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to embed texts batch", e);
        }
    }
}
