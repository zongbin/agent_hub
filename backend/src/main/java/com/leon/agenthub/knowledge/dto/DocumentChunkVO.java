package com.leon.agenthub.knowledge.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 文档分块视图对象 — 返回给前端的分块内容。
 */
@Data
public class DocumentChunkVO implements Serializable {

    /** 分块序号（从 0 开始） */
    private Integer chunkIndex;

    /** 分块文本内容 */
    private String content;

    /** 分块字符数 */
    private Integer charCount;
}
