package com.leon.agenthub.dashboard.dto;

import com.leon.agenthub.knowledge.enums.DocumentStatus;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 最近文档视图 — 工作台「最近上传」列表项。
 */
@Data
public class RecentDocumentVO implements Serializable {

    /** 文档 ID */
    private Long id;

    /** 文档标题 */
    private String title;

    /** 文件类型：docx/pdf/pptx/xlsx/txt/md */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 安全密级 */
    private String securityLevel;

    /** 处理状态（枚举，JSON 中为枚举名） */
    private DocumentStatus status;

    /** 分块数量 */
    private Integer chunkCount;

    /** 上传时间 */
    private OffsetDateTime createdAt;
}
