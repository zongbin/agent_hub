package com.leon.agenthub.dashboard.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 超级智能体统计 — 当前用户的任务与会话数据。
 */
@Data
public class AgentStatVO implements Serializable {

    /** 会话总数 */
    private long conversationTotal;

    /** 累计任务数（一次提问计为一个任务） */
    private long taskTotal;

    /** 今日任务数 */
    private long todayTaskCount;
}
