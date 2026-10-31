package com.leon.agenthub.agent.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * 统计用户的会话总数。
     */
    long countByUserId(Long userId);

    /**
     * 查询用户最近的若干个会话 — 工作台「最近会话」列表。
     *
     * @param userId   用户 ID
     * @param pageable 分页参数（取前 N 条即可）
     * @return 按最近更新倒序的会话列表
     */
    @Query("SELECT c FROM AgentConversation c WHERE c.userId = :userId ORDER BY c.updatedAt DESC")
    List<AgentConversation> findRecentConversations(@Param("userId") Long userId, Pageable pageable);
}
