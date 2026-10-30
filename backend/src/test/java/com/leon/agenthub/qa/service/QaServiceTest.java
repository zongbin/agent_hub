package com.leon.agenthub.qa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leon.agenthub.common.llm.ChatCitation;
import com.leon.agenthub.common.llm.ChatService;
import com.leon.agenthub.common.llm.ChatStreamResult;
import com.leon.agenthub.qa.converter.QaConverter;
import com.leon.agenthub.qa.model.ChatMemoryMessage;
import com.leon.agenthub.qa.model.Conversation;
import com.leon.agenthub.qa.repository.ChatMemoryMessageRepository;
import com.leon.agenthub.qa.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link QaService} 单元测试 — 覆盖会话创建、字符串会话 ID 透传与答案流。
 */
class QaServiceTest {

    private ChatService chatService;
    private ConversationRepository conversationRepository;
    private ChatMemoryMessageRepository messageRepository;
    private QaService qaService;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        conversationRepository = mock(ConversationRepository.class);
        messageRepository = mock(ChatMemoryMessageRepository.class);
        QaConverter qaConverter = new QaConverter();
        ReflectionTestUtils.setField(qaConverter, "snippetLength", 10);
        qaService = new QaService(chatService, conversationRepository,
                messageRepository, new ObjectMapper(), qaConverter);

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId("conv-generated");
            }
            return c;
        });
        when(messageRepository.save(any(ChatMemoryMessage.class))).thenAnswer(inv -> {
            ChatMemoryMessage m = inv.getArgument(0);
            if (m.getId() == null) {
                m.setId(100L);
            }
            return m;
        });
    }

    @Test
    void askCreatesConversationAndStreamsAnswer() {
        when(chatService.chatStream(anyString(), anyString()))
                .thenReturn(new ChatStreamResult(Flux.just("答案"), List.of()));

        QaService.QaResult result = qaService.ask("年假几天", null, 99L);

        assertNotNull(result.conversationId());
        assertTrue(result.conversationId().length() <= 64, "会话 ID 需符合 VARCHAR(64) 长度限制");
        List<String> output = result.chat().content().collectList().block();
        assertTrue(output != null && output.size() == 1 && output.get(0).contains("答案"));
    }

    @Test
    void askReturnsCitationsTogetherWithAnswer() {
        ChatCitation citation = new ChatCitation(12L, "员工手册", 2, "片段", "/knowledge/12");
        when(chatService.chatStream(anyString(), anyString()))
                .thenReturn(new ChatStreamResult(Flux.just("答案"), List.of(citation)));

        QaService.QaResult result = qaService.ask("年假几天", null, 99L);

        assertEquals(1, result.chat().citations().size());
        assertEquals(12L, result.chat().citations().get(0).documentId());
        assertEquals("/knowledge/12", result.chat().citations().get(0).documentUrl());
    }

    @Test
    void chatMemoryKeyIsConversationId() {
        when(chatService.chatStream(anyString(), anyString()))
                .thenReturn(new ChatStreamResult(Flux.just("答案"), List.of()));

        QaService.QaResult result = qaService.ask("问题", null, 99L);

        verify(chatService).chatStream("问题", result.conversationId());
    }
}
