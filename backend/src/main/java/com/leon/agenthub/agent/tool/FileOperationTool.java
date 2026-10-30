package com.leon.agenthub.agent.tool;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import com.leon.agenthub.agent.config.AgentToolProperties;

import cn.hutool.core.io.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件操作工具 — 供智能体读写工作目录下的文本文件。
 *
 * <p>文件名由模型生成，必须经 {@link AgentToolSupport#sanitizeFileName(String)} 清洗，
 * 防止 {@code ../} 路径穿越到工作目录之外。
 */
@Slf4j
@RequiredArgsConstructor
public class FileOperationTool {

    /** 工具配置 */
    private final AgentToolProperties properties;

    /**
     * 读取工作目录下的文件内容。
     */
    @Tool(description = """
            Read the content of a text file in the agent working directory.
            Use this tool to review files written earlier in the task.
            """)
    public String readFile(
            @ToolParam(description = "Name of the file to read, e.g. report.md") String fileName) {
        Path filePath = resolveFilePath(fileName);
        try {
            if (!Files.exists(filePath)) {
                return "文件不存在: " + filePath;
            }
            return FileUtil.readUtf8String(filePath.toString());
        } catch (Exception e) {
            log.error("读取文件失败: {}", filePath, e);
            return "Error reading file: " + e.getMessage();
        }
    }

    /**
     * 将内容写入工作目录下的文件（覆盖写）。
     */
    @Tool(description = """
            Write text content to a file in the agent working directory.
            Existing files with the same name are overwritten.
            """)
    public String writeFile(
            @ToolParam(description = "Name of the file to write, e.g. report.md") String fileName,
            @ToolParam(description = "Content to write into the file") String content) {
        Path filePath = resolveFilePath(fileName);
        try {
            Files.createDirectories(filePath.getParent());
            FileUtil.writeUtf8String(content == null ? "" : content, filePath.toString());
            return "文件写入成功: " + filePath;
        } catch (Exception e) {
            log.error("写入文件失败: {}", filePath, e);
            return "Error writing file: " + e.getMessage();
        }
    }

    /**
     * 解析（并确保存在）工作目录下的文件路径。
     */
    private Path resolveFilePath(String fileName) {
        Path dir = AgentToolSupport.resolveDir(properties.getWorkDir(), "file");
        return dir.resolve(AgentToolSupport.sanitizeFileName(fileName)).normalize();
    }
}
