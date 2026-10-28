package com.leon.agenthub.knowledge.parser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.leon.agenthub.knowledge.exception.DocumentParseException;

import lombok.extern.slf4j.Slf4j;

/**
 * 文档解析器 — 从多种格式文件中提取纯文本。
 *
 * <p>
 * 支持的格式：docx、pdf、pptx、xlsx、txt、md。
 * 使用 Apache Tika 自动检测文件类型并提取文本内容。
 *
 */
@Slf4j
@Component
public class DocumentParser {

    /**
     * 解析指定路径的文件，提取纯文本内容。
     *
     * @param filePath 文件路径
     * @return 提取的文本内容（单元素列表，保持与 Spring AI Document 读取器一致的返回格式）
     */
    public List<Document> loadDocument(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            throw new DocumentParseException("文件不存在: " + filePath);
        }
        Resource resource = new FileSystemResource(filePath);
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);
        return tikaDocumentReader.read();
    }
}
