package com.leon.agenthub.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 灵积平台 Embedding 配置 — 绑定 `agenthub.embedding.*` 前缀。
 */
@Data
@ConfigurationProperties(prefix = "agenthub.embedding")
public class EmbeddingProperties {

    /** 灵积平台 API 基础地址 */
    private String baseUrl = "https://dashscope.aliyuncs.com";

    /** API 密钥（通过环境变量 DASHSCOPE_API_KEY 注入） */
    private String apiKey = "";

    /** Embedding 模型名称 */
    private String model = "text-embedding-v3";

    /** 向量维度 */
    private int dimensions = 1024;

    /** 单次批量调用最大条数 */
    private int batchSize = 10;

    /** 连接超时（毫秒） */
    private int connectTimeoutMs = 5000;

    /** 读取超时（毫秒） */
    private int readTimeoutMs = 30000;
}
