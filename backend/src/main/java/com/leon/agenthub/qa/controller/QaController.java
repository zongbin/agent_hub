package com.leon.agenthub.qa.controller;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.leon.agenthub.auth.security.SecurityContextUtil;
import com.leon.agenthub.common.exception.BusinessException;
import com.leon.agenthub.common.response.R;
import com.leon.agenthub.qa.dto.ConversationVO;
import com.leon.agenthub.qa.dto.MessageVO;
import com.leon.agenthub.qa.dto.QaAnswerVO;
import com.leon.agenthub.qa.dto.QaDeltaVO;
import com.leon.agenthub.qa.dto.QaErrorVO;
import com.leon.agenthub.qa.dto.QaFeedbackRequest;
import com.leon.agenthub.qa.dto.QaRequest;
import com.leon.agenthub.qa.service.QaService;

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

    /**
     * 流式问答 — 依次推送 delta、done/error 事件。
     *
     * <p>引用来源不单独提前推送，而是在回答完成后随 done 事件一起返回，
     * 由前端统一渲染（可跳转到对应文档详情页）。
     */
    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter ask(@RequestBody QaRequest request) {
        String question = request == null ? null : request.getQuestion();
        if (question == null || question.isBlank()) {
            throw new BusinessException(400, "问题不能为空");
        }
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new BusinessException(400, "问题长度不能超过 " + MAX_QUESTION_LENGTH + " 字符");
        }

        Long userId = SecurityContextUtil.getCurrentUserId();
        QaService.QaResult result = qaService.ask(question, request.getConversationId(), userId);

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

        StringBuilder answer = new StringBuilder();
        // 逐段推送答案增量，结束后推送元数据 done 事件
        result.chat().content().subscribe(
                delta -> {
                    if (closed.get()) {
                        return;
                    }
                    answer.append(delta);
                    try {
                        emitter.send(SseEmitter.event().name("delta").data(QaDeltaVO.of(delta)));
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
                        emitter.send(SseEmitter.event().name("error").data(QaErrorVO.of(error.getMessage())));
                    } catch (IOException ignored) {
                        // 连接已断开，无需处理
                    }
                    emitter.complete();
                },
                () -> {
                    if (!closed.compareAndSet(false, true)) {
                        return;
                    }
                    try {
                        Long messageId = qaService.saveAssistantMessage(
                                result.conversationId(), answer.toString(), result.chat().citations());
                        emitter.send(SseEmitter.event().name("done")
                                .data(buildDoneVO(result, messageId)));
                    } catch (Exception e) {
                        // 保存引用来源或推送失败：给出可读错误，避免异常被静默丢弃
                        log.error("保存回答或推送结果失败: {}", e.getMessage(), e);
                        try {
                            emitter.send(SseEmitter.event().name("error")
                                    .data(QaErrorVO.of("回答已生成，但保存引用来源失败：" + e.getMessage())));
                        } catch (IOException ignored) {
                            // 连接已断开，无需处理
                        }
                    }
                    emitter.complete();
                }
        );

        return emitter;
    }

    /**
     * 提交回答反馈。
     */
    @PostMapping("/feedback")
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
    public R<List<ConversationVO>> conversations() {
        return R.ok(qaService.listConversations(SecurityContextUtil.getCurrentUserId()));
    }

    /**
     * 查询某会话的消息列表。
     */
    @GetMapping("/conversations/{id}/messages")
    public R<List<MessageVO>> messages(@PathVariable String id) {
        return R.ok(qaService.listMessages(id, SecurityContextUtil.getCurrentUserId()));
    }

    /**
     * 删除会话及其消息。
     */
    @DeleteMapping("/conversations/{id}")
    public R<Void> deleteConversation(@PathVariable String id) {
        qaService.deleteConversation(id, SecurityContextUtil.getCurrentUserId());
        return R.ok();
    }

    /**
     * 组装 done 事件元数据 —— 会话 ID、消息 ID 与引用来源。
     *
     * <p>回答内容已通过 delta 事件逐段推送，此处不再重复携带，避免冗余传输。
     */
    private QaAnswerVO buildDoneVO(QaService.QaResult result, Long messageId) {
        QaAnswerVO vo = new QaAnswerVO();
        vo.setConversationId(result.conversationId());
        vo.setMessageId(messageId);
        vo.setSources(result.chat().citations());
        return vo;
    }
}
