package com.leon.agenthub.knowledge.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 文档视图对象 — 返回给前端的文档信息。
 */
@Data
public class DocumentVO implements Serializable {

    /** 文档 ID */
    private Long id;

    /** 文档标题 */
    private String title;

    /** 文件原始名称 */
    private String fileName;

    /** 文件类型：docx/pdf/pptx/xlsx/txt/md */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 安全密级 */
    private String securityLevel;

    /** AI 生成摘要 */
    private String summary;

    /** AI 生成标签 */
    private String[] tags;

    /** 处理状态：PROCESSING / ACTIVE / FAILED */
    private String status;

    /** 分块数量 */
    private Integer chunkCount;

    /** 上传时间 */
    private OffsetDateTime createdAt;
}
