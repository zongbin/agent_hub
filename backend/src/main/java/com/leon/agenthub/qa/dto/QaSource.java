package com.leon.agenthub.qa.dto;

import lombok.Data;

/**
 * 引用来源 — 答案引用的文档分块信息。
 */
@Data
public class QaSource {

    /** 文档 ID */
    private Long documentId;

    /** 文档标题 */
    private String title;

    /** 分块序号 */
    private Integer chunkIndex;

    /** 分块内容片段（截断后） */
    private String snippet;
}
