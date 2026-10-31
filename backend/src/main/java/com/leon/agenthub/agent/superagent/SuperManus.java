package com.leon.agenthub.agent.superagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import com.leon.agenthub.agent.base.ToolCallAgent;
import com.leon.agenthub.common.advisor.MyLoggerAdvisor;

/**
 * 超级智能体（Manus）— 全能型工具调用代理。
 *
 * <p>
 * 继承 {@link ToolCallAgent} 的思考-行动循环：模型自行决定调用哪些工具、调用顺序，
 * 每一步的结果通过 SSE 推送给前端，直到模型调用终止工具或达到最大步数。
 *
 * <p>
 * 实例持有运行期状态，<b>不可作为单例共享</b>，由 {@code SuperAgentService} 为每个任务新建。
 */
public class SuperManus extends ToolCallAgent {

        /** 系统提示词 */
        private static final String SYSTEM_PROMPT = """
                        你是SuperManus，一个全能AI助手，旨在解决用户提出的任何任务。你可以调用各种工具来高效完成复杂请求。
                        """;

        /** 下一步提示词 */
        private static final String NEXT_STEP_PROMPT = """
                        根据用户需求，主动选择最合适的工具或工具组合。
                        对于复杂任务，你可以将问题拆解，并逐步使用不同工具来解决。
                        每次使用工具后，清晰说明执行结果并建议下一步。
                        如果你想在任何时候停止交互，请使用 terminate 工具/函数调用。
                                    """;

        public SuperManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
                super(allTools);
                this.setName("superManus");
                this.setSystemPrompt(SYSTEM_PROMPT);
                this.setNextStepPrompt(NEXT_STEP_PROMPT);
                this.setMaxSteps(20);
                // 初始化客户端
                ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                                .defaultAdvisors(new MyLoggerAdvisor())
                                .build();
                this.setChatClient(chatClient);
        }
}
