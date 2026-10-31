package com.leon.agenthub.dashboard.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 知识库文档统计 — 当前用户可见范围内的文档概览。
 *
 * <p>可见范围由角色 × 密级 + 文档级授权决定，与文档列表、向量检索的过滤规则一致。
 */
@Data
public class DocumentStatVO implements Serializable {

    /** 可见文档总数 */
    private long total;

    /** 已就绪文档数（可被检索） */
    private long active;

    /** 处理中文档数 */
    private long processing;

    /** 处理失败文档数 */
    private long failed;

    /** 已索引分块总数 */
    private long chunkTotal;

    /** 文档总体积（字节） */
    private long sizeBytes;
}
