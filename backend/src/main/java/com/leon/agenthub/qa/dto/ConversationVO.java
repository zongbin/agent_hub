package com.leon.agenthub.qa.dto;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 会话视图对象。
 */
@Data
public class ConversationVO {

    private String id;

    private String title;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
