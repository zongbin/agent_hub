package com.leon.agenthub.permission.controller;

import com.leon.agenthub.audit.annotation.AuditLog;
import com.leon.agenthub.auth.security.SecurityContextUtil;
import com.leon.agenthub.common.response.R;
import com.leon.agenthub.permission.annotation.RequirePermission;
import com.leon.agenthub.permission.enums.PermissionAction;
import com.leon.agenthub.permission.enums.SecurityLevel;
import com.leon.agenthub.permission.enums.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;

/**
 * 权限测试控制器 — 用于验证权限矩阵和集成测试。
 */
@Slf4j
@RestController
@RequestMapping("/permissions/test")
@RequiredArgsConstructor
public class PermissionTestController {

    /**
     * 测试：公开级别访问（所有角色可访问）。
     */
    @GetMapping("/public")
    @RequirePermission(resource = "DOCUMENT", action = PermissionAction.READ, minLevel = SecurityLevel.PUBLIC)
    @AuditLog(action = "VIEW_PUBLIC", resource = "DOCUMENT")
    public R<String> testPublicAccess(HttpServletRequest httpRequest) {
        return R.ok("公开文档访问成功 - 当前角色: " + SecurityContextUtil.getCurrentUserRole());
    }

    /**
     * 测试：内部级别访问（仅正式员工可访问）。
     */
    @GetMapping("/internal")
    @RequirePermission(resource = "DOCUMENT", action = PermissionAction.READ, minLevel = SecurityLevel.INTERNAL)
    @AuditLog(action = "VIEW_INTERNAL", resource = "DOCUMENT")
    public R<String> testInternalAccess(HttpServletRequest httpRequest) {
        return R.ok("内部文档访问成功 - 当前角色: " + SecurityContextUtil.getCurrentUserRole());
    }

    /**
     * 测试：机密级别访问（仅授权人员可访问）。
     */
    @GetMapping("/confidential")
    @RequirePermission(resource = "DOCUMENT", action = PermissionAction.READ, minLevel = SecurityLevel.CONFIDENTIAL)
    @AuditLog(action = "VIEW_CONFIDENTIAL", resource = "DOCUMENT")
    public R<String> testConfidentialAccess(HttpServletRequest httpRequest) {
        return R.ok("机密文档访问成功 - 当前角色: " + SecurityContextUtil.getCurrentUserRole());
    }

    /**
     * 测试：获取当前用户身份信息。
     */
    @GetMapping("/whoami")
    public R<UserIdentity> getCurrentIdentity() {
        UserIdentity identity = new UserIdentity();
        identity.setUserId(SecurityContextUtil.getCurrentUserId());
        identity.setRole(SecurityContextUtil.getCurrentUserRole().getCode());
        identity.setDepartment(SecurityContextUtil.getCurrentUserDepartment());
        return R.ok(identity);
    }

    /**
     * 用户身份信息 DTO。
     */
    @lombok.Data
    public static class UserIdentity implements Serializable {
        private Long userId;
        private String role;
        private String department;
    }
}
