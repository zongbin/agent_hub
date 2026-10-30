package com.leon.agenthub.agent.base;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.leon.agenthub.agent.enums.AgentStateEnum;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 抽象基础代理类 — 管理代理状态与执行流程。
 *
 * <p>提供状态转换、上下文管理和基于步骤的执行循环，子类必须实现 {@link #step()}。
 *
 * <p>注意：代理实例持有运行期状态（步骤数、消息上下文），因此<b>每次任务必须使用新实例</b>，
 * 不可作为单例 Bean 在多个请求间共享。
 */
@Data
public abstract class BaseAgent {

    /**
     * 日志器 — 显式声明为 protected 供子类复用。
     *
     * <p>若父类使用 {@code @Slf4j}，Lombok 会因同名字段已存在而不再在子类中生成 log，
     * 导致子类访问父类 private 字段编译失败。
     */
    protected static final Logger log = LoggerFactory.getLogger(BaseAgent.class);

    /** 代理名称 */
    private String name;

    /** 系统提示词 */
    private String systemPrompt;

    /** 下一步提示词 */
    private String nextStepPrompt;

    /** 执行状态 */
    private AgentStateEnum state = AgentStateEnum.IDLE;

    /** 最大执行步数 */
    private int maxSteps = 10;

    /** 当前执行到第几步 */
    private int currentStep = 0;

    /** LLM 客户端 */
    private ChatClient chatClient;

    /** 上下文消息（自行维护会话上下文） */
    private List<Message> messageList = new ArrayList<>();

    /**
     * 运行代理（流式输出）— 每执行一步推送一次结果。
     *
     * @param userPrompt 用户提示词
     * @return SSE 发射器
     */
    public SseEmitter run(String userPrompt) {
        // SSE 连接超时 5 分钟，与 LLM 长回答耗时匹配
        SseEmitter emitter = new SseEmitter(300_000L);

        // 异步执行，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                if (this.state != AgentStateEnum.IDLE) {
                    emitter.send("错误：无法从状态运行代理: " + this.state);
                    emitter.complete();
                    return;
                }
                if (userPrompt == null || userPrompt.isBlank()) {
                    emitter.send("错误：不能使用空提示词运行代理");
                    emitter.complete();
                    return;
                }

                // 变更状态
                state = AgentStateEnum.RUNNING;
                // 记录消息上下文
                messageList.add(new UserMessage(userPrompt));

                try {
                    for (int i = 0; i < maxSteps && state != AgentStateEnum.FINISHED; i++) {
                        int stepNumber = i + 1;
                        currentStep = stepNumber;
                        log.info("Executing step {}/{}", stepNumber, maxSteps);

                        // 单步执行
                        String stepResult = step();
                        String result = "Step " + stepNumber + ": " + stepResult;

                        // 推送每一步的结果
                        emitter.send(result);
                    }
                    // 检查是否超出步骤限制
                    if (currentStep >= maxSteps) {
                        state = AgentStateEnum.FINISHED;
                        emitter.send("执行结束: 达到最大步骤 (" + maxSteps + ")");
                    }
                    // 正常完成
                    emitter.complete();
                } catch (Exception e) {
                    state = AgentStateEnum.ERROR;
                    log.error("执行智能体失败", e);
                    try {
                        emitter.send("执行错误: " + e.getMessage());
                        emitter.complete();
                    } catch (Exception ex) {
                        emitter.completeWithError(ex);
                    }
                } finally {
                    // 清理资源
                    this.cleanup();
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        // 超时回调
        emitter.onTimeout(() -> {
            this.state = AgentStateEnum.ERROR;
            this.cleanup();
            log.warn("SSE connection timed out");
        });

        // 完成回调
        emitter.onCompletion(() -> {
            if (this.state == AgentStateEnum.RUNNING) {
                this.state = AgentStateEnum.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });

        return emitter;
    }

    /**
     * 执行单个步骤。
     *
     * @return 步骤执行结果
     */
    public abstract String step();

    /**
     * 清理资源 — 子类可重写。
     */
    protected void cleanup() {
        // 默认无需清理
    }
}
