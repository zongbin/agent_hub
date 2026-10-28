package com.leon.agenthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 智知 AgentHub 后端启动类。
 *
 * <p>模块扫描范围：com.leon.agenthub 下的所有子包（auth / qa / knowledge / agent /
 * permission / audit / connector / common）。
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class AgentHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentHubApplication.class, args);
    }
}
