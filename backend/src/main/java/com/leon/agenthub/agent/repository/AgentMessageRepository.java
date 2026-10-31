package com.leon.agenthub.agent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.leon.agenthub.agent.model.AgentMessage;

/**
 * 智能体消息数据访问接口。
 */
@Repository
public interface AgentMessageRepository extends JpaRepository<AgentMessage, Long> {

    /**
     * 查询某会话的消息，按发生顺序升序（用于展示）。
     */
    List<AgentMessage> findByConversationIdOrderByIdAsc(String conversationId);

    /**
     * 删除某会话的全部消息。
     */
    void deleteByConversationId(String conversationId);
}
