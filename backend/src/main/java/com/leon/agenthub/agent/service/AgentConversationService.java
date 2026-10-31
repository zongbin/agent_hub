package com.leon.agenthub.agent.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leon.agenthub.agent.converter.AgentConverter;
import com.leon.agenthub.agent.dto.AgentConversationVO;
import com.leon.agenthub.agent.dto.AgentMessageVO;
import com.leon.agenthub.agent.dto.AgentStepEventDTO;
import com.leon.agenthub.agent.enums.AgentMessageRole;
import com.leon.agenthub.agent.model.AgentConversation;
import com.leon.agenthub.agent.model.AgentMessage;
import com.leon.agenthub.agent.repository.AgentConversationRepository;
import com.leon.agenthub.agent.repository.AgentMessageRepository;
import com.leon.agenthub.common.exception.BusinessException;
import com.leon.agenthub.permission.annotation.RequirePermission;
import com.leon.agenthub.permission.enums.PermissionAction;

import lombok.RequiredArgsConstructor;

/**
 * 智能体会话服务 — 负责会话解析、消息追加与历史查询。
 *
 * <p>会话与消息按用户隔离：跨用户访问他人会话一律按「会话不存在」处理。
 */
@Service
@RequiredArgsConstructor
public class AgentConversationService {

    /** 会话标题最大长度 */
    private static final int TITLE_MAX_LENGTH = 30;

    private final AgentConversationRepository conversationRepository;
    private final AgentMessageRepository messageRepository;
    private final AgentConverter agentConverter;

    /**
     * 解析会话 — 传入会话 ID 则校验归属，为空则新建会话。
     *
     * @param conversationId 会话 ID（可为空）
     * @param firstMessage   首条消息（用于生成会话标题）
     * @param userId         当前用户 ID
     * @return 会话实体
     */
    @Transactional(rollbackFor = Exception.class)
    public AgentConversation resolveConversation(String conversationId, String firstMessage, Long userId) {
        if (conversationId != null && !conversationId.isBlank()) {
            return requireOwnedConversation(conversationId, userId);
        }
        AgentConversation conversation = new AgentConversation();
        conversation.setId(UUID.randomUUID().toString());
        conversation.setUserId(userId);
        conversation.setTitle(truncate(firstMessage, TITLE_MAX_LENGTH));
        return conversationRepository.save(conversation);
    }

    /**
     * 追加一条消息。
     *
     * @param conversationId 会话 ID
     * @param role           消息角色
     * @param content        消息内容
     * @param steps          步骤轨迹（助手消息，可为空）
     * @return 消息 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long appendMessage(String conversationId, AgentMessageRole role, String content,
            List<AgentStepEventDTO> steps) {
        AgentMessage message = new AgentMessage();
        message.setConversationId(conversationId);
        message.setRole(role.name());
        message.setContent(content == null ? "" : content);
        message.setStepsJson(agentConverter.serializeSteps(steps));
        Long messageId = messageRepository.save(message).getId();
        // 会话更新时间随消息追加刷新，保证历史列表按最近活跃排序
        conversationRepository.findById(conversationId).ifPresent(conversationRepository::save);
        return messageId;
    }

    /**
     * 查询用户的会话列表。
     */
    @Transactional(readOnly = true)
    @RequirePermission(resource = "AGENT", action = PermissionAction.READ)
    public List<AgentConversationVO> listConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(agentConverter::toConversationVO)
                .toList();
    }

    /**
     * 查询某会话的消息列表。
     */
    @Transactional(readOnly = true)
    @RequirePermission(resource = "AGENT", action = PermissionAction.READ)
    public List<AgentMessageVO> listMessages(String conversationId, Long userId) {
        AgentConversation conversation = requireOwnedConversation(conversationId, userId);
        return messageRepository.findByConversationIdOrderByIdAsc(conversation.getId()).stream()
                .map(agentConverter::toMessageVO)
                .toList();
    }

    /**
     * 删除会话及其消息。
     */
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission(resource = "AGENT", action = PermissionAction.READ)
    public void deleteConversation(String conversationId, Long userId) {
        AgentConversation conversation = requireOwnedConversation(conversationId, userId);
        messageRepository.deleteByConversationId(conversation.getId());
        conversationRepository.delete(conversation);
    }

    /**
     * 校验会话归属 — 他人会话按「不存在」处理，不泄露会话是否存在。
     */
    private AgentConversation requireOwnedConversation(String conversationId, Long userId) {
        AgentConversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BusinessException(404, "会话不存在");
        }
        return conversation;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
