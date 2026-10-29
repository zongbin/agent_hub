package com.leon.agenthub.common.llm;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leon.agenthub.auth.security.SecurityContextUtil;
import com.leon.agenthub.common.advisor.MyLoggerAdvisor;
import com.leon.agenthub.common.advisor.SensitiveWordAdvisor;
import com.leon.agenthub.common.client.LlmSummaryResult;
import com.leon.agenthub.common.exception.BusinessException;
import com.leon.agenthub.permission.repository.DocPermissionRepository;
import com.leon.agenthub.qa.rag.RagAdvisorService;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 对话服务 — 封装 Spring AI 的 {@link ChatClient}，底层走阿里云 DashScope qwen。
 *
 * <p>
 * 提供同步调用、流式调用和摘要标签生成功能。
 */
@Slf4j
@Service
public class ChatService {

    private final ChatClient chatClient;

    private final VectorStore pgVectorStore;

    // 保存 ChatClient.Builder，供 RagAdvisorService 构建查询重写器使用
    private final ChatClient.Builder chatClientBuilder;

    @Resource
    private DocPermissionRepository docPermissionRepository;
    @Resource
    private SecurityContextUtil securityContextUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatService(ChatModel dashscopeChatModel, ChatMemory databaseChatMemory, VectorStore pgVectorStore) {

        String systemPrompt = """
                你是企业知识助手，仅依据以下参考资料回答问题；若资料不足以回答，请明确说明无法回答，不要编造。
                请给出答案，并在引用处标注来源编号，如 [1]、[2]。
                """;
        this.chatClientBuilder = ChatClient.builder(dashscopeChatModel);
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(databaseChatMemory),
                        new MyLoggerAdvisor(),
                        new SensitiveWordAdvisor())
                .build();
        this.pgVectorStore = pgVectorStore;
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
     * 流式调用 LLM，返回文本流。
     *
     * @param prompt 提示词
     * @return 文本内容的 Flux 流
     */
    public Flux<String> chatStream(String question, String chatId) {

        Long userId = securityContextUtil.getCurrentUserId();
        String userRole = securityContextUtil.getCurrentUserRole().getCode();

        Flux<String> response = chatClient.prompt()
                .user(question)
                .advisors(spec -> spec
                        .param(MessageChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(MessageChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 从pgsql向量库中检索内容
                .advisors(
                        new RagAdvisorService(pgVectorStore, userId + "", userRole, docPermissionRepository, chatClientBuilder)
                                .getAdvisor()
                )
                .stream()
                .content();
        // 把模型服务的原始异常（如账户欠费 Arrearage、限流）转成可读的业务异常
        return response.onErrorMap(e -> {
            log.error("LLM stream call failed: {}", e.getMessage(), e);
            return new BusinessException(500, "模型服务调用失败：" + rootMessageOf(e));
        });
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
