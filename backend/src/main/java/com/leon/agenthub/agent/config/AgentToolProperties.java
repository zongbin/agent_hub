package com.leon.agenthub.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 智能体工具配置 — 绑定 `agenthub.agent.tool.*` 前缀。
 */
@Data
@ConfigurationProperties(prefix = "agenthub.agent.tool")
public class AgentToolProperties {

    /** 工具工作目录（文件读写 / PDF 生成 / 资源下载的落盘根目录） */
    private String workDir = "./data/agent";

    /** Web 搜索 API Key（为空时 WebSearchTool 不可用） */
    private String webSearchApiKey = "";

    /** Web 搜索接口地址 */
    private String webSearchApiUrl = "https://www.searchapi.io/api/v1/search";

    /** Web 搜索返回条数 */
    private int webSearchLimit = 5;

    /** Web 爬取超时（毫秒） */
    private int webScrapeTimeoutMs = 15000;
}
