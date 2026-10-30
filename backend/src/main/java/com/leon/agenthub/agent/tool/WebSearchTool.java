package com.leon.agenthub.agent.tool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import com.leon.agenthub.agent.config.AgentToolProperties;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Web 搜索工具 — 通过 SearchAPI 调用搜索引擎。
 */
@Slf4j
@RequiredArgsConstructor
public class WebSearchTool {

    /** 工具配置 */
    private final AgentToolProperties properties;

    /**
     * 搜索互联网信息。
     */
    @Tool(description = """
            Search the internet for up-to-date information using a search engine.
            Returns the top matching search results.
            """)
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {
        String apiKey = properties.getWebSearchApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return "Error searching web: 未配置 web 搜索 API Key（agenthub.agent.tool.web-search-api-key），该工具暂不可用。";
        }
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", "bing");
        try {
            String response = HttpUtil.get(properties.getWebSearchApiUrl(), paramMap);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");
            if (organicResults == null || organicResults.isEmpty()) {
                return "未搜索到结果: " + query;
            }
            int limit = Math.max(1, properties.getWebSearchLimit());
            int size = Math.min(limit, organicResults.size());
            List<Object> objects = organicResults.subList(0, size);
            return objects.stream()
                    .map(obj -> ((JSONObject) obj).toString())
                    .collect(Collectors.joining(",\n"));
        } catch (Exception e) {
            log.error("Web 搜索失败: {}", query, e);
            return "Error searching web: " + e.getMessage();
        }
    }
}
