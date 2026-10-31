package com.leon.agenthub.agent.base;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import com.leon.agenthub.agent.dto.AgentStepEventDTO;
import com.leon.agenthub.agent.enums.AgentStateEnum;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 抽象基础代理类 — 管理代理状态、上下文与执行循环。
 *
 * <p>本类只负责「发生了什么」：执行过程中通过 {@link AgentEventListener} 回调上报
 * 步骤开始 / 进展 / 完成、以及最终结果或错误。<b>不持有任何 HTTP 传输对象</b>，
 * 事件如何送达客户端由 Service 层实现监听器决定。
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

    /** 事件监听器 — 由 Service 层注入，负责把运行时事件翻译成 SSE 事件 */
    private AgentEventListener eventListener;

    /**
     * 运行代理（同步）— 每执行一步通过监听器上报一次结果。
     *
     * <p>本方法不负责线程调度：是否异步执行由调用方（Service 层）决定。
     *
     * @param userPrompt 用户提示词
     */
    public void run(String userPrompt) {
        try {
            if (this.state != AgentStateEnum.IDLE) {
                emitError("无法从状态运行代理: " + this.state);
                return;
            }
            if (userPrompt == null || userPrompt.isBlank()) {
                emitError("不能使用空提示词运行代理");
                return;
            }

            // 变更状态
            state = AgentStateEnum.RUNNING;
            // 记录消息上下文
            messageList.add(new UserMessage(userPrompt));

            for (int i = 0; i < maxSteps && state != AgentStateEnum.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step {}/{}", stepNumber, maxSteps);

                // 单步执行 — 步骤内的细化事件由子类通过 emit* 回调上报
                String stepResult = step();
                log.info("Step {} result: {}", stepNumber, stepResult);
            }
            // 检查是否超出步骤限制
            if (currentStep >= maxSteps && state == AgentStateEnum.RUNNING) {
                state = AgentStateEnum.FINISHED;
                emitStepProgress("执行结束: 达到最大步骤 (" + maxSteps + ")");
            }
            // 正常完成
            emitComplete(resolveFinalResult());
        } catch (Exception e) {
            state = AgentStateEnum.ERROR;
            log.error("执行智能体失败", e);
            emitError("执行错误: " + e.getMessage());
        } finally {
            // 清理资源
            this.cleanup();
        }
    }

    /**
     * 执行单个步骤。
     *
     * @return 步骤执行结果
     */
    public abstract String step();

    /**
     * 解析最终结果 — 子类可覆写以提供更贴切的最终回答。
     *
     * @return 最终结果文本
     */
    protected String resolveFinalResult() {
        return "";
    }

    /**
     * 清理资源 — 子类可重写。
     */
    protected void cleanup() {
        // 默认无需清理
    }

    /**
     * 上报步骤开始。
     *
     * @param stepName 步骤名称
     */
    protected void emitStepStart(String stepName) {
        if (eventListener == null) {
            return;
        }
        eventListener.onStepStart(AgentStepEventDTO.ofStart(currentStep, stepName));
    }

    /**
     * 上报步骤进展。
     *
     * @param message 进展描述
     */
    protected void emitStepProgress(String message) {
        if (eventListener == null) {
            return;
        }
        eventListener.onStepProgress(AgentStepEventDTO.ofProgress(currentStep, message));
    }

    /**
     * 上报步骤完成（携带工具执行结果）。
     *
     * @param toolName   工具名
     * @param toolResult 工具执行结果
     */
    protected void emitStepComplete(String toolName, String toolResult) {
        if (eventListener == null) {
            return;
        }
        eventListener.onStepComplete(AgentStepEventDTO.ofComplete(currentStep, toolName, toolResult));
    }

    /**
     * 上报执行完成。
     *
     * @param finalResult 最终结果
     */
    protected void emitComplete(String finalResult) {
        if (eventListener == null) {
            return;
        }
        eventListener.onComplete(finalResult);
    }

    /**
     * 上报执行出错。
     *
     * @param errorMessage 错误描述
     */
    protected void emitError(String errorMessage) {
        if (eventListener == null) {
            return;
        }
        eventListener.onError(errorMessage);
    }
}
