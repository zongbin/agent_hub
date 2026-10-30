package com.leon.agenthub.qa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leon.agenthub.common.exception.BusinessException;
import com.leon.agenthub.common.llm.ChatCitation;
import com.leon.agenthub.common.llm.ChatService;
import com.leon.agenthub.common.llm.ChatStreamResult;
import com.leon.agenthub.qa.converter.QaConverter;
import com.leon.agenthub.qa.dto.ConversationVO;
import com.leon.agenthub.qa.dto.MessageVO;
import com.leon.agenthub.qa.model.ChatMemoryMessage;
import com.leon.agenthub.qa.model.Conversation;
import com.leon.agenthub.qa.repository.ChatMemoryMessageRepository;
import com.leon.agenthub.qa.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link QaService} 会话管理单元测试 — 覆盖会话创建/复用、消息持久化与删除。
 *
 * <p>会话 ID 为 UUID 字符串（qa_conversations.id / qa_messages.conversation_id 均为 VARCHAR(64)）。
 */
class QaConversationServiceTest {

    private ChatService chatService;
    private ConversationRepository conversationRepository;
    private ChatMemoryMessageRepository messageRepository;
    private QaService qaService;

    /** 模拟持久化的会话与消息 */
    private final List<Conversation> conversations = new ArrayList<>();
    private final List<ChatMemoryMessage> messages = new ArrayList<>();
    private long messageSeq = 0;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        conversationRepository = mock(ConversationRepository.class);
        messageRepository = mock(ChatMemoryMessageRepository.class);
        QaConverter qaConverter = new QaConverter();
        ReflectionTestUtils.setField(qaConverter, "snippetLength", 500);
        qaService = new QaService(chatService, conversationRepository,
                messageRepository, new ObjectMapper(), qaConverter);

