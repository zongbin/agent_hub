package com.leon.agenthub.permission.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leon.agenthub.auth.enums.UserStatus;
import com.leon.agenthub.auth.model.User;
import com.leon.agenthub.auth.repository.UserRepository;
import com.leon.agenthub.common.exception.BusinessException;
import com.leon.agenthub.permission.annotation.RequirePermission;
import com.leon.agenthub.permission.dto.CreateUserRequest;
import com.leon.agenthub.permission.dto.ResetPasswordRequest;
import com.leon.agenthub.permission.dto.RoleInfo;
import com.leon.agenthub.permission.dto.UpdateRoleRequest;
import com.leon.agenthub.permission.dto.UpdateStatusRequest;
import com.leon.agenthub.permission.dto.UpdateUserRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户管理服务 — 用户与角色的增删改查（管理员专属）。
 *
 * <p>权限判定统一由 {@link RequirePermission#adminOnly()} 表达，
 * Controller 只做转发与包装。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 查询用户列表（按创建时间倒序，密码字段已脱敏）。
     */
    @Transactional(readOnly = true)
    @RequirePermission(resource = "USER", action = "READ", adminOnly = true)
    public List<User> listUsers() {
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    /**
     * 查询角色字典。
     */
    @RequirePermission(resource = "USER", action = "READ", adminOnly = true)
    public List<RoleInfo> listRoles() {
        return List.of(
                RoleInfo.builder().code("ADMIN").name("管理员").description("全部权限 + 系统配置").build(),
                RoleInfo.builder().code("EDITOR").name("编辑者").description("上传/编辑文档、使用 SuperAgent").build(),
                RoleInfo.builder().code("VIEWER").name("查看者").description("查看授权文档、基础问答").build(),
                RoleInfo.builder().code("GUEST").name("访客").description("仅查看公开文档").build()
        );
    }

    /**
     * 创建用户。
     */
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission(resource = "USER", action = "WRITE", adminOnly = true)
    public User createUser(CreateUserRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException(400, "用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole().toUpperCase());
        user.setDepartment(request.getDepartment());
        user.setStatus(UserStatus.ENABLED.getCode());
        userRepository.save(user);
        user.setPassword(null);
        log.info("User created: {}", user.getUsername());
        return user;
    }

    /**
     * 更新用户基本信息。
     */
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission(resource = "USER", action = "WRITE", adminOnly = true)
    public User updateUser(Long userId, UpdateUserRequest request) {
        User user = requireUser(userId);
        if (request.getRealName() != null) user.setRealName(request.getRealName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getDepartment() != null) user.setDepartment(request.getDepartment());
        if (request.getRole() != null) user.setRole(request.getRole().toUpperCase());
        userRepository.save(user);
        user.setPassword(null);
        log.info("User {} updated", userId);
        return user;
    }

    /**
     * 删除用户（逻辑删除）。
     */
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission(resource = "USER", action = "WRITE", adminOnly = true)
    public void deleteUser(Long userId) {
        User user = requireUser(userId);
        user.setDeleted(1);
        userRepository.save(user);
        log.info("User {} deleted (soft)", userId);
    }

    /**
     * 启用/禁用用户。
     */
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission(resource = "USER", action = "WRITE", adminOnly = true)
    public void toggleUserStatus(Long userId, UpdateStatusRequest request) {
        User user = requireUser(userId);
        user.setStatus(UserStatus.fromCode(request.getStatus()).getCode());
        userRepository.save(user);
        log.info("User {} status set to {}", userId, request.getStatus());
    }

    /**
     * 重置用户密码。
     */
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission(resource = "USER", action = "WRITE", adminOnly = true)
    public void resetPassword(Long userId, ResetPasswordRequest request) {
        User user = requireUser(userId);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("User {} password reset", userId);
    }

    /**
     * 修改用户角色。
     */
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission(resource = "USER", action = "WRITE", adminOnly = true)
    public void updateUserRole(Long userId, UpdateRoleRequest request) {
        User user = requireUser(userId);
        user.setRole(request.getRole().toUpperCase());
        userRepository.save(user);
        log.info("User {} role updated to {}", userId, request.getRole());
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }
}
