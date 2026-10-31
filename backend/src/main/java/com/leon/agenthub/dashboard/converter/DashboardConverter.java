package com.leon.agenthub.dashboard.converter;

import com.leon.agenthub.dashboard.dto.LevelCountVO;
import com.leon.agenthub.dashboard.dto.RecentConversationVO;
import com.leon.agenthub.dashboard.dto.RecentDocumentVO;
import com.leon.agenthub.dashboard.enums.ConversationType;
import com.leon.agenthub.agent.model.AgentConversation;
import com.leon.agenthub.knowledge.enums.DocumentStatus;
import com.leon.agenthub.knowledge.model.Document;
import com.leon.agenthub.qa.model.Conversation;

import java.util.List;

/**
 * 工作台转换器 — 实体与视图对象之间的映射。
 *
 * <p>放在转换器而非 Controller，避免表现层承担业务映射逻辑。
 */
public final class DashboardConverter {

    private DashboardConverter() {
    }

    /**
     * 文档实体转「最近文档」视图。
     *
     * @param doc 文档实体
     * @return 最近文档视图
     */
    public static RecentDocumentVO toRecentDocumentVO(Document doc) {
        RecentDocumentVO vo = new RecentDocumentVO();
        vo.setId(doc.getId());
        vo.setTitle(doc.getTitle());
        vo.setFileType(doc.getFileType());
        vo.setFileSize(doc.getFileSize());
        vo.setSecurityLevel(doc.getSecurityLevel());
        vo.setStatus(doc.getStatus());
        vo.setChunkCount(doc.getChunkCount());
        vo.setCreatedAt(doc.getCreatedAt());
        return vo;
    }

    /**
     * 问答会话实体转「最近会话」视图。
     *
     * @param conversation 问答会话实体
     * @return 最近会话视图
     */
    public static RecentConversationVO toRecentConversationVO(Conversation conversation) {
        return buildRecentConversationVO(ConversationType.QA, conversation.getId(),
                conversation.getTitle(), conversation.getUpdatedAt());
    }

    /**
     * 智能体会话实体转「最近会话」视图。
     *
     * @param conversation 智能体会话实体
     * @return 最近会话视图
     */
    public static RecentConversationVO toRecentConversationVO(AgentConversation conversation) {
        return buildRecentConversationVO(ConversationType.AGENT, conversation.getId(),
                conversation.getTitle(), conversation.getUpdatedAt());
    }

    /**
     * 密级分组计数转视图。
     *
     * @param level 密级
     * @param count 数量
     * @return 密级分布项
     */
    public static LevelCountVO toLevelCountVO(String level, long count) {
        LevelCountVO vo = new LevelCountVO();
        vo.setLevel(level);
        vo.setCount(count);
        return vo;
    }

    /**
     * 文档状态 code 转枚举（未识别值兜底为 {@link DocumentStatus#ACTIVE}）。
     *
     * @param statusCode 数据库存储的状态码
     * @return 状态枚举
     */
    public static DocumentStatus toStatus(Integer statusCode) {
        return DocumentStatus.fromCode(statusCode);
    }

    /**
     * 构造「最近会话」视图。
     */
    private static RecentConversationVO buildRecentConversationVO(ConversationType type, String id,
                                                                  String title,
                                                                  java.time.OffsetDateTime updatedAt) {
        RecentConversationVO vo = new RecentConversationVO();
        vo.setType(type);
        vo.setId(id);
        vo.setTitle(title);
        vo.setUpdatedAt(updatedAt);
        return vo;
    }

    /**
     * 批量转换最近文档。
     *
     * @param documents 文档实体列表
     * @return 最近文档视图列表
     */
    public static List<RecentDocumentVO> toRecentDocumentVOList(List<Document> documents) {
        return documents.stream().map(DashboardConverter::toRecentDocumentVO).toList();
    }
}
