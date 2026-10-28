package com.leon.agenthub.knowledge.parser;

import com.leon.agenthub.knowledge.exception.DocumentParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档解析器路由 — 根据文件扩展名选择对应的加载方式。
 * 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParserProvider {

    /**
     * 根据文件类型加载文档。
     *
     * @param resource 待解析的文件资源
     * @return Spring AI 文档列表
     * @throws DocumentParseException 类型不支持或解析失败时抛出
     */
    public List<Document> load(Resource resource) {
        String filename = resource.getFilename() == null ? "" : resource.getFilename();
        String extension = extractExtension(filename);
        log.info("Loading document: filename={}, extension={}", filename, extension);

        if ("md".contains(extension)) {
            return loadMarkdown(resource);
        }
        if ("txt".contains(extension)) {
            return loadPlainText(resource, filename);
        }
        throw new DocumentParseException("不支持的文档类型: " + extension);
    }

    private List<Document> loadMarkdown(Resource resource) {
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false)
                .withAdditionalMetadata("filename", resource.getFilename())
                .build();
        return new MarkdownDocumentReader(resource, config).get();
    }

    /**
     * 纯文本读取。
     */
    private List<Document> loadPlainText(Resource resource, String filename) {
        TextReader textReader = new TextReader(resource);
        textReader.getCustomMetadata().put("filename", resource.getFilename());
        return textReader.read();
    }

    /**
     * 提取文件扩展名（小写，不含点）。
     */
    private String extractExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }
}
