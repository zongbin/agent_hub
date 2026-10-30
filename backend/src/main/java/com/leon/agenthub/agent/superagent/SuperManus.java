package com.leon.agenthub.agent.superagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import com.leon.agenthub.agent.base.ToolCallAgent;
import com.leon.agenthub.common.advisor.MyLoggerAdvisor;

/**
 * 超级智能体（Manus）— 全能型工具调用代理。
 *
 * <p>继承 {@link ToolCallAgent} 的思考-行动循环：模型自行决定调用哪些工具、调用顺序，
 * 每一步的结果通过 SSE 推送给前端，直到模型调用终止工具或达到最大步数。
 *
 * <p>实例持有运行期状态，<b>不可作为单例共享</b>，由 {@code SuperAgentService} 为每个任务新建。
 */
public class SuperManus extends ToolCallAgent {

    /** 系统提示词 */
    private static final String SYSTEM_PROMPT = """
            You are SuperManus, an all-capable AI assistant, aimed at solving any task presented by the user.
            You have various tools at your disposal that you can call upon to efficiently complete complex requests.
            """;

    /** 下一步提示词 */
    private static final String NEXT_STEP_PROMPT = """
            Based on user needs, proactively select the most appropriate tool or combination of tools.
            For complex tasks, you can break down the problem and use different tools step by step to solve it.
            After using each tool, clearly explain the execution results and suggest the next steps.
            If you want to stop the interaction at any point, use the `terminate` tool/function call.
            """;

    public SuperManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("superManus");
        this.setSystemPrompt(SYSTEM_PROMPT);
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(10);
        // 初始化客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
