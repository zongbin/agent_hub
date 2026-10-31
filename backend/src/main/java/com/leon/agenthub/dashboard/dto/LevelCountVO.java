package com.leon.agenthub.dashboard.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 密级分布项 — 可见文档按安全密级分组的计数。
 */
@Data
public class LevelCountVO implements Serializable {

    /** 安全密级（枚举名：PUBLIC / INTERNAL / CONFIDENTIAL） */
    private String level;

    /** 该密级的文档数 */
    private long count;
}