        conversations.clear();
        messages.clear();
        messageSeq = 0;

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(java.util.UUID.randomUUID().toString());
            }
            c.setCreatedAt(OffsetDateTime.now());
            c.setUpdatedAt(OffsetDateTime.now());
            conversations.add(c);
            return c;
        });
        when(conversationRepository.findById(anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(0);
            return conversations.stream().filter(c -> id.equals(c.getId())).findFirst();
        });
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(anyLong())).thenAnswer(inv -> {
            long userId = inv.getArgument(0);
            return conversations.stream().filter(c -> c.getUserId() == userId).toList();
        });
        org.mockito.Mockito.doNothing().when(conversationRepository).delete(any(Conversation.class));

        when(messageRepository.save(any(ChatMemoryMessage.class))).thenAnswer(inv -> {
            ChatMemoryMessage m = inv.getArgument(0);
            if (m.getId() == null) {
                m.setId(++messageSeq);
                m.setCreatedAt(OffsetDateTime.now());
                messages.add(m);
            }
            return m;
        });
        when(messageRepository.findByConversationIdOrderByIdAsc(anyString())).thenAnswer(inv -> {
            String convId = inv.getArgument(0);
            return messages.stream().filter(m -> convId.equals(m.getConversationId())).toList();
        });
        when(messageRepository.findById(anyLong())).thenAnswer(inv -> {
            long id = inv.getArgument(0);
            return messages.stream().filter(m -> m.getId() == id).findFirst();
        });
    }

    @Test
    void firstAskCreatesConversationTitledByQuestion() {
        when(chatService.chatStream(anyString(), anyString()))
                .thenReturn(new ChatStreamResult(Flux.just("答案"), List.of()));

        QaService.QaResult result = qaService.ask("年假有几天？", null, 99L);

        assertNotNull(result.conversationId());
        assertEquals(1, conversations.size());
        assertEquals("年假有几天？", conversations.get(0).getTitle());
        assertEquals(99L, conversations.get(0).getUserId());
        assertEquals(result.conversationId(), conversations.get(0).getId());
    }

    @Test
    void askWithExistingConversationReusesIt() {
        when(chatService.chatStream(anyString(), anyString()))
                .thenReturn(new ChatStreamResult(Flux.just("答案"), List.of()));

        Conversation existing = new Conversation();
        existing.setId("conv-7");
        existing.setUserId(99L);
        existing.setTitle("已有会话");
        conversations.add(existing);

        QaService.QaResult result = qaService.ask("继续问", "conv-7", 99L);

        assertEquals("conv-7", result.conversationId());
        assertEquals(1, conversations.size());
    }

    @Test
    void askRejectsConversationOwnedByAnotherUser() {
        Conversation other = new Conversation();
        other.setId("conv-8");
        other.setUserId(1L);
        other.setTitle("别人的会话");
        conversations.add(other);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> qaService.ask("问题", "conv-8", 99L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void chatMemoryUsesConversationIdAsString() {
        when(chatService.chatStream(anyString(), anyString()))
                .thenReturn(new ChatStreamResult(Flux.just("答案"), List.of()));

        QaService.QaResult result = qaService.ask("问题", null, 99L);

        verify(chatService).chatStream("问题", result.conversationId());
    }

    @Test
    void saveAssistantMessagePersistsAnswerAndSources() {
        Conversation conv = new Conversation();
        conv.setId("conv-7");
        conv.setUserId(99L);
        conv.setTitle("会话");
        conversations.add(conv);

        Long messageId = qaService.saveAssistantMessage("conv-7", "回答内容", List.<ChatCitation>of());

        assertNotNull(messageId);
        ChatMemoryMessage saved = messages.stream()
                .filter(m -> m.getId().equals(messageId)).findFirst().orElseThrow();
        assertEquals("conv-7", saved.getConversationId());
        assertEquals("ASSISTANT", saved.getRole());
        assertEquals("回答内容", saved.getContent());
    }

    @Test
    void saveFeedbackUpdatesMessage() {
        ChatMemoryMessage m = new ChatMemoryMessage();
        m.setId(5L);
        m.setConversationId("conv-7");
        m.setRole("assistant");
        m.setContent("回答");
        messages.add(m);

        qaService.saveFeedback(5L, true);

        assertEquals(1, m.getFeedback());
        qaService.saveFeedback(5L, false);

        assertEquals(0, m.getFeedback());
    }

    @Test
    void listConversationsReturnsOnlyOwnedOnes() {
        Conversation mine = new Conversation();
        mine.setId("conv-1");
        mine.setUserId(99L);
        mine.setTitle("我的会话");
        conversations.add(mine);
        Conversation theirs = new Conversation();
        theirs.setId("conv-2");
        theirs.setUserId(1L);
        theirs.setTitle("别人的");
        conversations.add(theirs);

        List<ConversationVO> list = qaService.listConversations(99L);

        assertEquals(1, list.size());
        assertEquals("我的会话", list.get(0).getTitle());
        assertEquals("conv-1", list.get(0).getId());
    }

    @Test
    void listMessagesReturnsMessagesInOrder() {
        Conversation conv = new Conversation();
        conv.setId("conv-7");
        conv.setUserId(99L);
        conv.setTitle("会话");
        conversations.add(conv);
        for (int i = 1; i <= 3; i++) {
            ChatMemoryMessage m = new ChatMemoryMessage();
            m.setId((long) i);
            m.setConversationId("conv-7");
            m.setRole(i % 2 == 1 ? "USER" : "ASSISTANT");
            m.setContent("消息" + i);
            messages.add(m);
        }

        List<MessageVO> list = qaService.listMessages("conv-7", 99L);

        assertEquals(3, list.size());
        assertEquals("消息1", list.get(0).getContent());
        assertEquals("消息3", list.get(2).getContent());
        assertEquals("user", list.get(0).getRole());
        assertEquals("assistant", list.get(1).getRole());
    }

    @Test
    void deleteConversationRemovesMessages() {
        Conversation conv = new Conversation();
        conv.setId("conv-7");
        conv.setUserId(99L);
        conv.setTitle("会话");
        conversations.add(conv);
        ChatMemoryMessage m = new ChatMemoryMessage();
        m.setId(1L);
        m.setConversationId("conv-7");
        m.setRole("USER");
        m.setContent("消息");
        messages.add(m);

        qaService.deleteConversation("conv-7", 99L);

        verify(messageRepository).deleteByConversationId("conv-7");
        verify(conversationRepository).delete(conv);
    }

    @Test
    void deleteConversationRejectsOtherUsers() {
        Conversation other = new Conversation();
        other.setId("conv-8");
        other.setUserId(1L);
        other.setTitle("别人的");
        conversations.add(other);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> qaService.deleteConversation("conv-8", 99L));
        assertEquals(404, ex.getCode());
    }
}
