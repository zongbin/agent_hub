package com.leon.agenthub.permission.controller;

import java.util.List;

import com.leon.agenthub.audit.annotation.AuditLog;
import com.leon.agenthub.auth.model.User;
import com.leon.agenthub.common.dto.PageQuery;
import com.leon.agenthub.common.dto.PageResult;
import com.leon.agenthub.common.response.R;
import com.leon.agenthub.permission.dto.AuditLogQuery;
import com.leon.agenthub.permission.dto.CreateUserRequest;
import com.leon.agenthub.permission.dto.DocPermissionVO;
import com.leon.agenthub.permission.dto.GrantPermissionRequest;
import com.leon.agenthub.permission.dto.ResetPasswordRequest;
import com.leon.agenthub.permission.dto.RevokePermissionRequest;
import com.leon.agenthub.permission.dto.RoleInfo;
import com.leon.agenthub.permission.dto.UpdateRoleRequest;
import com.leon.agenthub.permission.dto.UpdateStatusRequest;
import com.leon.agenthub.permission.dto.UpdateUserRequest;
import com.leon.agenthub.permission.model.AuditLogEntity;
import com.leon.agenthub.permission.service.AuditLogService;
import com.leon.agenthub.permission.service.DocumentPermissionService;
import com.leon.agenthub.permission.service.UserAdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限管理控制器 — 用户角色管理、文档授权/撤销、审计日志查询。
 *
 * <p>所有接口仅 ADMIN 角色可访问 —— 权限判定下沉到 Service 层
 * （{@code @RequirePermission(adminOnly = true)}），Controller 只做转发与包装。
 */
@Slf4j
@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final UserAdminService userAdminService;
    private final DocumentPermissionService documentPermissionService;
    private final AuditLogService auditLogService;

    /**
     * 获取用户列表。
     */
    @GetMapping("/users")
    public R<List<User>> listUsers() {
        return R.ok(userAdminService.listUsers());
    }

    /**
     * 创建用户。
     */
    @PostMapping("/users")
    @AuditLog(action = "CREATE_USER", resource = "USER")
    public R<User> createUser(@RequestBody @Valid CreateUserRequest request) {
        return R.ok(userAdminService.createUser(request));
    }

    /**
     * 更新用户信息。
     */
    @PutMapping("/users/{userId}")
    @AuditLog(action = "UPDATE_USER", resource = "USER")
    public R<User> updateUser(@PathVariable Long userId, @RequestBody @Valid UpdateUserRequest request) {
        return R.ok(userAdminService.updateUser(userId, request));
    }

    /**
     * 删除用户（逻辑删除）。
     */
    @DeleteMapping("/users/{userId}")
    @AuditLog(action = "DELETE_USER", resource = "USER")
    public R<Void> deleteUser(@PathVariable Long userId) {
        userAdminService.deleteUser(userId);
        return R.ok();
    }

    /**
     * 启用/禁用用户。
     */
    @PutMapping("/users/{userId}/status")
    @AuditLog(action = "TOGGLE_STATUS", resource = "USER")
    public R<Void> toggleUserStatus(@PathVariable Long userId, @RequestBody @Valid UpdateStatusRequest request) {
        userAdminService.toggleUserStatus(userId, request);
        return R.ok();
    }

    /**
     * 重置用户密码。
     */
    @PutMapping("/users/{userId}/password")
    @AuditLog(action = "RESET_PASSWORD", resource = "USER")
    public R<Void> resetPassword(@PathVariable Long userId, @RequestBody @Valid ResetPasswordRequest request) {
        userAdminService.resetPassword(userId, request);
        return R.ok();
    }

    /**
     * 获取角色列表。
     */
    @GetMapping("/roles")
    public R<List<RoleInfo>> listRoles() {
        return R.ok(userAdminService.listRoles());
    }

    /**
     * 修改用户角色。
     */
    @PutMapping("/users/{userId}/role")
    @AuditLog(action = "UPDATE_ROLE", resource = "USER")
    public R<Void> updateUserRole(@PathVariable Long userId, @RequestBody @Valid UpdateRoleRequest request) {
        userAdminService.updateUserRole(userId, request);
        return R.ok();
    }

    /**
     * 分页查询全部文档授权。
     *
     * <p>分页参数继承通用基类 {@link PageQuery}（page 从 1 开始、size 默认 20）。
     */
    @GetMapping("/documents/grants")
    public R<PageResult<DocPermissionVO>> listDocumentGrants(PageQuery query) {
        return R.ok(documentPermissionService.listAll(query));
    }

    /**
     * 查询某个文档的授权列表。
     */
    @GetMapping("/documents/{documentId}/permissions")
    public R<List<DocPermissionVO>> listDocumentPermissions(@PathVariable Long documentId) {
        return R.ok(documentPermissionService.listByDocument(documentId));
    }

    /**
     * 授予文档权限。
     */
    @PostMapping("/documents/{documentId}/grant")
    @AuditLog(action = "GRANT", resource = "DOCUMENT")
    public R<DocPermissionVO> grantDocumentPermission(
            @PathVariable Long documentId,
            @RequestBody @Valid GrantPermissionRequest request) {
        return R.ok(documentPermissionService.grant(documentId, request.getUserId(), request.getPermission()));
    }

    /**
     * 撤销文档权限。
     */
    @DeleteMapping("/documents/{documentId}/revoke")
    @AuditLog(action = "REVOKE", resource = "DOCUMENT")
    public R<Void> revokeDocumentPermission(
            @PathVariable Long documentId,
            @RequestBody @Valid RevokePermissionRequest request) {
        documentPermissionService.revoke(documentId, request.getUserId());
        return R.ok();
    }

    /**
     * 查询审计日志（分页）。
     */
    @GetMapping("/audit-logs")
    public R<PageResult<AuditLogEntity>> queryAuditLogs(AuditLogQuery query) {
        return R.ok(PageResult.of(auditLogService.queryAuditLogs(query)));
    }
}
