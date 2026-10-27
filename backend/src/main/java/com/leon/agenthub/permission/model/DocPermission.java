package com.leon.agenthub.permission.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 文档级授权实体 — 对应 doc_permissions 表。
 *
 * <p>用于机密文档的细粒度授权，支持按用户或按部门授权。
 */
@Data
@TableName("doc_permissions")
public class DocPermission implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文档 ID */
    private Long documentId;

    /** 被授权用户 ID */
    private Long granteeId;

    /** 权限类型：READ / WRITE */
    private String permission;

    /** 创建时间 */
    private OffsetDateTime createdAt;
}
