package com.leon.agenthub.common.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * LLM 摘要生成结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmSummaryResult implements Serializable {

    /** 文档摘要（≤200 字） */
    private String summary;

    /** 自动标签 */
    private String[] tags;
}
