package com.leon.agenthub.agent.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.leon.agenthub.agent.service.SuperAgentService;
import com.leon.agenthub.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 超级智能体控制器 — 提供智能体任务的流式入口。
 */
@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final SuperAgentService superAgentService;

    /**
     * 流式调用超级智能体 — 每执行一步推送一次结果。
     *
     * @param message 用户任务描述
     * @return SSE 发射器
     */
    @GetMapping(value = "/manus/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatWithManus(@RequestParam String message) {
        if (message == null || message.isBlank()) {
            throw new BusinessException(400, "任务描述不能为空");
        }
        return superAgentService.chat(message);
    }
}
