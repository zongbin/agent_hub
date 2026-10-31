package com.leon.agenthub.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 知识库模块配置 — 绑定 `agenthub.chunk.*` 和 `agenthub.storage.*` 前缀。
 */
@Data
@ConfigurationProperties(prefix = "agenthub")
public class KnowledgeProperties {

    /** 分块配置 */
    private final Chunk chunk = new Chunk();

    /** 存储配置 */
    private final Storage storage = new Storage();

    @Data
    public static class Chunk {
        /** 每块 token 数 */
        private int size = 1000;

        /**
         * 每块的最小字符数 —— 小于该长度的分块会被合并到相邻分块。
         *
         * <p>Spring AI 的 {@code TokenTextSplitter} 不支持 overlap（重叠）参数，
         * 因此这里只暴露「块大小」与「最小块长度」两项。
         */
        private int minSize = 400;
    }

    @Data
    public static class Storage {
        /** 本地存储根路径 */
        private String rootPath = "./data/docs";
    }
}
