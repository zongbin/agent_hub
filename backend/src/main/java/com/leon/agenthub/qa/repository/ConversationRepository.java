package com.leon.agenthub.qa.repository;

import com.leon.agenthub.qa.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
