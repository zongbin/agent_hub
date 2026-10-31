package com.leon.agenthub.knowledge.config;

import com.leon.agenthub.common.config.SpringAiConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 知识库配置启用类 — 启用 Knowledge 属性绑定，导入 Spring AI 配置。
 */
@Configuration
@EnableConfigurationProperties(KnowledgeProperties.class)
@Import(SpringAiConfig.class)
public class KnowledgeConfig {
}
