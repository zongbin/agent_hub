package com.leon.agenthub.dashboard.service;

import com.leon.agenthub.agent.repository.AgentConversationRepository;
import com.leon.agenthub.agent.repository.AgentMessageRepository;
import com.leon.agenthub.auth.security.SecurityContextUtil;
import com.leon.agenthub.dashboard.converter.DashboardConverter;
import com.leon.agenthub.dashboard.dto.ActivityStatVO;
import com.leon.agenthub.dashboard.dto.AgentStatVO;
import com.leon.agenthub.dashboard.dto.DashboardStatsVO;
import com.leon.agenthub.dashboard.dto.DocumentStatVO;
import com.leon.agenthub.dashboard.dto.LevelCountVO;
import com.leon.agenthub.dashboard.dto.QaStatVO;
import com.leon.agenthub.dashboard.dto.RecentConversationVO;
import com.leon.agenthub.dashboard.dto.RecentDocumentVO;
import com.leon.agenthub.dashboard.dto.TrendPointVO;
import com.leon.agenthub.agent.enums.AgentMessageRole;
import com.leon.agenthub.knowledge.enums.DocumentStatus;
import com.leon.agenthub.knowledge.repository.DocumentRepository;
import com.leon.agenthub.permission.annotation.RequirePermission;
import com.leon.agenthub.permission.enums.PermissionAction;
import com.leon.agenthub.permission.repository.AuditLogRepository;
import com.leon.agenthub.qa.enums.MessageRole;
import com.leon.agenthub.qa.repository.ChatMemoryMessageRepository;
import com.leon.agenthub.qa.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台服务 — 汇总工作台所需的真实统计数据。
 *
 * <p>数据口径：
 * <ul>
 *   <li><b>文档相关</b>取自 {@code documents} 表，按当前用户的角色 × 密级 + 文档级授权过滤，
 *       与文档列表、向量检索的可见性规则完全一致；</li>
 *   <li><b>问答 / 智能体 / 操作活跃度</b>取自当前用户自己的会话、消息与审计日志。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    /** 最近文档条数 */
    private static final int RECENT_DOCUMENT_LIMIT = 6;

    /** 最近会话条数（问答 + 智能体合计） */
    private static final int RECENT_CONVERSATION_LIMIT = 6;

    /** 趋势统计天数 */
    private static final int TREND_DAYS = 7;

    private final DocumentRepository documentRepository;
    private final ConversationRepository conversationRepository;
    private final ChatMemoryMessageRepository chatMemoryMessageRepository;
    private final AgentConversationRepository agentConversationRepository;
    private final AgentMessageRepository agentMessageRepository;
    private final AuditLogRepository auditLogRepository;

    /**
     * 查询工作台统计数据。
     *
     * @return 工作台完整统计数据
     */
    @Transactional(readOnly = true)
    @RequirePermission(resource = "DASHBOARD", action = PermissionAction.READ)
    public DashboardStatsVO stats() {
        Long userId = SecurityContextUtil.getCurrentUserId();
        String userRole = SecurityContextUtil.getCurrentUserRole().getCode();

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        OffsetDateTime dayStart = today.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime trendStart = today.minusDays(TREND_DAYS - 1L).atStartOfDay(zone).toOffsetDateTime();

        DashboardStatsVO stats = new DashboardStatsVO();
        stats.setDocument(documentStat(userId, userRole));
        stats.setQa(qaStat(userId, dayStart));
        stats.setAgent(agentStat(userId, dayStart));
        stats.setActivity(activityStat(userId, dayStart, trendStart));
        stats.setLevelDistribution(levelDistribution(userId, userRole));
        stats.setRecentDocuments(recentDocuments(userId, userRole));
        stats.setRecentConversations(recentConversations(userId));
        stats.setTrend(trend(userId, today, trendStart, zone));
        return stats;
    }

    /**
     * 文档统计 — 可见范围内的文档数、状态分布、分块数与体积。
     */
    private DocumentStatVO documentStat(Long userId, String userRole) {
        Map<DocumentStatus, Long> statusCounts = new EnumMap<>(DocumentStatus.class);
        for (Object[] row : documentRepository.countVisibleByStatus(userId, userRole)) {
            DocumentStatus status = DashboardConverter.toStatus(toInt(row[0]));
            statusCounts.merge(status, toLong(row[1]), Long::sum);
        }

        DocumentStatVO vo = new DocumentStatVO();
        vo.setTotal(statusCounts.values().stream().mapToLong(Long::longValue).sum());
        vo.setActive(statusCounts.getOrDefault(DocumentStatus.ACTIVE, 0L));
        vo.setProcessing(statusCounts.getOrDefault(DocumentStatus.PROCESSING, 0L));
        vo.setFailed(statusCounts.getOrDefault(DocumentStatus.FAILED, 0L));
        vo.setChunkTotal(totalChunkCount(userId, userRole));
        vo.setSizeBytes(totalSizeBytes(userId, userRole));
        return vo;
    }

    /**
     * 可见文档的分块总数。
     */
    private long totalChunkCount(Long userId, String userRole) {
        Long total = documentRepository.sumVisibleChunkCount(userId, userRole);
        return total == null ? 0L : total;
    }

    /**
     * 可见文档的总体积（字节）。
     */
    private long totalSizeBytes(Long userId, String userRole) {
        Long total = documentRepository.sumVisibleFileSize(userId, userRole);
        return total == null ? 0L : total;
    }

    /**
     * 密级分布。
     */
    private List<LevelCountVO> levelDistribution(Long userId, String userRole) {
        List<LevelCountVO> distribution = new ArrayList<>();
        for (Object[] row : documentRepository.countVisibleBySecurityLevel(userId, userRole)) {
            distribution.add(DashboardConverter.toLevelCountVO(String.valueOf(row[0]), toLong(row[1])));
        }
        return distribution;
    }

    /**
     * 最近上传的可见文档。
     */
    private List<RecentDocumentVO> recentDocuments(Long userId, String userRole) {
        return DashboardConverter.toRecentDocumentVOList(
                documentRepository.findRecentVisible(userId, userRole, RECENT_DOCUMENT_LIMIT));
    }

    /**
     * 智能问答统计。
     */
    private QaStatVO qaStat(Long userId, OffsetDateTime dayStart) {
        QaStatVO vo = new QaStatVO();
        vo.setConversationTotal(conversationRepository.countByUserId(userId));
        vo.setQuestionTotal(chatMemoryMessageRepository
                .countRoleByUser(MessageRole.USER.name(), userId));
        vo.setTodayQuestionCount(chatMemoryMessageRepository
                .countRoleByUserSince(MessageRole.USER.name(), userId, dayStart));
        return vo;
    }

    /**
     * 超级智能体统计。
     */
    private AgentStatVO agentStat(Long userId, OffsetDateTime dayStart) {
        AgentStatVO vo = new AgentStatVO();
        vo.setConversationTotal(agentConversationRepository.countByUserId(userId));
        vo.setTaskTotal(agentMessageRepository
                .countRoleByUser(AgentMessageRole.USER.name(), userId));
        vo.setTodayTaskCount(agentMessageRepository
                .countRoleByUserSince(AgentMessageRole.USER.name(), userId, dayStart));
        return vo;
    }

    /**
     * 操作活跃度统计（来自审计日志）。
     */
    private ActivityStatVO activityStat(Long userId, OffsetDateTime dayStart, OffsetDateTime trendStart) {
        ActivityStatVO vo = new ActivityStatVO();
        vo.setTodayActions(auditLogRepository.countByUserIdAndCreatedAtAfter(userId, dayStart));
        vo.setWeekActions(auditLogRepository.countByUserIdAndCreatedAtAfter(userId, trendStart));
        vo.setTotalActions(auditLogRepository.countByUserId(userId));
        return vo;
    }

    /**
     * 最近会话 — 问答与智能体合并，按最近更新时间倒序。
     */
    private List<RecentConversationVO> recentConversations(Long userId) {
        PageRequest pageRequest = PageRequest.of(0, RECENT_CONVERSATION_LIMIT);

        List<RecentConversationVO> merged = new ArrayList<>();
        conversationRepository.findRecentConversations(userId, pageRequest)
                .forEach(c -> merged.add(DashboardConverter.toRecentConversationVO(c)));
        agentConversationRepository.findRecentConversations(userId, pageRequest)
                .forEach(c -> merged.add(DashboardConverter.toRecentConversationVO(c)));

        merged.sort(Comparator.comparing(
                RecentConversationVO::getUpdatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return merged.size() > RECENT_CONVERSATION_LIMIT
                ? merged.subList(0, RECENT_CONVERSATION_LIMIT)
                : merged;
    }

    /**
     * 近 7 日趋势 — 每日问答次数与智能体任务数。
     *
     * <p>数据库按 {@code created_at} 分组，缺失的日期补 0，保证前端图表点数固定。
     */
    private List<TrendPointVO> trend(Long userId, LocalDate today, OffsetDateTime trendStart, ZoneId zone) {
        String zoneId = zone.getId();

        Map<String, Long> qaCounts = toDateCountMap(chatMemoryMessageRepository
                .countRoleByUserGroupByDay(MessageRole.USER.name(), userId, trendStart, zoneId));
        Map<String, Long> agentCounts = toDateCountMap(agentMessageRepository
                .countRoleByUserGroupByDay(AgentMessageRole.USER.name(), userId, trendStart, zoneId));

        List<TrendPointVO> points = new ArrayList<>(TREND_DAYS);
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            TrendPointVO point = new TrendPointVO();
            point.setDate(date);
            point.setQaCount(qaCounts.getOrDefault(date, 0L).intValue());
            point.setAgentCount(agentCounts.getOrDefault(date, 0L).intValue());
            points.add(point);
        }
        return points;
    }

    /**
     * 把 {@code [yyyy-MM-dd, count]} 行转换为日期 → 数量的映射。
     */
    private Map<String, Long> toDateCountMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(String.valueOf(row[0]), toLong(row[1]));
        }
        return result;
    }

    /**
     * 安全地把原生查询的数值列转为 long。
     */
    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    /**
     * 安全地把原生查询的数值列转为 int。
     */
    private int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : DocumentStatus.ACTIVE.getCode();
    }
}
