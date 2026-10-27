package com.leon.agenthub.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码请求 DTO — 用户自行修改密码。
 */
@Data
public class ChangePasswordRequest implements Serializable {

    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度 6-64")
    private String newPassword;
}
