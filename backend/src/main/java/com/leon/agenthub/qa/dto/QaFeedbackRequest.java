package com.leon.agenthub.qa.dto;

import lombok.Data;

/**
 * 答案反馈请求。
 */
@Data
public class QaFeedbackRequest {

    /** 回答消息 ID */
    private Long messageId;

    /** 是否有帮助 */
    private Boolean useful;
}
