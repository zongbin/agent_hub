package com.leon.agenthub.qa.repository;

import com.leon.agenthub.qa.model.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 问答会话数据访问接口。
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {

    /**
     * 查询用户的全部分会话，按最近更新倒序。
     */
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId);

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
    @Query("SELECT c FROM Conversation c WHERE c.userId = :userId ORDER BY c.updatedAt DESC")
    List<Conversation> findRecentConversations(@Param("userId") Long userId, Pageable pageable);
}
