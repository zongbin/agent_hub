package com.leon.agenthub.qa.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.leon.agenthub.qa.model.ChatMemoryMessage;
import com.leon.agenthub.qa.repository.ChatMemoryMessageRepository;

import lombok.AllArgsConstructor;

/**
 * 基于 PostgreSQL对话记忆实现
 * 消息按 id 自增顺序存取，保证同一会话内消息顺序稳定
 */
@Component
@AllArgsConstructor
public class DatabaseChatMemory implements ChatMemory {

    private final ChatMemoryMessageRepository chatMemoryMessageRepository;

    @Override
    public void add(String conversationId, List<Message> messages) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        Assert.notNull(messages, "messages cannot be null");
        if (messages.isEmpty()) {
            return;
        }
        List<ChatMemoryMessage> entities = messages.stream()
                .map(message -> {
                    ChatMemoryMessage entity = new ChatMemoryMessage();
                    entity.setConversationId(conversationId);
                    entity.setRole(message.getMessageType().name());
                    entity.setContent(message.getText());
                    entity.setSources(null);
                    return entity;
                })
                .toList();
        for (ChatMemoryMessage chatMemoryMessage : entities) {
            chatMemoryMessageRepository.save(chatMemoryMessage);
        }
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        Assert.isTrue(lastN > 0, "lastN must be greater than 0");
        // 先取最近 lastN 条（倒序），再反转回正序，保证对话顺序

        List<ChatMemoryMessage> desc = chatMemoryMessageRepository
                .findByConversationIdOrderByIdDesc(conversationId);

        if (desc.isEmpty()) {
            return List.of();
        }
        int end = Math.min(lastN, desc.size());
        List<ChatMemoryMessage> recent = new ArrayList<>(desc.subList(0, end));
        return recent.reversed().stream()
                .map(e -> toMessage(MessageType.valueOf(e.getRole().toUpperCase()), e.getContent()))
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        chatMemoryMessageRepository.deleteByConversationId(conversationId);
    }

    private Message toMessage(MessageType type, String content) {
        return switch (type) {
            case USER -> new UserMessage(content);
            case ASSISTANT -> new AssistantMessage(content);
            case SYSTEM -> new SystemMessage(content);
            default -> throw new IllegalArgumentException("Unsupported message type: " + type);
        };
    }
}
