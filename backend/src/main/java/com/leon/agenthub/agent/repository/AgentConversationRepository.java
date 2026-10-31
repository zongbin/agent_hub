package com.leon.agenthub.agent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.leon.agenthub.agent.model.AgentConversation;

/**
 * 智能体会话数据访问接口。
 */
@Repository
public interface AgentConversationRepository extends JpaRepository<AgentConversation, String> {

    /**
     * 查询用户的全部会话，按最近更新倒序。
     */
    List<AgentConversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
