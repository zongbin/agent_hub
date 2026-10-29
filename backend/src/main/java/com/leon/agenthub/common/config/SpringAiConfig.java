package com.leon.agenthub.common.config;

import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 核心配置。
 *
 * <p>LLM 对话与 Embedding 统一走阿里云 DashScope（由 {@code spring-ai-alibaba-starter} 自动装配），
 * 只有 DashScope 一套模型 Bean，因此无需 {@code @Primary}/{@code @Qualifier} 区分。
 */
@Configuration
public class SpringAiConfig {
}
