package com.leon.agenthub.qa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leon.agenthub.common.exception.BusinessException;
import com.leon.agenthub.common.llm.ChatService;
import com.leon.agenthub.qa.dto.ConversationVO;
import com.leon.agenthub.qa.dto.MessageVO;
import com.leon.agenthub.qa.dto.QaSource;
import com.leon.agenthub.qa.model.Conversation;
import com.leon.agenthub.qa.model.ChatMemoryMessage;
import com.leon.agenthub.qa.repository.ConversationRepository;
import com.leon.agenthub.qa.repository.ChatMemoryMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 问答服务 — 编排会话、检索、多轮上下文与 LLM 流式生成。
 */
@Service
@RequiredArgsConstructor
public class QaService {

    private final KnowledgeRetriever knowledgeRetriever;
    private final ChatService chatService;
    private final ConversationRepository conversationRepository;
    private final ChatMemoryMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    @Value("${agenthub.qa.snippet-length:500}")
    private int snippetLength;

    @Value("${agenthub.qa.history-size:6}")
    private int historySize;

    @Value("${agenthub.qa.history-msg-length:200}")
    private int historyMsgLength;

    /**
     * 执行一次问答：解析会话、保存提问、加载历史、检索并流式生成。
     *
     * @param question       用户问题
     * @param conversationId 会话 ID（可为空，空则新建会话）
     * @param userId         当前用户 ID
     * @param userRole       当前用户角色代码
     * @return 问答结果（含会话 ID、引用来源与答案流）
     */
    public QaResult ask(String question, Long conversationId, Long userId, String userRole) {
        // 新增or 获取 对话信息
        Conversation conversation = resolveConversation(conversationId, question, userId);

        // List<QaSource> sources = hits.stream().map(this::toSource).toList();

        List<QaSource> sources = new ArrayList<>();

        Flux<String> stream = chatService.chatStream(question, conversation.getId().toString());
        return new QaResult(conversation.getId(), sources, stream);
    }

    /**
     * 流式完成后保存回答消息，返回消息 ID（供反馈使用）。
     */
    public Long saveAssistantMessage(Long conversationId, String answer, List<QaSource> sources) {
        Long messageId = saveMessage(conversationId, "assistant", answer, serializeSources(sources));
        conversationRepository.findById(conversationId).ifPresent(this::touchConversation);
        return messageId;
    }

    /**
     * 提交回答反馈。
     */
    public void saveFeedback(Long messageId, Boolean useful) {
        messageRepository.findById(messageId).ifPresent(m -> {
            m.setFeedback(Boolean.TRUE.equals(useful) ? 1 : 0);
            messageRepository.save(m);
        });
    }

    /**
     * 查询用户的会话列表。
     */
    public List<ConversationVO> listConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toConversationVO)
                .toList();
    }

    /**
     * 查询某会话的消息列表。
     */
    public List<MessageVO> listMessages(Long conversationId, Long userId) {
        Conversation conversation = requireOwnedConversation(conversationId, userId);
        return messageRepository.findByConversationIdOrderByIdAsc(conversation.getId()).stream()
                .map(this::toMessageVO)
                .toList();
    }

    /**
     * 删除会话及其消息。
     */
    public void deleteConversation(Long conversationId, Long userId) {
        Conversation conversation = requireOwnedConversation(conversationId, userId);
        messageRepository.deleteByConversationId(conversation.getId());
        conversationRepository.delete(conversation);
    }

    private Conversation resolveConversation(Long conversationId, String question, Long userId) {
        if (conversationId != null) {
            return requireOwnedConversation(conversationId, userId);
        }
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(truncate(question, 30));
        return conversationRepository.save(conversation);
    }

    private void touchConversation(Conversation conversation) {
        conversationRepository.save(conversation);
    }

    private Conversation requireOwnedConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BusinessException(404, "会话不存在");
        }
        return conversation;
    }

    private Long saveMessage(Long conversationId, String role, String content, String sources) {
        ChatMemoryMessage message = new ChatMemoryMessage();
        message.setConversationId(conversationId);
        message.setRole(role.toUpperCase());
        message.setContent(content);
        message.setSources(sources);
        return messageRepository.save(message).getId();
    }

    private QaSource toSource(Document doc) {
        QaSource source = new QaSource();
        source.setDocumentId(documentIdOf(doc));
        source.setTitle(titleOf(doc));
        source.setChunkIndex(chunkIndexOf(doc));
        String content = doc.getText() == null ? "" : doc.getText();
        source.setSnippet(truncate(content, snippetLength));
        return source;
    }

    private ConversationVO toConversationVO(Conversation conversation) {
        ConversationVO vo = new ConversationVO();
        vo.setId(conversation.getId());
        vo.setTitle(conversation.getTitle());
        vo.setCreatedAt(conversation.getCreatedAt());
        vo.setUpdatedAt(conversation.getUpdatedAt());
        return vo;
    }

    private MessageVO toMessageVO(ChatMemoryMessage message) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setSources(deserializeSources(message.getSources()));
        vo.setFeedback(message.getFeedback());
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }

    private String serializeSources(List<QaSource> sources) {
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<QaSource> deserializeSources(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<QaSource>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private Long documentIdOf(Document doc) {
        Object value = doc.getMetadata().get("documentId");
        return value instanceof Number number ? number.longValue() : null;
    }

    private Integer chunkIndexOf(Document doc) {
        Object value = doc.getMetadata().get("chunkIndex");
        return value instanceof Number number ? number.intValue() : null;
    }

    private String titleOf(Document doc) {
        Object value = doc.getMetadata().get("title");
        return value == null ? "" : value.toString();
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
     * @param sources        引用来源
     * @param stream         答案文本流
     */
    public record QaResult(Long conversationId, List<QaSource> sources, Flux<String> stream) {
    }
}
