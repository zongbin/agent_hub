package com.leon.agenthub.permission.service;

import com.leon.agenthub.permission.dto.AuditLogQuery;
import com.leon.agenthub.permission.model.AuditLogEntity;
import com.leon.agenthub.permission.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 审计日志服务 — 负责审计日志的条件查询与分页。
 *
 * <p>过滤条件按需拼装（Specification），未指定的条件不会进入 SQL，
 * 规避了「{@code :param IS NULL} 判断 + null 参数」导致 PostgreSQL 无法推断参数类型的问题。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * 分页查询审计日志。
     *
     * @param query 查询条件（含分页参数）
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    public Page<AuditLogEntity> queryAuditLogs(AuditLogQuery query) {
        OffsetDateTime startTime = parseStartTime(query.getStartTime());
        OffsetDateTime endTime = parseEndTime(query.getEndTime());
        String actionType = trimToNull(query.getActionType());

        Specification<AuditLogEntity> spec = (root, criteriaQuery, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query.getUserId() != null) {
                predicates.add(builder.equal(root.get("userId"), query.getUserId()));
            }
            if (startTime != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), startTime));
            }
            if (endTime != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), endTime));
            }
            if (actionType != null) {
                predicates.add(builder.equal(root.get("action"), actionType));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };

        return auditLogRepository.findAll(
                spec,
                query.toPageRequest(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    /**
     * 解析起始时间。
     *
     * <p>支持 {@code yyyy-MM-dd}、{@code yyyy-MM-dd HH:mm:ss}、{@code yyyy-MM-ddTHH:mm:ss}
     * 以及带时区偏移的完整 ISO-8601。日期（无时分秒）按当天 00:00:00 处理。
     */
    private OffsetDateTime parseStartTime(String value) {
        OffsetDateTime parsed = parseDateTime(value);
        if (parsed == null) {
            return null;
        }
        // 纯日期 → 当天零点
        if (value != null && value.trim().length() <= 10) {
            return parsed.with(LocalTime.MIN);
        }
        return parsed;
    }

    /**
     * 解析结束时间。
     *
     * <p>与起始时间相同的格式支持；纯日期按当天 23:59:59.999999999 处理，
     * 保证「选择 10-01 ~ 10-31」包含 10-31 当天的全部记录。
     */
    private OffsetDateTime parseEndTime(String value) {
        OffsetDateTime parsed = parseDateTime(value);
        if (parsed == null) {
            return null;
        }
        if (value != null && value.trim().length() <= 10) {
            return parsed.with(LocalTime.MAX);
        }
        return parsed;
    }

    /**
     * 宽松解析时间字符串，解析失败时记录告警并返回 null（该条件不参与过滤）。
     */
    private OffsetDateTime parseDateTime(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            // 带时区偏移的完整 ISO-8601（2026-10-01T00:00:00+08:00 / ...Z）
            return OffsetDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
            // 继续尝试其他格式
        }
        try {
            // 本地日期时间，T 或空格分隔（2026-10-01T00:00:00 / 2026-10-01 00:00:00）
            String normalized = text.replace(' ', 'T');
            return LocalDateTime.parse(normalized).atZone(ZoneId.systemDefault()).toOffsetDateTime();
        } catch (DateTimeParseException ignored) {
            // 继续尝试其他格式
        }
        try {
            // 纯日期（2026-10-01）
            return LocalDate.parse(text).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        } catch (DateTimeParseException e) {
            log.warn("无法解析审计日志时间参数: '{}', 已忽略该过滤条件", text);
            return null;
        }
    }

    /**
     * 去除首尾空白，空串视为 null。
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
