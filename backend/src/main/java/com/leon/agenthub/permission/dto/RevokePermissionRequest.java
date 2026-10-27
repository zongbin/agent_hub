package com.leon.agenthub.permission.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 撤销文档权限请求 DTO。
 */
@Data
public class RevokePermissionRequest implements Serializable {

    /** 被撤销权限的用户 ID */
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;
}
