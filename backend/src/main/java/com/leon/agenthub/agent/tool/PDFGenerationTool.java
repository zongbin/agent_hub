package com.leon.agenthub.agent.tool;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.leon.agenthub.agent.config.AgentToolProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PDF 生成工具 — 将文本内容渲染为 PDF 文件。
 */
@Slf4j
@RequiredArgsConstructor
public class PDFGenerationTool {

    /** 工具配置 */
    private final AgentToolProperties properties;

    /**
     * 生成 PDF 文件。
     */
    @Tool(description = """
            Generate a PDF file with the given content in the agent working directory.
            Chinese text is supported.
            """)
    public String generatePDF(
            @ToolParam(description = "Name of the PDF file to save, e.g. report.pdf") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {
        Path filePath = resolveFilePath(fileName);
        try {
            Files.createDirectories(filePath.getParent());
            try (PdfWriter writer = new PdfWriter(filePath.toString());
                    PdfDocument pdf = new PdfDocument(writer);
                    Document document = new Document(pdf)) {
                // 内置中文字体（依赖 itext font-asian）
                PdfFont font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
                document.setFont(font);
                document.add(new Paragraph(content == null ? "" : content));
            }
            return "PDF 生成成功: " + filePath;
        } catch (Exception e) {
            log.error("生成 PDF 失败: {}", filePath, e);
            return "Error generating PDF: " + e.getMessage();
        }
    }

    /**
     * 解析（并确保存在）PDF 目录下的文件路径。
     */
    private Path resolveFilePath(String fileName) {
        Path dir = AgentToolSupport.resolveDir(properties.getWorkDir(), "pdf");
        return dir.resolve(AgentToolSupport.sanitizeFileName(fileName)).normalize();
    }
}
