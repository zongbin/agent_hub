package com.leon.agenthub.qa.service;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leon.agenthub.common.exception.BusinessException;
import com.leon.agenthub.common.llm.ChatCitation;
import com.leon.agenthub.common.llm.ChatService;
import com.leon.agenthub.common.llm.ChatStreamResult;
import com.leon.agenthub.permission.annotation.RequirePermission;
import com.leon.agenthub.permission.enums.PermissionAction;
import com.leon.agenthub.qa.converter.QaConverter;
import com.leon.agenthub.qa.dto.ConversationVO;
import com.leon.agenthub.qa.dto.MessageVO;
import com.leon.agenthub.qa.enums.MessageRole;
import com.leon.agenthub.qa.model.ChatMemoryMessage;
import com.leon.agenthub.qa.model.Conversation;
import com.leon.agenthub.qa.repository.ChatMemoryMessageRepository;
import com.leon.agenthub.qa.repository.ConversationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 问答服务 — 编排会话、检索、多轮上下文与 LLM 流式生成。
 */
@Service
@RequiredArgsConstructor
public class QaService {

    private final ChatService chatService;
    private final ConversationRepository conversationRepository;
    private final ChatMemoryMessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final QaConverter qaConverter;

    /**
     * 执行一次问答：解析会话、检索参考资料并流式生成。
     *
     * <p>参考资料与回答内容一起返回（{@link ChatStreamResult}），不提前推送给前端。
     *
     * @param question       用户问题
     * @param conversationId 会话 ID（可为空，空则新建会话）
     * @param userId         当前用户 ID
     * @return 问答结果（含会话 ID、回答内容流与引用来源）
     */
    @RequirePermission(resource = "QA", action = PermissionAction.READ)
    public QaResult ask(String question, String conversationId, Long userId) {
        // 新增or 获取 对话信息
        Conversation conversation = resolveConversation(conversationId, question, userId);

        // 回答内容与引用来源一起返回，由前端在回答完成后统一渲染
        ChatStreamResult chat = chatService.chatStream(question, conversation.getId());
        return new QaResult(conversation.getId(), chat);
    }

    /**
     * 流式完成后补充引用来源，返回消息 ID（供反馈使用）。
     *
     * <p>回答正文已由 Spring AI 的对话记忆 Advisor 落库，这里只把引用来源补写到同一条
     * assistant 消息上，避免重复入库。
     *
     * <p>注意：本方法由 SSE 完成回调在异步线程中调用，不在 HTTP 请求线程内，
     * 此时没有登录态（SecurityContext 为空），因此<b>不能</b>标注 {@code @RequirePermission}，
     * 否则权限切面会抛「未登录」。权限已在 {@link #ask} 入口处校验过。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveAssistantMessage(String conversationId, String answer, List<ChatCitation> citations) {
        ChatMemoryMessage message = messageRepository
                .findFirstByConversationIdAndRoleOrderByIdDesc(conversationId, MessageRole.ASSISTANT.name())
                .filter(m -> answer.equals(m.getContent()))
                .orElse(null);
        if (message == null) {
            // 兜底：回答未落库时（异常中断等）补写一条完整消息
            message = new ChatMemoryMessage();
            message.setConversationId(conversationId);
            message.setRole(MessageRole.ASSISTANT.name());
            message.setContent(answer);
        }
        message.setSources(serializeSources(citations));
        Long messageId = messageRepository.save(message).getId();
        conversationRepository.findById(conversationId).ifPresent(this::touchConversation);
        return messageId;
    }

    /**
     * 提交回答反馈。
     */
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission(resource = "QA", action = PermissionAction.READ)
    public void saveFeedback(Long messageId, Boolean useful) {
        messageRepository.findById(messageId).ifPresent(m -> {
            m.setFeedback(Boolean.TRUE.equals(useful) ? 1 : 0);
            messageRepository.save(m);
        });
    }

    /**
     * 查询用户的会话列表。
     */
    @RequirePermission(resource = "QA", action = PermissionAction.READ)
    public List<ConversationVO> listConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(qaConverter::toConversationVO)
                .toList();
    }

    /**
     * 查询某会话的消息列表。
     */
    @RequirePermission(resource = "QA", action = PermissionAction.READ)
    public List<MessageVO> listMessages(String conversationId, Long userId) {
        Conversation conversation = requireOwnedConversation(conversationId, userId);
        return messageRepository.findByConversationIdOrderByIdAsc(conversation.getId()).stream()
                .map(m -> qaConverter.toMessageVO(m, deserializeSources(m.getSources())))
                .toList();
    }

    /**
     * 删除会话及其消息。
     */
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission(resource = "QA", action = PermissionAction.READ)
    public void deleteConversation(String conversationId, Long userId) {
        Conversation conversation = requireOwnedConversation(conversationId, userId);
        messageRepository.deleteByConversationId(conversation.getId());
        conversationRepository.delete(conversation);
    }

    private Conversation resolveConversation(String conversationId, String question, Long userId) {
        if (conversationId != null && !conversationId.isBlank()) {
            return requireOwnedConversation(conversationId, userId);
        }
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID().toString());
        conversation.setUserId(userId);
        conversation.setTitle(truncate(question, 30));
        return conversationRepository.save(conversation);
    }

    private void touchConversation(Conversation conversation) {
        conversationRepository.save(conversation);
    }

    private Conversation requireOwnedConversation(String conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BusinessException(404, "会话不存在");
        }
        return conversation;
    }

    private String serializeSources(List<ChatCitation> citations) {
        if (citations == null || citations.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<ChatCitation> deserializeSources(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ChatCitation>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    /**
     * 一次问答的内部结果。
     *
     * @param conversationId 会话 ID
     * @param chat           回答内容流与引用来源（一起返回）
     */
    public record QaResult(String conversationId, ChatStreamResult chat) {
    }
}
