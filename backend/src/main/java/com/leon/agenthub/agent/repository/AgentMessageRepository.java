package com.leon.agenthub.agent.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * 统计指定角色消息的累计数量 — 工作台智能体任务数。
     *
     * <p>{@code agent_messages} 不带用户列，需按会话归属用户过滤。
     *
     * @param role   消息角色（取值见 {@link com.leon.agenthub.agent.enums.AgentMessageRole}）
     * @param userId 用户 ID
     * @return 消息条数
     */
    @Query(value = "SELECT COUNT(*) FROM agent_messages m " +
           "JOIN agent_conversations c ON c.id = m.conversation_id " +
           "WHERE m.role = :role AND c.user_id = :userId",
           nativeQuery = true)
    long countRoleByUser(@Param("role") String role, @Param("userId") Long userId);

    /**
     * 统计指定角色在指定时间之后的消息数量 — 工作台今日智能体任务数。
     *
     * @param role   消息角色（取值见 {@link com.leon.agenthub.agent.enums.AgentMessageRole}）
     * @param userId 用户 ID
     * @param after  起始时间（不含）
     * @return 消息条数
     */
    @Query(value = "SELECT COUNT(*) FROM agent_messages m " +
           "JOIN agent_conversations c ON c.id = m.conversation_id " +
           "WHERE m.role = :role AND c.user_id = :userId AND m.created_at >= :after",
           nativeQuery = true)
    long countRoleByUserSince(@Param("role") String role,
                               @Param("userId") Long userId,
                               @Param("after") OffsetDateTime after);

    /**
     * 按天统计指定角色的消息数量 — 工作台近 7 日趋势。
     *
     * @param role   消息角色（取值见 {@link com.leon.agenthub.agent.enums.AgentMessageRole}）
     * @param userId 用户 ID
     * @param since  起始时间（含）
     * @param zone   统计时区（ID，如 {@code Asia/Shanghai}），保证「日」的分界与应用一致
     * @return 每行为 {@code [yyyy-MM-dd, count]}
     */
    @Query(value = "SELECT to_char(m.created_at AT TIME ZONE :zone, 'YYYY-MM-DD'), COUNT(*) FROM agent_messages m " +
           "JOIN agent_conversations c ON c.id = m.conversation_id " +
           "WHERE m.role = :role AND c.user_id = :userId AND m.created_at >= :since " +
           "GROUP BY 1 ORDER BY 1",
           nativeQuery = true)
    List<Object[]> countRoleByUserGroupByDay(@Param("role") String role,
                                              @Param("userId") Long userId,
                                              @Param("since") OffsetDateTime since,
                                              @Param("zone") String zone);
}
