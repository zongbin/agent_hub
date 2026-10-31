package com.leon.agenthub.permission.repository;

import com.leon.agenthub.permission.model.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

/**
 * 审计日志数据访问接口 — 查询和管理 audit_logs 表。
 *
 * <p>条件查询使用 {@link JpaSpecificationExecutor} 的 Specification 机制：
 * 过滤条件由服务层按需拼装，未命中的条件根本不进入 SQL，
 * 避免「参数为 null 时用 {@code :param IS NULL} 判断」导致数据库无法推断参数类型
 * （{@code could not determine data type of parameter}）。
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long>,
        JpaSpecificationExecutor<AuditLogEntity> {

    /**
     * 统计指定用户的累计操作数 — 工作台活跃度统计。
     *
     * @param userId 用户 ID
     * @return 操作条数
     */
    long countByUserId(Long userId);

    /**
     * 统计指定用户在指定时间之后的操作数 — 工作台今日 / 近 7 日活跃度统计。
     *
     * @param userId 用户 ID
     * @param after  起始时间（不含）
     * @return 操作条数
     */
    long countByUserIdAndCreatedAtAfter(Long userId, OffsetDateTime after);
}
