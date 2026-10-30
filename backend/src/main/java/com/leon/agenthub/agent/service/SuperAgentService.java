package com.leon.agenthub.agent.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.leon.agenthub.agent.superagent.SuperManus;

import lombok.extern.slf4j.Slf4j;

/**
 * 超级智能体服务 — 负责创建智能体实例并启动任务。
 *
 * <p>智能体实例带运行期状态（步骤计数、消息上下文），因此每个任务都新建一个实例，
 * 由 {@link SuperManus#run(String)} 驱动执行并把每一步结果通过 SSE 推送出去。
 */
@Slf4j
@Service
public class SuperAgentService {

    /** 智能体可用的工具 */
    private final ToolCallback[] allTools;

    /** 底层 ChatModel（DashScope） */
    private final ChatModel dashscopeChatModel;

    public SuperAgentService(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        this.allTools = allTools;
        this.dashscopeChatModel = dashscopeChatModel;
    }

    /**
     * 以用户消息启动一次超级智能体任务。
     *
     * @param message 用户任务描述
     * @return SSE 发射器，逐步推送执行结果
     */
    public SseEmitter chat(String message) {
        log.info("启动超级智能体任务: {}", message);
        SuperManus superManus = new SuperManus(allTools, dashscopeChatModel);
        return superManus.run(message);
    }
}
