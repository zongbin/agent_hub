package com.leon.agenthub.permission.repository;

import com.leon.agenthub.permission.model.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

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
}
