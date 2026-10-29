package com.leon.agenthub.permission.dto;

import com.leon.agenthub.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 审计日志查询条件 DTO — 继承通用分页基类 {@link PageQuery}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLogQuery extends PageQuery implements Serializable {

    /** 按操作人过滤 */
    private Long userId;

    /** 起始时间（ISO-8601） */
    private String startTime;

    /** 结束时间（ISO-8601） */
    private String endTime;
}
