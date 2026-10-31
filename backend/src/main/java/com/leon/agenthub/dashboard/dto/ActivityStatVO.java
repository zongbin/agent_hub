package com.leon.agenthub.dashboard.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 操作活跃度统计 — 取自审计日志，反映当前用户的真实操作轨迹。
 */
@Data
public class ActivityStatVO implements Serializable {

    /** 今日操作数 */
    private long todayActions;

    /** 近 7 日操作数 */
    private long weekActions;

    /** 累计操作数 */
    private long totalActions;
}
