package com.leon.agenthub.agent.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.leon.agenthub.agent.dto.AgentChatRequest;
import com.leon.agenthub.agent.dto.AgentConversationVO;
import com.leon.agenthub.agent.dto.AgentMessageVO;
import com.leon.agenthub.agent.service.AgentConversationService;
import com.leon.agenthub.agent.service.SuperAgentService;
import com.leon.agenthub.auth.security.SecurityContextUtil;
import com.leon.agenthub.common.response.R;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 超级智能体控制器 — 提供智能体任务的流式入口与会话管理接口。
 */
@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final SuperAgentService superAgentService;

    private final AgentConversationService conversationService;

    /**
     * 流式调用超级智能体 — 逐步推送步骤事件与最终结果。
     *
     * @param request 任务描述与可选会话 ID
     * @return SSE 发射器
     */
    @PostMapping(value = "/manus/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatWithManus(@RequestBody AgentChatRequest request) {
        return superAgentService.chat(request, SecurityContextUtil.getCurrentUserId());
    }

    /**
     * 查询当前用户的会话列表。
     */
    @GetMapping("/conversations")
    public R<List<AgentConversationVO>> conversations() {
        return R.ok(conversationService.listConversations(SecurityContextUtil.getCurrentUserId()));
    }

    /**
     * 查询某会话的消息列表。
     */
    @GetMapping("/conversations/{id}/messages")
    public R<List<AgentMessageVO>> messages(@PathVariable String id) {
        return R.ok(conversationService.listMessages(id, SecurityContextUtil.getCurrentUserId()));
    }

    /**
     * 删除会话及其消息。
     */
    @DeleteMapping("/conversations/{id}")
    public R<Void> deleteConversation(@PathVariable String id) {
        conversationService.deleteConversation(id, SecurityContextUtil.getCurrentUserId());
        return R.ok();
    }
}
