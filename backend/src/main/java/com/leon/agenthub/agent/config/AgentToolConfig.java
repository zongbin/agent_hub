package com.leon.agenthub.agent.config;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.leon.agenthub.agent.tool.FileOperationTool;
import com.leon.agenthub.agent.tool.PDFGenerationTool;
import com.leon.agenthub.agent.tool.ResourceDownloadTool;
import com.leon.agenthub.agent.tool.TerminateTool;
import com.leon.agenthub.agent.tool.WebScrapingTool;
import com.leon.agenthub.agent.tool.WebSearchTool;

/**
 * 智能体工具注册配置。
 *
 * <p>注册后即被超级智能体（{@code SuperManus}）自动使用，模型自行决定调用哪些工具、调用顺序。
 * 后续按业务需要在此追加工具即可，无需改动智能体本身。
 */
@Configuration
@EnableConfigurationProperties(AgentToolProperties.class)
public class AgentToolConfig {

    /**
     * 超级智能体可用的全部工具。
     *
     * <p>包含：文件读写、PDF 生成、资源下载、网页爬取、网页搜索、终止协议。
     */
    @Bean
    public ToolCallback[] allTools(AgentToolProperties properties) {
        return ToolCallbacks.from(
                new FileOperationTool(properties),
                new WebSearchTool(properties),
                new WebScrapingTool(properties),
                new ResourceDownloadTool(properties),
                new PDFGenerationTool(properties),
                new TerminateTool());
    }
}
