package com.leon.agenthub.dashboard.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 工作台统计数据 — 一次性返回工作台所需的全部真实数据。
 *
 * <p>数据来源：
 * <ul>
 *   <li>{@code document} —— 当前用户可见范围内的文档（按角色 × 密级 + 文档级授权过滤）</li>
 *   <li>{@code qa} / {@code agent} —— 当前用户的会话与提问数据</li>
 *   <li>{@code activity} —— 当前用户的审计操作记录</li>
 * </ul>
 */
@Data
public class DashboardStatsVO implements Serializable {

    /** 知识库文档统计 */
    private DocumentStatVO document;

    /** 智能问答统计 */
    private QaStatVO qa;

    /** 超级智能体统计 */
    private AgentStatVO agent;

    /** 操作活跃度统计 */
    private ActivityStatVO activity;

    /** 可见文档的密级分布 */
    private List<LevelCountVO> levelDistribution;

    /** 最近上传的可见文档 */
    private List<RecentDocumentVO> recentDocuments;

    /** 最近会话（问答 + 智能体，按更新时间倒序） */
    private List<RecentConversationVO> recentConversations;

    /** 近 7 日问答与智能体任务趋势 */
    private List<TrendPointVO> trend;
}
