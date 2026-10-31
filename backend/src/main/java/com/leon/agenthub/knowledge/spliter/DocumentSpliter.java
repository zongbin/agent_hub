package com.leon.agenthub.knowledge.spliter;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import com.leon.agenthub.knowledge.config.KnowledgeProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文档分块器 — 基于 Spring AI 的 {@link TokenTextSplitter} 切分文档。
 *
 * <p>块大小与最小块长度由 {@code agenthub.chunk.*} 配置驱动；
 * Spring AI 的 {@code TokenTextSplitter} 未提供 overlap（重叠）参数，
 * 故本实现不支持重叠分块。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentSpliter {

    /** 最小块长度（字符）—— 小于该长度的分块会被并入相邻分块 */
    private static final int MIN_CHUNK_LENGTH_TO_EMBED = 5;

    /** 单文档最大分块数 */
    private static final int MAX_NUM_CHUNKS = 6000;

    /** 分块时保留分隔符 */
    private static final boolean KEEP_SEPARATOR = true;

    private final KnowledgeProperties knowledgeProperties;

    public List<Document> split(List<Document> documents) {
        KnowledgeProperties.Chunk chunk = knowledgeProperties.getChunk();
        // 最小块长度不得超过块大小，否则小块会被无休止地合并
        int minChunkSizeChars = Math.min(chunk.getMinSize(), chunk.getSize());
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter(
                chunk.getSize(),
                minChunkSizeChars,
                MIN_CHUNK_LENGTH_TO_EMBED,
                MAX_NUM_CHUNKS,
                KEEP_SEPARATOR);
        log.debug("Splitting {} documents with chunkSize={}, minChunkSizeChars={}",
                documents.size(), chunk.getSize(), minChunkSizeChars);
        return tokenTextSplitter.apply(documents);
    }
}
