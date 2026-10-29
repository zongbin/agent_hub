package com.leon.agenthub.qa.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 消息视图对象。
 */
@Data
public class MessageVO {

    private Long id;

    private String role;

    private String content;

    private List<QaSource> sources;

    private Integer feedback;

    private OffsetDateTime createdAt;
}
