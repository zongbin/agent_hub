package com.leon.agenthub.dashboard.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 趋势数据点 — 近 7 日问答与智能体任务的数量变化。
 */
@Data
public class TrendPointVO implements Serializable {

    /** 日期（yyyy-MM-dd） */
    private String date;

    /** 当日问答次数 */
    private int qaCount;

    /** 当日智能体任务数 */
    private int agentCount;
}
