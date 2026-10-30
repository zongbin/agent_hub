package com.leon.agenthub.common.llm;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leon.agenthub.auth.security.SecurityContextUtil;
import com.leon.agenthub.common.advisor.MyLoggerAdvisor;
import com.leon.agenthub.common.advisor.SensitiveWordAdvisor;
import com.leon.agenthub.common.client.LlmSummaryResult;
import com.leon.agenthub.common.exception.BusinessException;
import com.leon.agenthub.permission.enums.UserRole;
import com.leon.agenthub.qa.rag.RagContext;
import com.leon.agenthub.qa.rag.RagRetriever;

import jakarta.annotation.Resource;
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

    @Resource
    private SecurityContextUtil securityContextUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
     * 同步调用 LLM 生成文本。
     *
     * @param prompt 提示词
     * @return 生成的文本内容
     */
    public String chat(String prompt) {
        try {
            return chatClient.prompt(prompt).call().content();
        } catch (Exception e) {
            log.error("LLM chat call failed: {}", e.getMessage(), e);
            throw new BusinessException(500, "LLM 调用失败: " + e.getMessage());
        }
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

        Long userId = securityContextUtil.getCurrentUserId();
        UserRole userRole = securityContextUtil.getCurrentUserRole();

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

    /**
     * 生成文档摘要和自动标签。
     *
     * @param text 文档文本内容（过长时会被截断）
     * @return 摘要和标签结果
     */
    public LlmSummaryResult generateSummaryAndTags(String text) {
        // 截断过长文本（取前 5000 字符，节省 token）
        String truncatedText = text.length() > 5000 ? text.substring(0, 5000) : text;

        String promptText = """
                你是一个企业知识助手。请对以下文档生成：
                1. 一个简洁的摘要（不超过 200 字）
                2. 3-5 个标签关键词（用逗号分隔）

                请按以下 JSON 格式返回：
                {"summary": "摘要内容", "tags": ["标签1", "标签2", "标签3"]}

                文档内容：
                """ + truncatedText;

        try {
            String content = chatClient.prompt(promptText)
                    .call()
                    .content();

            return parseSummaryResponse(content, text);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Summary generation failed: {}", e.getMessage(), e);
            throw new BusinessException(500, "摘要生成失败: " + e.getMessage());
        }
    }

    /**
     * 解析 LLM 返回的 JSON 摘要结果。
     */
    private LlmSummaryResult parseSummaryResponse(String content, String originalText) {
        try {
            // 简单提取 JSON 部分
            int jsonStart = content.indexOf("{");
            int jsonEnd = content.lastIndexOf("}");
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String json = content.substring(jsonStart, jsonEnd + 1);
                @SuppressWarnings("unchecked")
                Map<String, Object> result = objectMapper.readValue(json, Map.class);

                String summary = (String) result.getOrDefault("summary", "");
                if (summary.length() > 200) {
                    summary = summary.substring(0, 200);
                }

                Object tagsObj = result.get("tags");
                String[] tags;
                if (tagsObj instanceof List<?> tagList) {
                    tags = tagList.stream().map(Object::toString).toArray(String[]::new);
                } else if (tagsObj instanceof String tagStr) {
                    tags = tagStr.split("[,，]");
                } else {
                    tags = new String[0];
                }

                return new LlmSummaryResult(summary, tags);
            }
        } catch (Exception e) {
            log.warn("Failed to parse LLM summary response, using fallback", e);
        }

        // 降级：使用文本前 200 字作为摘要
        return new LlmSummaryResult(
                originalText.substring(0, Math.min(200, originalText.length())),
                new String[0]);
    }
}
