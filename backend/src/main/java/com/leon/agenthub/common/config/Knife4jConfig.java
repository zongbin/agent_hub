package com.leon.agenthub.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 3 配置。
 *
 * <p>启动后访问：{@code http://localhost:8080/api/doc.html}
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI agentHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智知 AgentHub API")
                        .description("企业知识管理平台 — RAG 问答 + SuperAgent 接口文档")
                        .version("v0.1.0")
                        .contact(new Contact().name("Leon").email(""))
                        .license(new License().name("Apache 2.0")));
    }
}
