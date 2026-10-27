package com.leon.agenthub.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 重置密码请求 DTO。
 */
@Data
public class ResetPasswordRequest implements Serializable {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度 6-64")
    private String newPassword;
}
