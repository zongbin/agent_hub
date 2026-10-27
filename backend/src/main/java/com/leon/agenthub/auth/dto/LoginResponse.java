package com.leon.agenthub.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录响应 DTO — 包含 JWT Token 和用户基本信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse implements Serializable {

    /** JWT Token */
    private String token;

    /** Token 类型，固定为 Bearer */
    private String tokenType;

    /** Token 过期时间（秒） */
    private long expiresIn;

    /** 用户信息 */
    private UserInfo userInfo;
}
