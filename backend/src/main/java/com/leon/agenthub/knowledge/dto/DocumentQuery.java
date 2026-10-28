package com.leon.agenthub.knowledge.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 文档查询条件 DTO — 分页列表查询参数。
 */
@Data
public class DocumentQuery implements Serializable {

    /** 页码（从 1 开始） */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 20;

    /** 按密级筛选 */
    private String securityLevel;

    /** 按标签筛选 */
    private String tag;

    /** 按状态筛选 */
    private Integer status;
}
