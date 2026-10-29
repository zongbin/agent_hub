package com.leon.agenthub.common.advisor;

import java.util.List;
import java.util.Set;

import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 敏感词校验 Advisor：命中敏感词时直接拦截，不再调用大模型。
 *
 * @author leon
 */
@Slf4j
@Component
public class SensitiveWordAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private static final Set<String> SENSITIVE_WORDS = Set.of(
            "枪支", "毒品", "赌博", "洗钱", "假币");

    private static final String REFUSAL_TEXT = "您的问题包含不合规内容，已拦截，请修改后重试。";

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 900;
    }

    private boolean containsSensitiveWord(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String word : SENSITIVE_WORDS) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private AdvisedResponse buildRefusalResponse(AdvisedRequest advisedRequest) {
        AssistantMessage assistantMessage = new AssistantMessage(REFUSAL_TEXT);
        ChatResponse chatResponse = ChatResponse.builder()
                .generations(List.of(new Generation(assistantMessage)))
                .build();
        return AdvisedResponse.builder()
                .response(chatResponse)
                .adviseContext(advisedRequest.adviseContext())
                .build();
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        String userText = advisedRequest.userText();
        if (containsSensitiveWord(userText)) {
            log.warn("SensitiveWordAdvisor 拦截命中: {}", userText);
            return buildRefusalResponse(advisedRequest);
        }
        return chain.nextAroundCall(advisedRequest);
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        String userText = advisedRequest.userText();
        if (containsSensitiveWord(userText)) {
            log.warn("SensitiveWordAdvisor 拦截命中: {}", userText);
            return Flux.just(buildRefusalResponse(advisedRequest));
        }
        return chain.nextAroundStream(advisedRequest);
    }
}
