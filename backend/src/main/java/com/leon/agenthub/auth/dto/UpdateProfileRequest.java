package com.leon.agenthub.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新个人资料请求 DTO — 用户自行修改自己的信息。
 */
@Data
public class UpdateProfileRequest implements Serializable {

    @Size(max = 64, message = "姓名长度不能超过 64")
    private String realName;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 64, message = "部门长度不能超过 64")
    private String department;
}
