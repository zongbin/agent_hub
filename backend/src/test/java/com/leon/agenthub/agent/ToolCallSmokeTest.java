package com.leon.agenthub.agent;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;

/**
 * 临时冒烟测试：验证模型能识别并调用新注册的工具。
 *
 * <p>走与 {@code ToolCallAgent} 相同的<b>代理式工具调用</b>模式
 * （{@code withProxyToolCalls(true)}）：工具由 Agent 自己执行，模型只负责发起调用。
 */
@SpringBootTest
@ActiveProfiles("dev")
class ToolCallSmokeTest {

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private ToolCallback[] allTools;

    @Test
    void modelShouldCallWriteFileTool() {
        ChatResponse response = org.springframework.ai.chat.client.ChatClient.builder(chatModel)
                .build()
                .prompt(new Prompt(
                        "请调用 writeFile 工具，把内容 'hello agent' 写入文件 hello.txt。",
                        DashScopeChatOptions.builder().withProxyToolCalls(true).build()))
                .tools(allTools)
                .call()
                .chatResponse();
        AssistantMessage message = response.getResult().getOutput();
        List<AssistantMessage.ToolCall> toolCalls = message.getToolCalls();
        System.out.println(">>> text = " + message.getText());
        System.out.println(">>> tool calls = " + toolCalls);
        assertTrue(!toolCalls.isEmpty(), "模型未发起任何工具调用");
        assertTrue(toolCalls.stream().anyMatch(t -> t.name().equals("writeFile")), "模型未调用 writeFile");
    }
}
