package com.leon.agenthub.permission.repository;

import com.leon.agenthub.permission.model.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

/**
 * 审计日志数据访问接口 — 查询和管理 audit_logs 表。
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    /**
     * 按条件查询审计日志（真正的数据库级分页）。
     *
     * @param userId    用户 ID（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param pageable  分页参数
     * @return 分页审计日志
     */
    @Query("SELECT a FROM AuditLogEntity a WHERE " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:startTime IS NULL OR a.createdAt >= :startTime) AND " +
           "(:endTime IS NULL OR a.createdAt <= :endTime) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLogEntity> findByCondition(@Param("userId") Long userId,
                                          @Param("startTime") OffsetDateTime startTime,
                                          @Param("endTime") OffsetDateTime endTime,
                                          Pageable pageable);
}
