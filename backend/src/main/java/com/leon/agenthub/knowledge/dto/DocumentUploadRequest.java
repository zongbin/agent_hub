package com.leon.agenthub.knowledge.dto;

import com.leon.agenthub.permission.enums.SecurityLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 文档上传请求 DTO — 接收前端上传时提交的元信息。
 */
@Data
public class DocumentUploadRequest implements Serializable {

    /** 文档标题（可选，默认取文件名） */
    private String title;

    /** 安全密级：PUBLIC / INTERNAL / CONFIDENTIAL */
    @NotNull(message = "安全密级不能为空")
    private SecurityLevel securityLevel;

    /** 标签，逗号分隔字符串（如 "API,规范"） */
    private String tags;
}
