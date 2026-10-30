package com.leon.agenthub.agent.tool;

import java.net.URI;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import com.leon.agenthub.agent.config.AgentToolProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Web 爬取工具 — 抓取网页正文内容。
 */
@Slf4j
@RequiredArgsConstructor
public class WebScrapingTool {

    /** 工具配置 */
    private final AgentToolProperties properties;

    /**
     * 抓取网页正文。
     */
    @Tool(description = """
            Scrape the readable text content of a web page (http/https URL).
            Returns the page title and visible text, not raw HTML.
            """)
    public String scrapeWebPage(
            @ToolParam(description = "URL of the web page to scrape") String url) {
        try {
            String scheme = URI.create(url).getScheme();
            if (scheme == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                return "仅支持 http/https 协议的链接: " + url;
            }
            Document doc = Jsoup.connect(url)
                    .timeout(properties.getWebScrapeTimeoutMs())
                    .userAgent("Mozilla/5.0 (compatible; AgentHub-SuperAgent)")
                    .ignoreHttpErrors(true)
                    .get();
            String title = doc.title();
            String text = doc.text();
            return title.isBlank() ? text : ("# " + title + "\n" + text);
        } catch (Exception e) {
            log.error("爬取网页失败: {}", url, e);
            return "Error scraping web page: " + e.getMessage();
        }
    }
}
