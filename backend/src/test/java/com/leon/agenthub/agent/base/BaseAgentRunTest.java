package com.leon.agenthub.agent.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.leon.agenthub.agent.dto.AgentStepEventDTO;
import com.leon.agenthub.agent.enums.AgentEventType;

/**
 * {@link BaseAgent} 执行循环单元测试 — 验证步骤序号连续递增与最终回调。
 */
class BaseAgentRunTest {

    /** 记录监听器收到的全部事件 */
    private static class RecordingListener implements AgentEventListener {

        final List<AgentStepEventDTO> events = new ArrayList<>();
        String finalResult;
        String errorMessage;

        @Override
        public void onStepStart(AgentStepEventDTO event) {
            events.add(event);
        }

        @Override
        public void onStepProgress(AgentStepEventDTO event) {
            events.add(event);
        }

        @Override
        public void onStepComplete(AgentStepEventDTO event) {
            events.add(event);
        }

        @Override
        public void onComplete(String result) {
            finalResult = result;
        }

        @Override
        public void onError(String message) {
            errorMessage = message;
        }

        /** 取所有步骤序号 */
        List<Integer> stepIndexes() {
            return events.stream()
                    .filter(e -> e.getEventType() == AgentEventType.STEP_START)
                    .map(AgentStepEventDTO::getStepIndex)
                    .toList();
        }
    }

    /**
     * 桩代理 — 每步上报一次开始与完成，不依赖 LLM。
     */
    private static class CountingAgent extends BaseAgent {

        CountingAgent() {
            this.setName("counting");
        }

        @Override
        public String step() {
            emitStepStart("步骤 " + getCurrentStep());
            emitStepComplete(null, "第 " + getCurrentStep() + " 步结果");
            return "第 " + getCurrentStep() + " 步结果";
        }

        @Override
        protected String resolveFinalResult() {
            return "共执行 " + getCurrentStep() + " 步";
        }
    }

    @Test
    void shouldReportStepIndexIncreasingFromOne() {
        RecordingListener listener = new RecordingListener();
        CountingAgent agent = new CountingAgent();
        agent.setEventListener(listener);
        agent.setMaxSteps(3);

        agent.run("做一件事");

        assertEquals(List.of(1, 2, 3), listener.stepIndexes(), "步骤序号应从 1 开始连续递增");
    }

    @Test
    void shouldReportCompleteWhenMaxStepsReached() {
        RecordingListener listener = new RecordingListener();
        CountingAgent agent = new CountingAgent();
        agent.setEventListener(listener);
        agent.setMaxSteps(2);

        agent.run("做一件事");

        assertNotNull(listener.finalResult, "达到最大步数时应回调 onComplete");
        assertTrue(listener.finalResult.contains("2"), "最终结果应反映已执行的步数");
    }

    @Test
    void shouldReportErrorOnEmptyPrompt() {
        RecordingListener listener = new RecordingListener();
        CountingAgent agent = new CountingAgent();
        agent.setEventListener(listener);

        agent.run("  ");

        assertNotNull(listener.errorMessage, "空提示词应回调 onError");
        assertEquals(null, listener.finalResult, "出错时不应回调 onComplete");
    }

    @Test
    void shouldReportErrorWhenStepThrows() {
        RecordingListener listener = new RecordingListener();
        BaseAgent agent = new BaseAgent() {
            @Override
            public String step() {
                throw new IllegalStateException("步骤炸了");
            }
        };
        agent.setName("boom");
        agent.setEventListener(listener);

        agent.run("做一件事");

        assertNotNull(listener.errorMessage, "步骤异常时应回调 onError");
        assertTrue(listener.errorMessage.contains("步骤炸了"));
    }
}
