package com.leon.agenthub.qa.controller;

import com.leon.agenthub.auth.security.SecurityContextUtil;
import com.leon.agenthub.common.exception.BusinessException;
import com.leon.agenthub.common.response.R;
import com.leon.agenthub.qa.dto.ConversationVO;
import com.leon.agenthub.qa.dto.MessageVO;
import com.leon.agenthub.qa.dto.QaFeedbackRequest;
import com.leon.agenthub.qa.dto.QaRequest;
import com.leon.agenthub.qa.dto.QaSource;
import com.leon.agenthub.qa.service.QaService;
import com.leon.agenthub.permission.annotation.RequirePermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 智能问答控制器 — 流式问答、会话管理与答案反馈。
 */
@Slf4j
@RestController
@RequestMapping("/qa")
@RequiredArgsConstructor
public class QaController {

    /** SSE 连接超时（5 分钟），与 LLM 长回答耗时匹配 */
    private static final long SSE_TIMEOUT_MS = 300_000L;

    /** 问题最大长度 */
    private static final int MAX_QUESTION_LENGTH = 2000;

    private final QaService qaService;
    private final SecurityContextUtil securityContextUtil;

    /**
     * 流式问答 — 依次推送 source、delta、done/error 事件。
     */
    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequirePermission(resource = "QA", action = "READ")
    public SseEmitter ask(@RequestBody QaRequest request) {
        String question = request == null ? null : request.getQuestion();
        if (question == null || question.isBlank()) {
            throw new BusinessException(400, "问题不能为空");
        }
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new BusinessException(400, "问题长度不能超过 " + MAX_QUESTION_LENGTH + " 字符");
        }

        Long userId = securityContextUtil.getCurrentUserId();
        String userRole = securityContextUtil.getCurrentUserRole().getCode();
        QaService.QaResult result = qaService.ask(question, request.getConversationId(), userId, userRole);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        AtomicBoolean closed = new AtomicBoolean(false);
        emitter.onTimeout(() -> {
            closed.set(true);
            emitter.complete();
        });
        emitter.onError(e -> {
            closed.set(true);
            log.warn("SSE connection error: {}", e.getMessage());
        });

        try {
            // 1. 先推送引用来源与会话 ID
            emitter.send(SseEmitter.event().name("source").data(sourcePayload(result)));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }

        StringBuilder answer = new StringBuilder();
        // 2. 逐段推送答案增量，结束后保存回答并推送 done
        result.stream().subscribe(
                delta -> {
                    if (closed.get()) {
                        return;
                    }
                    answer.append(delta);
                    try {
                        emitter.send(SseEmitter.event().name("delta").data(deltaPayload(delta)));
                    } catch (IOException e) {
                        if (closed.compareAndSet(false, true)) {
                            emitter.completeWithError(e);
                        }
                    }
                },
                error -> {
                    log.error("Q&A generation failed: {}", error.getMessage(), error);
                    if (!closed.compareAndSet(false, true)) {
                        return;
                    }
                    try {
                        emitter.send(SseEmitter.event().name("error").data(errorPayload(error.getMessage())));
                    } catch (IOException ignored) {
                        // 连接已断开，无需处理
                    }
                    emitter.complete();
                },
                () -> {
                    if (!closed.compareAndSet(false, true)) {
                        return;
                    }
                    Long messageId = qaService.saveAssistantMessage(
                            result.conversationId(), answer.toString(), result.sources());
                    try {
                        emitter.send(SseEmitter.event().name("done")
                                .data(donePayload(result.conversationId(), messageId)));
                    } catch (IOException ignored) {
                        // 连接已断开，无需处理
                    }
                    emitter.complete();
                }
        );

        return emitter;
    }

    /**
     * 提交答案反馈。
     */
    @PostMapping("/feedback")
    @RequirePermission(resource = "QA", action = "READ")
    public R<Void> feedback(@RequestBody QaFeedbackRequest request) {
        if (request == null || request.getMessageId() == null) {
            throw new BusinessException(400, "messageId 不能为空");
        }
        if (request.getUseful() == null) {
            throw new BusinessException(400, "useful 不能为空");
        }
        qaService.saveFeedback(request.getMessageId(), request.getUseful());
        return R.ok();
    }

    /**
     * 查询当前用户的会话列表。
     */
    @GetMapping("/conversations")
    @RequirePermission(resource = "QA", action = "READ")
    public R<List<ConversationVO>> conversations() {
        return R.ok(qaService.listConversations(securityContextUtil.getCurrentUserId()));
    }

    /**
     * 查询某会话的消息列表。
     */
    @GetMapping("/conversations/{id}/messages")
    @RequirePermission(resource = "QA", action = "READ")
    public R<List<MessageVO>> messages(@PathVariable String id) {
        return R.ok(qaService.listMessages(id, securityContextUtil.getCurrentUserId()));
    }

    /**
     * 删除会话及其消息。
     */
    @DeleteMapping("/conversations/{id}")
    @RequirePermission(resource = "QA", action = "READ")
    public R<Void> deleteConversation(@PathVariable String id) {
        qaService.deleteConversation(id, securityContextUtil.getCurrentUserId());
        return R.ok();
    }

    private Map<String, Object> sourcePayload(QaService.QaResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("conversationId", result.conversationId());
        List<Map<String, Object>> sources = result.sources().stream().map(this::toSourceMap).toList();
        payload.put("sources", sources);
        return payload;
    }

    private Map<String, Object> toSourceMap(QaSource source) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("documentId", source.getDocumentId());
        map.put("title", source.getTitle());
        map.put("chunkIndex", source.getChunkIndex());
        map.put("snippet", source.getSnippet());
        return map;
    }

    private Map<String, Object> deltaPayload(String content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("content", content);
        return payload;
    }

    private Map<String, Object> donePayload(String conversationId, Long messageId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("conversationId", conversationId);
        payload.put("messageId", messageId);
        return payload;
    }

    private Map<String, Object> errorPayload(String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", message == null ? "问答处理失败" : message);
        return payload;
    }
}
