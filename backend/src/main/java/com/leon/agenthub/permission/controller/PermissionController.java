package com.leon.agenthub.permission.controller;

import com.leon.agenthub.audit.annotation.AuditLog;
import com.leon.agenthub.auth.enums.UserStatus;
import com.leon.agenthub.auth.model.User;
import com.leon.agenthub.auth.repository.UserRepository;
import com.leon.agenthub.auth.security.SecurityContextUtil;
import com.leon.agenthub.common.exception.BusinessException;
import com.leon.agenthub.common.response.R;
import com.leon.agenthub.permission.dto.*;
import com.leon.agenthub.permission.enums.UserRole;
import com.leon.agenthub.permission.model.AuditLogEntity;
import com.leon.agenthub.permission.repository.AuditLogRepository;
import com.leon.agenthub.permission.service.DocumentPermissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限管理控制器 — 用户角色管理、文档授权/撤销、审计日志查询。
 *
 * <p>所有接口仅 ADMIN 角色可访问。
 */
@Slf4j
@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final UserRepository userRepository;
    private final DocumentPermissionService documentPermissionService;
    private final AuditLogRepository auditLogRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecurityContextUtil securityContextUtil;

    /**
     * 获取用户列表。
     */
    @GetMapping("/users")
    public R<List<User>> listUsers() {
        checkAdmin();
        List<User> users = userRepository.findAll(
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        // 移除密码字段
        users.forEach(u -> u.setPassword(null));
        return R.ok(users);
    }

    /**
     * 创建用户。
     */
    @PostMapping("/users")
    @AuditLog(action = "CREATE_USER", resource = "USER")
    public R<User> createUser(HttpServletRequest httpRequest, @RequestBody CreateUserRequest request) {
        // 检查用户名是否已存在
        User existing = userRepository.findByUsername(request.getUsername()).orElse(null);
        if (existing != null) {
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
        return R.ok(user);
    }

    /**
     * 更新用户信息。
     */
    @PutMapping("/users/{userId}")
    @AuditLog(action = "UPDATE_USER", resource = "USER")
    public R<User> updateUser(HttpServletRequest httpRequest, @PathVariable Long userId, @RequestBody UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        if (request.getRealName() != null) user.setRealName(request.getRealName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getDepartment() != null) user.setDepartment(request.getDepartment());
        if (request.getRole() != null) user.setRole(request.getRole().toUpperCase());
        userRepository.save(user);
        user.setPassword(null);
        log.info("User {} updated", userId);
        return R.ok(user);
    }

    /**
     * 删除用户（逻辑删除）。
     */
    @DeleteMapping("/users/{userId}")
    @AuditLog(action = "DELETE_USER", resource = "USER")
    public R<Void> deleteUser(HttpServletRequest httpRequest, @PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        // 软删除：标记 deleted=1
        user.setDeleted(1);
        userRepository.save(user);
        log.info("User {} deleted (soft)", userId);
        return R.ok();
    }

    /**
     * 启用/禁用用户。
     */
    @PutMapping("/users/{userId}/status")
    @AuditLog(action = "TOGGLE_STATUS", resource = "USER")
    public R<Void> toggleUserStatus(HttpServletRequest httpRequest, @PathVariable Long userId, @RequestBody UpdateStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        user.setStatus(UserStatus.fromCode(request.getStatus()).getCode());
        userRepository.save(user);
        log.info("User {} status set to {}", userId, request.getStatus());
        return R.ok();
    }

    /**
     * 重置用户密码。
     */
    @PutMapping("/users/{userId}/password")
    @AuditLog(action = "RESET_PASSWORD", resource = "USER")
    public R<Void> resetPassword(HttpServletRequest httpRequest, @PathVariable Long userId, @RequestBody ResetPasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("User {} password reset", userId);
        return R.ok();
    }

    /**
     * 获取角色列表。
     */
    @GetMapping("/roles")
    public R<List<RoleInfo>> listRoles() {
        List<RoleInfo> roles = List.of(
                RoleInfo.builder().code("ADMIN").name("管理员").description("全部权限 + 系统配置").build(),
                RoleInfo.builder().code("EDITOR").name("编辑者").description("上传/编辑文档、使用 SuperAgent").build(),
                RoleInfo.builder().code("VIEWER").name("查看者").description("查看授权文档、基础问答").build(),
                RoleInfo.builder().code("GUEST").name("访客").description("仅查看公开文档").build()
        );
        return R.ok(roles);
    }

    /**
     * 修改用户角色。
     */
    @PutMapping("/users/{userId}/role")
    @AuditLog(action = "UPDATE_ROLE", resource = "USER")
    public R<Void> updateUserRole(HttpServletRequest httpRequest, @PathVariable Long userId, @RequestBody UpdateRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        user.setRole(request.getRole().toUpperCase());
        userRepository.save(user);
        log.info("User {} role updated to {}", userId, request.getRole());
        return R.ok();
    }

    /**
     * 获取文档授权列表。
     */
    @GetMapping("/documents/{documentId}/permissions")
    public R<List<com.leon.agenthub.permission.model.DocPermission>> listDocumentPermissions(
            @PathVariable Long documentId) {
        List<com.leon.agenthub.permission.model.DocPermission> permissions =
                documentPermissionService.listByDocument(documentId);
        return R.ok(permissions);
    }

    /**
     * 授予文档权限。
     */
    @PostMapping("/documents/{documentId}/grant")
    @AuditLog(action = "GRANT", resource = "DOCUMENT")
    public R<Void> grantDocumentPermission(
            HttpServletRequest httpRequest,
            @PathVariable Long documentId,
            @RequestBody GrantPermissionRequest request) {
        documentPermissionService.grantDocument(documentId, request.getUserId(), request.getPermission());
        return R.ok();
    }

    /**
     * 撤销文档权限。
     */
    @DeleteMapping("/documents/{documentId}/revoke")
    @AuditLog(action = "REVOKE", resource = "DOCUMENT")
    public R<Void> revokeDocumentPermission(
            HttpServletRequest httpRequest,
            @PathVariable Long documentId,
            @RequestBody RevokePermissionRequest request) {
        documentPermissionService.revokeDocument(documentId, request.getUserId());
        return R.ok();
    }

    /**
     * 校验当前用户是否为管理员，不是则抛出 403。
     */
    private void checkAdmin() {
        UserRole role = securityContextUtil.getCurrentUserRole();
        if (role != UserRole.ADMIN) {
            throw new BusinessException(403, "仅管理员可访问");
        }
    }

    /**
     * 查询审计日志（分页）。
     *
     * <p>分页参数继承通用基类 {@link com.leon.agenthub.common.dto.PageQuery}（page 从 1 开始、size 默认 20）。
     */
    @GetMapping("/audit-logs")
    public R<Map<String, Object>> queryAuditLogs(AuditLogQuery query) {

        OffsetDateTime start = query.getStartTime() != null ? OffsetDateTime.parse(query.getStartTime()) : null;
        OffsetDateTime end = query.getEndTime() != null ? OffsetDateTime.parse(query.getEndTime()) : null;

        // 真正的数据库级分页（替代原来的内存分页）
        PageRequest pageRequest = query.toPageRequest(Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLogEntity> page = auditLogRepository.findByCondition(query.getUserId(), start, end, pageRequest);

        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getContent());
        result.put("total", page.getTotalElements());
        result.put("page", query.getPage());
        result.put("size", query.getPageSize());

        return R.ok(result);
    }
}
