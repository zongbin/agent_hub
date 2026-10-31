package com.leon.agenthub.agent.converter;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leon.agenthub.agent.dto.AgentConversationVO;
import com.leon.agenthub.agent.dto.AgentMessageVO;
import com.leon.agenthub.agent.dto.AgentStepEventDTO;
import com.leon.agenthub.agent.model.AgentConversation;
import com.leon.agenthub.agent.model.AgentMessage;

/**
 * 智能体转换器 — 实体 ↔ 出参对象映射。
 *
 * <p>只做数据搬运，不访问数据库、不抛业务异常。
 */
@Component
public class AgentConverter {

    private final ObjectMapper objectMapper;

    public AgentConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 会话实体 → 会话出参。
     */
    public AgentConversationVO toConversationVO(AgentConversation conversation) {
        AgentConversationVO vo = new AgentConversationVO();
        vo.setId(conversation.getId());
        vo.setTitle(conversation.getTitle());
        vo.setCreatedAt(conversation.getCreatedAt());
        vo.setUpdatedAt(conversation.getUpdatedAt());
        return vo;
    }

    /**
     * 消息实体 → 消息出参（含步骤轨迹反序列化）。
     */
    public AgentMessageVO toMessageVO(AgentMessage message) {
        AgentMessageVO vo = new AgentMessageVO();
        vo.setId(message.getId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setSteps(deserializeSteps(message.getStepsJson()));
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }

    /**
     * 步骤轨迹 → JSON 字符串。
     */
    public String serializeSteps(List<AgentStepEventDTO> steps) {
        if (steps == null || steps.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * JSON 字符串 → 步骤轨迹。
     */
    public List<AgentStepEventDTO> deserializeSteps(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<AgentStepEventDTO>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }
}
