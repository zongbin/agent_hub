package com.leon.agenthub.agent.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

class AgentToolConfigTest {

    @Test
    void shouldRegisterAllToolsForSuperAgent() {
        AgentToolProperties properties = new AgentToolProperties();
        ToolCallback[] tools = new AgentToolConfig().allTools(properties);

        Set<String> names = java.util.Arrays.stream(tools)
                .map(t -> t.getToolDefinition().name())
                .collect(Collectors.toSet());

        assertEquals(7, tools.length, "工具数量应为 7");
        assertTrue(names.contains("readFile"), "缺少文件读取工具");
        assertTrue(names.contains("writeFile"), "缺少文件写入工具");
        assertTrue(names.contains("searchWeb"), "缺少网页搜索工具");
        assertTrue(names.contains("scrapeWebPage"), "缺少网页爬取工具");
        assertTrue(names.contains("downloadResource"), "缺少资源下载工具");
        assertTrue(names.contains("generatePDF"), "缺少 PDF 生成工具");
        assertTrue(names.contains("doTerminate"), "缺少终止工具");
    }
}
