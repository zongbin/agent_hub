package com.leon.agenthub.qa.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.leon.agenthub.common.llm.ChatCitation;

import lombok.Data;

/**
 * 消息视图对象。
 */
@Data
public class MessageVO {

    private Long id;

    private String role;

    private String content;

    private List<ChatCitation> sources;

    private Integer feedback;

    private OffsetDateTime createdAt;
}
