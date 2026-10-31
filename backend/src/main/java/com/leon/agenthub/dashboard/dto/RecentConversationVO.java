package com.leon.agenthub.dashboard.dto;

import com.leon.agenthub.dashboard.enums.ConversationType;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 最近会话视图 — 工作台「最近会话」列表项（问答与智能体合并，按更新时间倒序）。
 */
@Data
public class RecentConversationVO implements Serializable {

    /** 会话类型（枚举，JSON 中为枚举名：QA / AGENT） */
    private ConversationType type;

    /** 会话 ID */
    private String id;

    /** 会话标题 */
    private String title;

    /** 最近更新时间 */
    private OffsetDateTime updatedAt;
}
