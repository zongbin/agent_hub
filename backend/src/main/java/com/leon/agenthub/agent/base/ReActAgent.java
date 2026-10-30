package com.leon.agenthub.agent.base;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * ReAct（Reasoning and Acting）模式代理抽象类 — 思考-行动循环。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent {

    /**
     * 处理当前状态并决定下一步行动。
     *
     * @return true 表示需要执行行动，false 表示不需要
     */
    public abstract boolean think();

    /**
     * 执行决定的行动。
     *
     * @return 行动执行结果
     */
    public abstract String act();

    /**
     * 执行单个步骤：思考 + 行动。
     *
     * @return 步骤执行结果
     */
    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                return "思考完成 - 无需行动";
            }
            return act();
        } catch (Exception e) {
            log.error("步骤执行失败", e);
            return "步骤执行失败: " + e.getMessage();
        }
    }
}
