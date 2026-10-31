package com.leon.agenthub.auth.controller;

import com.leon.agenthub.auth.dto.ChangePasswordRequest;
import com.leon.agenthub.auth.dto.LoginRequest;
import com.leon.agenthub.auth.dto.LoginResponse;
import com.leon.agenthub.auth.dto.UpdateProfileRequest;
import com.leon.agenthub.auth.dto.UserInfo;
import com.leon.agenthub.auth.security.SecurityContextUtil;
import com.leon.agenthub.auth.service.AuthService;
import com.leon.agenthub.common.response.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
        Long userId = SecurityContextUtil.getCurrentUserId();
        return R.ok(authService.getCurrentUser(userId));
    }

    /**
     * 更新个人资料 — 用户自行修改姓名、邮箱、部门。
     *
     * @param request 更新内容
     * @return 更新后的用户信息
     */
    @PutMapping("/me")
    public R<UserInfo> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        return R.ok(authService.updateProfile(userId, request));
    }

    /**
     * 修改密码 — 需校验原密码。
     *
     * @param request 原密码 + 新密码
     * @return 成功
     */
    @PutMapping("/me/password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        authService.changePassword(userId, request);
        return R.ok();
    }
}
