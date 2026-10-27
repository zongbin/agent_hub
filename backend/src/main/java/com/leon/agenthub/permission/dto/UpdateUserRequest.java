package com.leon.agenthub.permission.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新用户信息请求 DTO。
 */
@Data
public class UpdateUserRequest implements Serializable {

    private String realName;

    private String email;

    private String role;

    private String department;
}
