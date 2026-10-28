package com.leon.agenthub.common.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring AI 核心配置。
 *
 * <p>同时引入 DashScope 和 OpenAI 两套模型 Bean，通过 {@code @Primary} 解决注入歧义：
 * <ul>
 *   <li>Embedding → 默认使用 DashScope（{@code dashscopeEmbeddingModel}）</li>
 *   <li>Chat → 默认使用 OpenAI 兼容接口（{@code openAiChatModel}，实际连接 DeepSeek）</li>
 * </ul>
 */
//@Configuration
public class SpringAiConfig {

    // /**
    //  * 将 DashScope EmbeddingModel 标记为 Primary，解决多 Bean 注入歧义。
    //  */
    // @Bean
    // @Primary
    // public EmbeddingModel primaryEmbeddingModel(
    //         @Qualifier("dashscopeEmbeddingModel") EmbeddingModel dashscopeEmbeddingModel) {
    //     return dashscopeEmbeddingModel;
    // }


    // @Bean
    // @Primary
    // public ChatModel primaryChatModel(
    //         @Qualifier("dashScopeChatModel") ChatModel dashScopeChatModel) {
    //     return dashScopeChatModel;
    // }
}
