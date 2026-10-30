package com.leon.agenthub.permission.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;

import lombok.Data;

/**
 * 文档级授权视图对象 — 授权记录 + 文档标题 + 被授权人信息。
 */
@Data
public class DocPermissionVO implements Serializable {

    /** 授权记录 ID */
    private Long id;

    /** 文档 ID */
    private Long documentId;

    /** 文档标题（文档已删除时为「文档已删除」） */
    private String documentTitle;

    /** 文档密级：PUBLIC / INTERNAL / CONFIDENTIAL */
    private String documentSecurityLevel;

    /** 被授权用户 ID */
    private Long granteeId;

    /** 被授权人姓名 */
    private String granteeName;

    /** 被授权人用户名 */
    private String granteeUsername;

    /** 被授权人角色 */
    private String granteeRole;

    /** 权限类型：READ / WRITE */
    private String permission;

    /** 权限类型中文标签 */
    private String permissionLabel;

    /** 授权时间 */
    private OffsetDateTime createdAt;
}
