package com.leon.agenthub.auth.controller;

import com.leon.agenthub.auth.dto.LoginRequest;
import com.leon.agenthub.auth.dto.LoginResponse;
import com.leon.agenthub.auth.dto.UserInfo;
import com.leon.agenthub.auth.security.SecurityContextUtil;
import com.leon.agenthub.auth.service.AuthService;
import com.leon.agenthub.common.response.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器 — 登录、登出、当前用户信息。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecurityContextUtil securityContextUtil;

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return JWT Token + 用户信息
     */
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    /**
     * 用户登出。
     *
     * @return 成功
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    /**
     * 获取当前登录用户信息。
     *
     * @return 用户信息
     */
    @GetMapping("/me")
    public R<UserInfo> getCurrentUser() {
        Long userId = securityContextUtil.getCurrentUserId();
        return R.ok(authService.getCurrentUser(userId));
    }
}
