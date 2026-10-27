package com.leon.agenthub.permission.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改用户角色请求 DTO。
 */
@Data
public class UpdateRoleRequest implements Serializable {

    /** 目标角色代码：ADMIN/EDITOR/VIEWER/GUEST */
    @NotBlank(message = "角色不能为空")
    private String role;
}
