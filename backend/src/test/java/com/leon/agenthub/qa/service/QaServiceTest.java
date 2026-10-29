package com.leon.agenthub.qa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leon.agenthub.common.llm.ChatService;
import com.leon.agenthub.qa.dto.QaSource;
import com.leon.agenthub.qa.model.Conversation;
import com.leon.agenthub.qa.model.ChatMemoryMessage;
import com.leon.agenthub.qa.repository.ConversationRepository;
import com.leon.agenthub.qa.repository.ChatMemoryMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link QaService} 单元测试 — 覆盖引用映射、片段截断、空结果兜底与 prompt 组装。
 */
class QaServiceTest {

    private KnowledgeRetriever knowledgeRetriever;
    private ChatService chatService;
    private ConversationRepository conversationRepository;
    private ChatMemoryMessageRepository messageRepository;
    private QaService qaService;

    @BeforeEach
    void setUp() {
        knowledgeRetriever = mock(KnowledgeRetriever.class);
        chatService = mock(ChatService.class);
        conversationRepository = mock(ConversationRepository.class);
        messageRepository = mock(ChatMemoryMessageRepository.class);
        qaService = new QaService(knowledgeRetriever, chatService, conversationRepository,
                messageRepository, new ObjectMapper());
        ReflectionTestUtils.setField(qaService, "snippetLength", 10);

        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(1L);
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
        when(messageRepository.findByConversationIdOrderByIdDesc(anyLong())).thenReturn(List.of());
    }

    @Test
    void mapsHitsToSourcesAndTruncatesSnippet() {
        Document hit = hit(7L, "考勤制度", "这是一段超过十个字符的分块内容", 3);
        when(knowledgeRetriever.retrieve("年假几天", 99L, "VIEWER")).thenReturn(List.of(hit));
        when(chatService.chatStream(anyString())).thenReturn(Flux.just("答案"));

        QaService.QaResult result = qaService.ask("年假几天", null, 99L, "VIEWER");

        assertEquals(1, result.sources().size());
        QaSource source = result.sources().get(0);
        assertEquals(7L, source.getDocumentId());
        assertEquals("考勤制度", source.getTitle());
        assertEquals(3, source.getChunkIndex());
        assertEquals("这是一段超过十个字符".substring(0, 10), source.getSnippet());
        assertNotNull(result.conversationId());
    }

    @Test
    void promptContainsSourcesAndCitationInstruction() {
        Document hit = hit(1L, "制度", "分块内容", 0);
        when(knowledgeRetriever.retrieve(anyString(), anyLong(), anyString())).thenReturn(List.of(hit));
        when(chatService.chatStream(anyString())).thenReturn(Flux.just("答案"));

        qaService.ask("问题", null, 99L, "VIEWER");

        verify(chatService).chatStream(org.mockito.ArgumentMatchers.argThat(prompt ->
                prompt.contains("《制度》") && prompt.contains("[1]") && prompt.contains("问题：问题")));
    }

    @Test
    void emptyRetrievalReturnsFallbackWithoutCallingLlm() {
        when(knowledgeRetriever.retrieve(anyString(), anyLong(), anyString())).thenReturn(List.of());

        QaService.QaResult result = qaService.ask("无答案的问题", null, 99L, "VIEWER");

        assertTrue(result.sources().isEmpty());
        List<String> output = result.stream().collectList().block();
        assertTrue(output != null && output.size() == 1 && output.get(0).contains("未找到相关内容"));
        org.mockito.Mockito.verifyNoInteractions(chatService);
    }

    private Document hit(Long documentId, String title, String content, Integer chunkIndex) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("documentId", documentId);
        metadata.put("title", title);
        metadata.put("chunkIndex", chunkIndex);
        return Document.builder().id("test-" + documentId).text(content).metadata(metadata).build();
    }
}
