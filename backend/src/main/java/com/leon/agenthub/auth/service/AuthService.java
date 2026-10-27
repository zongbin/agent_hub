package com.leon.agenthub.auth.service;

import com.leon.agenthub.auth.dto.LoginRequest;
import com.leon.agenthub.auth.dto.LoginResponse;
import com.leon.agenthub.auth.dto.UserInfo;
import com.leon.agenthub.auth.mapper.UserMapper;
import com.leon.agenthub.auth.model.User;
import com.leon.agenthub.auth.util.JwtUtil;
import com.leon.agenthub.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务 — 处理登录、登出、用户信息查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 用户登录 — 校验用户名密码，签发 JWT。
     *
     * @param request 登录请求
     * @return 登录响应（含 Token 和用户信息）
     */
    public LoginResponse login(LoginRequest request) {
        // 1. 查询用户
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 2. 校验账号状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }

        // 3. 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 4. 生成 JWT
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 5. 构造响应
        UserInfo userInfo = UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .department(user.getDepartment())
                .build();

        long expiresIn = jwtUtil.getExpirationHours() * 3600;

        log.info("User logged in: {}", user.getUsername());

        return new LoginResponse(token, "Bearer", expiresIn, userInfo);
    }

    /**
     * 获取当前用户信息。
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    public UserInfo getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        return UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .department(user.getDepartment())
                .build();
    }

    /**
     * 用户登出 — MVP 仅返回成功（客户端丢弃 Token）。
     * 预留扩展点：可在此处将 Token 加入 Redis 黑名单。
     */
    public void logout() {
        // MVP 无状态登出，客户端清除 Token 即可
        log.debug("User logged out (stateless)");
    }
}
