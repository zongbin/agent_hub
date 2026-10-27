package com.leon.agenthub.permission.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 授予文档权限请求 DTO。
 */
@Data
public class GrantPermissionRequest implements Serializable {

    /** 被授权用户 ID */
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    /** 权限类型：READ / WRITE */
    private String permission = "READ";
}
