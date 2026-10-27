package com.leon.agenthub.permission.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.leon.agenthub.permission.handler.JsonbTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 审计日志实体 — 对应 audit_logs 表。
 *
 * <p>记录用户操作（登录、文档访问、权限变更等），用于安全审计和合规追溯。
 */
@Data
@TableName(value = "audit_logs", autoResultMap = true)
public class AuditLogEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作用户 ID */
    private Long userId;

    /** 操作用户名（冗余存储，防关联丢失） */
    private String username;

    /** 操作类型（LOGIN/VIEW/EDIT/DELETE/GRANT/REVOKE/DENIED 等） */
    private String action;

    /** 目标资源描述 */
    private String resource;

    /** 操作详情（JSON） */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> detail;

    /** 用户 IP 地址 */
    private String ipAddress;

    /** 操作是否成功：1 成功，0 失败/拒绝 */
    private Integer success;

    /** 创建时间 */
    private OffsetDateTime createdAt;
}
