package com.leon.agenthub.qa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.leon.agenthub.qa.model.ChatMemoryMessage;

/**
 * 问答消息数据访问接口。
 */
@Repository
public interface ChatMemoryMessageRepository extends JpaRepository<ChatMemoryMessage, Long> {

    /**
     * 查询某会话的消息，按时间倒序（用于取最近 N 条）。
     */
    List<ChatMemoryMessage> findByConversationIdOrderByIdDesc(Long conversationId);

    /**
     * 查询某会话的消息，按时间升序（用于展示）。
     */
    List<ChatMemoryMessage> findByConversationIdOrderByIdAsc(Long conversationId);

    /**
     * 删除某会话的全部消息。
     */
    void deleteByConversationId(Long conversationId);
}
