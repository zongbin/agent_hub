package com.leon.agenthub.permission.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leon.agenthub.permission.model.AuditLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 审计日志 Mapper — 查询和管理 audit_logs 表。
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {

    /**
     * 按条件查询审计日志（分页由上层 PageHelper 控制）。
     *
     * @param userId    用户 ID（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 审计日志列表
     */
    default List<AuditLogEntity> selectByCondition(
            @Param("userId") Long userId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime) {
        LambdaQueryWrapper<AuditLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, AuditLogEntity::getUserId, userId)
                .ge(startTime != null, AuditLogEntity::getCreatedAt, startTime)
                .le(endTime != null, AuditLogEntity::getCreatedAt, endTime)
                .orderByDesc(AuditLogEntity::getCreatedAt);
        return this.selectList(wrapper);
    }
}
