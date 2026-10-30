package com.leon.agenthub.agent.tool;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import com.leon.agenthub.agent.config.AgentToolProperties;

import cn.hutool.http.HttpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 资源下载工具 — 将网络资源下载到智能体工作目录。
 */
@Slf4j
@RequiredArgsConstructor
public class ResourceDownloadTool {

    /** 工具配置 */
    private final AgentToolProperties properties;

    /**
     * 下载指定 URL 的资源。
     */
    @Tool(description = """
            Download a resource from a given http/https URL and save it into the agent working directory.
            """)
    public String downloadResource(
            @ToolParam(description = "URL of the resource to download") String url,
            @ToolParam(description = "Name of the file to save the downloaded resource, e.g. data.csv") String fileName) {
        Path filePath = resolveFilePath(fileName);
        try {
            String scheme = URI.create(url).getScheme();
            if (scheme == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                return "仅支持 http/https 协议的下载链接: " + url;
            }
            Files.createDirectories(filePath.getParent());
            HttpUtil.downloadFile(url, filePath.toFile());
            return "资源下载成功: " + filePath;
        } catch (Exception e) {
            log.error("下载资源失败: url={}, path={}", url, filePath, e);
            return "Error downloading resource: " + e.getMessage();
        }
    }

    /**
     * 解析（并确保存在）下载目录下的文件路径。
     */
    private Path resolveFilePath(String fileName) {
        Path dir = AgentToolSupport.resolveDir(properties.getWorkDir(), "download");
        return dir.resolve(AgentToolSupport.sanitizeFileName(fileName)).normalize();
    }
}
