package com.leon.agenthub.knowledge.dto;

import com.leon.agenthub.common.dto.PageQuery;
import com.leon.agenthub.knowledge.enums.DocumentStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 文档查询条件 DTO — 继承通用分页基类 {@link PageQuery}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentQuery extends PageQuery implements Serializable {

    /** 按密级筛选 */
    private String securityLevel;

    /** 按标签筛选 */
    private String tag;

    /** 按状态筛选（枚举名，如 PROCESSING / ACTIVE / FAILED） */
    private DocumentStatus status;
}
