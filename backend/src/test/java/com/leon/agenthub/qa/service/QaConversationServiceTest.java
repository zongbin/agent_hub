package com.leon.agenthub.qa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leon.agenthub.common.exception.BusinessException;
import com.leon.agenthub.common.llm.ChatService;
import com.leon.agenthub.qa.dto.ConversationVO;
import com.leon.agenthub.qa.dto.MessageVO;
import com.leon.agenthub.qa.model.Conversation;
import com.leon.agenthub.qa.model.Message;
import com.leon.agenthub.qa.repository.ConversationRepository;
import com.leon.agenthub.qa.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * {@link QaService} 会话管理单元测试 — 覆盖会话创建/复用、多轮上下文、消息持久化与删除。
 *
 * <p>全部使用 mock，不触发外部 LLM 调用。
 */
class QaConversationServiceTest {

    private KnowledgeRetriever knowledgeRetriever;
    private ChatService chatService;
    private ConversationRepository conversationRepository;
    private MessageRepository messageRepository;
    private QaService qaService;

    /** 模拟持久化的会话与消息 */
    private final List<Conversation> conversations = new ArrayList<>();
    private final List<Message> messages = new ArrayList<>();
    private long conversationSeq = 0;
    private long messageSeq = 0;

    @BeforeEach
    void setUp() {
        knowledgeRetriever = mock(KnowledgeRetriever.class);
        chatService = mock(ChatService.class);
        conversationRepository = mock(ConversationRepository.class);
        messageRepository = mock(MessageRepository.class);
        qaService = new QaService(knowledgeRetriever, chatService, conversationRepository,
                messageRepository, new ObjectMapper());
        ReflectionTestUtils.setField(qaService, "snippetLength", 500);
        ReflectionTestUtils.setField(qaService, "historySize", 6);
        ReflectionTestUtils.setField(qaService, "historyMsgLength", 200);

        conversations.clear();
        messages.clear();
        conversationSeq = 0;
        messageSeq = 0;

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(++conversationSeq);
                c.setCreatedAt(OffsetDateTime.now());
                c.setUpdatedAt(OffsetDateTime.now());
                conversations.add(c);
            }
            return c;
        });
        when(conversationRepository.findById(anyLong())).thenAnswer(inv -> {
            long id = inv.getArgument(0);
            return conversations.stream().filter(c -> c.getId() == id).findFirst();
        });
        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(anyLong())).thenAnswer(inv -> {
            long userId = inv.getArgument(0);
            return conversations.stream().filter(c -> c.getUserId() == userId).toList();
        });
        org.mockito.Mockito.doNothing().when(conversationRepository).delete(any(Conversation.class));

        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            if (m.getId() == null) {
                m.setId(++messageSeq);
                m.setCreatedAt(OffsetDateTime.now());
                messages.add(m);
            }
            return m;
        });
        when(messageRepository.findByConversationIdOrderByIdDesc(anyLong())).thenAnswer(inv -> {
            long convId = inv.getArgument(0);
            List<Message> list = messages.stream().filter(m -> m.getConversationId() == convId).toList();
            List<Message> desc = new ArrayList<>(list);
            java.util.Collections.reverse(desc);
            return desc;
        });
        when(messageRepository.findByConversationIdOrderByIdAsc(anyLong())).thenAnswer(inv -> {
            long convId = inv.getArgument(0);
            return messages.stream().filter(m -> m.getConversationId() == convId).toList();
        });
        when(messageRepository.findById(anyLong())).thenAnswer(inv -> {
            long id = inv.getArgument(0);
            return messages.stream().filter(m -> m.getId() == id).findFirst();
        });
    }

    @Test
    void firstAskCreatesConversationTitledByQuestion() {
        when(knowledgeRetriever.retrieve(anyString(), anyLong(), anyString())).thenReturn(List.of(hit()));
        when(chatService.chatStream(anyString())).thenReturn(Flux.just("答案"));

        QaService.QaResult result = qaService.ask("年假有几天？", null, 99L, "VIEWER");

        assertNotNull(result.conversationId());
        assertEquals(1, conversations.size());
        assertEquals("年假有几天？", conversations.get(0).getTitle());
        assertEquals(99L, conversations.get(0).getUserId());
    }

    @Test
    void askWithExistingConversationReusesIt() {
        when(knowledgeRetriever.retrieve(anyString(), anyLong(), anyString())).thenReturn(List.of(hit()));
        when(chatService.chatStream(anyString())).thenReturn(Flux.just("答案"));

        Conversation existing = new Conversation();
        existing.setId(7L);
        existing.setUserId(99L);
        existing.setTitle("已有会话");
        conversations.add(existing);

        QaService.QaResult result = qaService.ask("继续问", 7L, 99L, "VIEWER");

        assertEquals(7L, result.conversationId());
        assertEquals(1, conversations.size());
    }

    @Test
    void askRejectsConversationOwnedByAnotherUser() {
        Conversation other = new Conversation();
        other.setId(8L);
        other.setUserId(1L);
        other.setTitle("别人的会话");
        conversations.add(other);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> qaService.ask("问题", 8L, 99L, "VIEWER"));
        assertEquals(404, ex.getCode());
    }

    @Test
    void promptContainsPreviousTurnsAsHistory() {
        when(knowledgeRetriever.retrieve(anyString(), anyLong(), anyString())).thenReturn(List.of(hit()));
        when(chatService.chatStream(anyString())).thenReturn(Flux.just("答案"));

        Conversation existing = new Conversation();
        existing.setId(7L);
        existing.setUserId(99L);
        existing.setTitle("已有会话");
        conversations.add(existing);
        // 预置一轮历史
        Message prevUser = new Message();
        prevUser.setId(1L);
        prevUser.setConversationId(7L);
        prevUser.setRole("user");
        prevUser.setContent("上一轮问的是什么");
        messages.add(prevUser);
        Message prevAssistant = new Message();
        prevAssistant.setId(2L);
        prevAssistant.setConversationId(7L);
        prevAssistant.setRole("assistant");
        prevAssistant.setContent("上一轮的回答");
        messages.add(prevAssistant);

        qaService.ask("那它呢？", 7L, 99L, "VIEWER");

        verify(chatService).chatStream(org.mockito.ArgumentMatchers.argThat(prompt ->
                prompt.contains("历史对话")
                        && prompt.contains("上一轮问的是什么")
                        && prompt.contains("上一轮的回答")
                        && prompt.contains("用户：")));
    }

    @Test
    void saveAssistantMessagePersistsAnswerAndSources() {
        Conversation conv = new Conversation();
        conv.setId(7L);
        conv.setUserId(99L);
        conv.setTitle("会话");
        conversations.add(conv);

        Long messageId = qaService.saveAssistantMessage(7L, "回答内容", List.of());

        assertNotNull(messageId);
        Message saved = messages.stream().filter(m -> m.getId() == messageId).findFirst().orElseThrow();
        assertEquals("assistant", saved.getRole());
        assertEquals("回答内容", saved.getContent());
    }

    @Test
    void saveFeedbackUpdatesMessage() {
        Message m = new Message();
        m.setId(5L);
        m.setConversationId(7L);
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
        mine.setId(1L);
        mine.setUserId(99L);
        mine.setTitle("我的会话");
        conversations.add(mine);
        Conversation theirs = new Conversation();
        theirs.setId(2L);
        theirs.setUserId(1L);
        theirs.setTitle("别人的");
        conversations.add(theirs);

        List<ConversationVO> list = qaService.listConversations(99L);

        assertEquals(1, list.size());
        assertEquals("我的会话", list.get(0).getTitle());
    }

    @Test
    void listMessagesReturnsMessagesInOrder() {
        Conversation conv = new Conversation();
        conv.setId(7L);
        conv.setUserId(99L);
        conv.setTitle("会话");
        conversations.add(conv);
        for (int i = 1; i <= 3; i++) {
            Message m = new Message();
            m.setId((long) i);
            m.setConversationId(7L);
            m.setRole(i % 2 == 1 ? "user" : "assistant");
            m.setContent("消息" + i);
            messages.add(m);
        }

        List<MessageVO> list = qaService.listMessages(7L, 99L);

        assertEquals(3, list.size());
        assertEquals("消息1", list.get(0).getContent());
        assertEquals("消息3", list.get(2).getContent());
    }

    @Test
    void deleteConversationRemovesMessages() {
        Conversation conv = new Conversation();
        conv.setId(7L);
        conv.setUserId(99L);
        conv.setTitle("会话");
        conversations.add(conv);
        Message m = new Message();
        m.setId(1L);
        m.setConversationId(7L);
        m.setRole("user");
        m.setContent("消息");
        messages.add(m);

        qaService.deleteConversation(7L, 99L);

        verify(messageRepository).deleteByConversationId(7L);
        verify(conversationRepository).delete(conv);
    }

    @Test
    void deleteConversationRejectsOtherUsers() {
        Conversation other = new Conversation();
        other.setId(8L);
        other.setUserId(1L);
        other.setTitle("别人的");
        conversations.add(other);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> qaService.deleteConversation(8L, 99L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void emptyRetrievalStillPersistsUserQuestion() {
        when(knowledgeRetriever.retrieve(anyString(), anyLong(), anyString())).thenReturn(List.of());

        qaService.ask("没有答案的问题", null, 99L, "VIEWER");

        assertEquals(1, messages.size());
        assertEquals("user", messages.get(0).getRole());
        assertTrue(messages.get(0).getContent().contains("没有答案"));
    }

    private Document hit() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("documentId", 1L);
        metadata.put("title", "制度");
        metadata.put("chunkIndex", 0);
        return Document.builder().id("x").text("分块内容").metadata(metadata).build();
    }
}
