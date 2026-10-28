package com.leon.agenthub.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 知识库模块配置 — 绑定 `agethub.chunk.*` 和 `agethub.storage.*` 前缀。
 */
@Data
@ConfigurationProperties(prefix = "agethub")
public class KnowledgeProperties {

    /** 分块配置 */
    private final Chunk chunk = new Chunk();

    /** 存储配置 */
    private final Storage storage = new Storage();

    @Data
    public static class Chunk {
        /** 每块 token 数 */
        private int size = 512;

        /** 重叠 token 数 */
        private int overlap = 50;
    }

    @Data
    public static class Storage {
        /** 本地存储根路径 */
        private String rootPath = "./data/docs";
    }
}
