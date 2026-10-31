package com.leon.agenthub.qa.converter;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import com.leon.agenthub.common.llm.ChatCitation;
import com.leon.agenthub.qa.dto.ConversationVO;
import com.leon.agenthub.qa.dto.MessageVO;
import com.leon.agenthub.qa.model.ChatMemoryMessage;
import com.leon.agenthub.qa.model.Conversation;

/**
 * 问答转换器 — 实体/检索结果 ↔ 出参对象映射。
 */
@Component
public class QaConverter {

    /**
     * 检索分块 → 引用来源。
     */
    public ChatCitation toCitation(Document doc) {
        Long documentId = longMetadata(doc, "documentId");
        Integer chunkIndex = intMetadata(doc, "chunkIndex");
        Object titleMeta = doc.getMetadata() == null ? null : doc.getMetadata().get("title");
        String title = titleMeta == null ? "" : titleMeta.toString();
        return new ChatCitation(documentId, title, chunkIndex);
    }

    /**
     * 批量转换检索分块。
     */
    public List<ChatCitation> toCitations(List<Document> documents) {
        return documents.stream().map(this::toCitation).toList();
    }

    /**
     * 会话实体 → 会话出参。
     */
    public ConversationVO toConversationVO(Conversation conversation) {
        ConversationVO vo = new ConversationVO();
        vo.setId(conversation.getId());
        vo.setTitle(conversation.getTitle());
        vo.setCreatedAt(conversation.getCreatedAt());
        vo.setUpdatedAt(conversation.getUpdatedAt());
        return vo;
    }

    /**
     * 消息实体 → 消息出参。
     */
    public MessageVO toMessageVO(ChatMemoryMessage message, List<ChatCitation> sources) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setRole(message.getRole() == null ? null : message.getRole().toLowerCase());
        vo.setContent(message.getContent());
        vo.setSources(sources);
        vo.setFeedback(message.getFeedback());
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }

    private Long longMetadata(Document doc, String key) {
        Object value = doc.getMetadata() == null ? null : doc.getMetadata().get(key);
        return value instanceof Number number ? number.longValue() : null;
    }

    private Integer intMetadata(Document doc, String key) {
        Object value = doc.getMetadata() == null ? null : doc.getMetadata().get(key);
        return value instanceof Number number ? number.intValue() : null;
    }
}
