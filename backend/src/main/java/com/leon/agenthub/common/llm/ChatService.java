package com.leon.agenthub.common.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leon.agenthub.common.client.LlmSummaryResult;
import com.leon.agenthub.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * DeepSeek 对话服务 — 封装 Spring AI 的 {@link ChatClient}。
 *
 * <p>替代原有的 {@code LlmSummaryClient}（手写 RestTemplate），
 * 提供同步调用、流式调用和摘要标签生成功能。
 */
@Slf4j
//@Service
public class ChatService {

    private final ChatClient chatClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${agenthub.llm.chat-model:deepseek-chat}")
    private String chatModel;

    public ChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 同步调用 DeepSeek 生成文本。
     *
     * @param prompt 提示词
     * @return 生成的文本内容
     */
    public String chat(String prompt) {
        try {
            return chatClient.prompt(prompt).call().content();
        } catch (Exception e) {
            log.error("DeepSeek chat call failed: {}", e.getMessage(), e);
            throw new BusinessException(500, "LLM 调用失败: " + e.getMessage());
        }
    }

    /**
     * 流式调用 DeepSeek，返回文本流。
     *
     * @param prompt 提示词
     * @return 文本内容的 Flux 流
     */
    public Flux<String> chatStream(String prompt) {
        return chatClient.prompt(prompt).stream().content();
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
                new String[0]
        );
    }
}
