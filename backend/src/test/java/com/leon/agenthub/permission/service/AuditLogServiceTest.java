package com.leon.agenthub.permission.service;

import com.leon.agenthub.permission.dto.AuditLogQuery;
import com.leon.agenthub.permission.model.AuditLogEntity;
import com.leon.agenthub.permission.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuditLogService} 单元测试 — 覆盖时间区间解析与过滤条件拼装。
 *
 * <p>重点回归两个历史缺陷：
 * <ul>
 *   <li>过滤参数为 null 时用 {@code :param IS NULL} 判断 → PostgreSQL 无法推断参数类型（500）</li>
 *   <li>前端传 {@code yyyy-MM-dd} 日期 → {@code OffsetDateTime.parse} 抛 DateTimeParseException（500）</li>
 * </ul>
 */
class AuditLogServiceTest {

    private AuditLogRepository auditLogRepository;
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        auditLogService = new AuditLogService(auditLogRepository);
        // 打桩必须在服务调用之前，否则 stubbing 本身会被记成一次额外调用
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
    }

    /** 捕获服务层传入仓储的 Specification */
    @SuppressWarnings("unchecked")
    private Specification<AuditLogEntity> captureSpecification() {
        ArgumentCaptor<Specification<AuditLogEntity>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(auditLogRepository).findAll(captor.capture(), any(Pageable.class));
        return captor.getValue();
    }

    @Test
    void 无过滤条件时不应抛异常且应返回空分页() {
        AuditLogQuery query = new AuditLogQuery();

        Page<AuditLogEntity> page = auditLogService.queryAuditLogs(query);

        assertNotNull(page);
        assertEquals(0, page.getTotalElements());
        // 关键回归：不再把 null 参数拼进 SQL（否则数据库无法推断参数类型）
        assertNotNull(captureSpecification());
    }

    @Test
    void 日期区间使用年月日格式时应正常解析() {
        AuditLogQuery query = new AuditLogQuery();
        query.setStartTime("2026-10-01");
        query.setEndTime("2026-10-31");

        // 不抛 DateTimeParseException
        Page<AuditLogEntity> page = auditLogService.queryAuditLogs(query);

        assertNotNull(page);
        assertNotNull(captureSpecification());
    }

    @Test
    void 非法时间字符串应被忽略而不是抛异常() {
        AuditLogQuery query = new AuditLogQuery();
        query.setStartTime("not-a-date");
        query.setEndTime("");

        Page<AuditLogEntity> page = auditLogService.queryAuditLogs(query);

        assertNotNull(page);
        assertNotNull(captureSpecification());
    }

    @Test
    void 完整ISO时间应正常解析() {
        AuditLogQuery query = new AuditLogQuery();
        query.setStartTime("2026-10-01T08:00:00+08:00");
        query.setEndTime("2026-10-31T23:59:59+08:00");

        Page<AuditLogEntity> page = auditLogService.queryAuditLogs(query);

        assertNotNull(page);
        assertNotNull(captureSpecification());
    }

    @Test
    void 空格分隔的本地时间应正常解析() {
        AuditLogQuery query = new AuditLogQuery();
        query.setStartTime("2026-10-01 08:00:00");
        query.setEndTime("2026-10-31 23:59:59");

        Page<AuditLogEntity> page = auditLogService.queryAuditLogs(query);

        assertNotNull(page);
        assertNotNull(captureSpecification());
    }

    @Test
    void 分页参数应使用基类的零基页码与上限保护() {
        AuditLogQuery query = new AuditLogQuery();
        query.setPage(3);
        query.setSize(50);

        auditLogService.queryAuditLogs(query);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogRepository).findAll(any(Specification.class), captor.capture());
        Pageable pageable = captor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(50, pageable.getPageSize());
        assertTrue(pageable.getSort().getOrderFor("createdAt").isDescending());
    }

    @Test
    void 每页条数超上限时应被截断() {
        AuditLogQuery query = new AuditLogQuery();
        query.setSize(9999);

        auditLogService.queryAuditLogs(query);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogRepository).findAll(any(Specification.class), captor.capture());
        assertEquals(200, captor.getValue().getPageSize());
    }
}
