package com.leon.agenthub.common.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import com.leon.agenthub.auth.security.SecurityContextUtil;
import com.leon.agenthub.common.advisor.MyLoggerAdvisor;
import com.leon.agenthub.common.advisor.SensitiveWordAdvisor;
import com.leon.agenthub.common.exception.BusinessException;
import com.leon.agenthub.permission.enums.UserRole;
import com.leon.agenthub.qa.rag.RagContext;
import com.leon.agenthub.qa.rag.RagRetriever;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 对话服务 — 封装 Spring AI 的 {@link ChatClient}，底层走阿里云 DashScope qwen。
 *
 * <p>提供同步调用、带知识库召回的流式调用和摘要标签生成功能。
 */
@Slf4j
@Service
public class ChatService {

    private final ChatClient chatClient;

    /** RAG 检索器 — 提供参考资料与引用来源 */
    private final RagRetriever ragRetriever;


    public ChatService(ChatModel dashscopeChatModel, ChatMemory databaseChatMemory, RagRetriever ragRetriever) {

        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(databaseChatMemory),
                        new MyLoggerAdvisor(),
                        new SensitiveWordAdvisor())
                .build();
        this.ragRetriever = ragRetriever;
    }

    /**
     * 流式调用 LLM，返回结构化结果（回答内容流 + 引用来源）。
     *
     * <p>参考资料由 Spring AI 的 RAG 组件召回后作为 system 消息注入，
     * 引用来源与回答内容一起返回，由前端统一渲染（支持跳转到文档详情页）。
     *
     * @param question 用户问题
     * @param chatId   会话 ID（用于多轮上下文）
     * @return 回答内容流与引用来源
     */
    public ChatStreamResult chatStream(String question, String chatId) {

        Long userId = SecurityContextUtil.getCurrentUserId();
        UserRole userRole = SecurityContextUtil.getCurrentUserRole();

        // 1. 权限感知召回参考资料
        RagContext rag = ragRetriever.retrieve(question, userId, userRole);

        // 2. 参考资料作为 system 消息注入（不放进 user 消息，避免污染对话记忆）
        //    注意：请求级 system 会覆盖客户端默认 system，因此这里需要与系统提示词拼接
        Flux<String> response = chatClient.prompt()
                .system(SystemPromptFactory.getQaSystemPrompt(question, rag.contextText()))
                .user(question)
                .advisors(spec -> spec
                        .param(MessageChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(MessageChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();
        // 把模型服务的原始异常（如账户欠费 Arrearage、限流）转成可读的业务异常
        Flux<String> stream = response.onErrorMap(e -> {
            log.error("LLM stream call failed: {}", e.getMessage(), e);
            return new BusinessException(500, "模型服务调用失败：" + rootMessageOf(e));
        });
        return new ChatStreamResult(stream, rag.citations());
    }

    /**
     * 取异常链中最内层的可读信息（去掉 Spring AI 的层层包装）。
     */
    private String rootMessageOf(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return message.length() > 300 ? message.substring(0, 300) : message;
    }
}
