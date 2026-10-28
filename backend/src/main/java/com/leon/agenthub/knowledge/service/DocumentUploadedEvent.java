package com.leon.agenthub.knowledge.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

/**
 * 文档上传事件 — 发布后触发异步解析处理。
 */
@Getter
@AllArgsConstructor
public class DocumentUploadedEvent {

    /** 文档 ID */
    private final Long docId;

    /** 文件存储路径 */
    private final Path filePath;

    /** 原始文件名 */
    private final String originalFilename;

    /** 文件类型 */
    private final String fileType;
}
