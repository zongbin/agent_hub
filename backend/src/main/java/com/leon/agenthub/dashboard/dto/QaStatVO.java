package com.leon.agenthub.dashboard.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 智能问答统计 — 当前用户的提问与会话数据。
 */
@Data
public class QaStatVO implements Serializable {

    /** 会话总数 */
    private long conversationTotal;

    /** 累计提问次数 */
    private long questionTotal;

    /** 今日提问次数 */
    private long todayQuestionCount;
}
