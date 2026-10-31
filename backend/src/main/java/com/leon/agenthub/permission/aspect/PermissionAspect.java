package com.leon.agenthub.permission.aspect;

import com.leon.agenthub.auth.security.SecurityContextUtil;
import com.leon.agenthub.common.util.IpUtils;
import com.leon.agenthub.permission.annotation.RequirePermission;
import com.leon.agenthub.permission.enums.PermissionAction;
import com.leon.agenthub.permission.enums.SecurityLevel;
import com.leon.agenthub.permission.enums.UserRole;
import com.leon.agenthub.permission.exception.PermissionDeniedException;
import com.leon.agenthub.permission.model.AuditLogEntity;
import com.leon.agenthub.permission.repository.AuditLogRepository;
import com.leon.agenthub.permission.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 权限校验切面 — 拦截标注 @RequirePermission 的方法，执行权限判定。
 *
 * <p>判定顺序：管理员直通 → 角色密级匹配 → 拒绝。
 * 文档级授权由 PermissionService 在业务层查询（需资源 ID 参数）。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;
    private final AuditLogRepository auditLogRepository;

    /**
     * 前置通知 — 在方法执行前校验权限。
     *
     * <p>判定顺序：管理员直通 → 仅管理员接口 → 角色 × 操作 × 密级判定 → 拒绝。
     */
    @Before("@annotation(requirePermission)")
    public void checkPermission(RequirePermission requirePermission) {
        UserRole currentUserRole = SecurityContextUtil.getCurrentUserRole();
        SecurityLevel minLevel = requirePermission.minLevel();
        PermissionAction action = requirePermission.action();
        boolean write = action.isWrite();

        // 1. 管理员直通
        if (currentUserRole == UserRole.ADMIN) {
            log.debug("Admin user passes permission check");
            return;
        }

        // 2. 仅管理员可访问的接口
        if (requirePermission.adminOnly()) {
            log.warn("Non-admin role {} rejected for admin-only operation {}:{}",
                    currentUserRole, requirePermission.resource(), action);
            saveDeniedAuditLog(requirePermission.resource(), action, "ADMIN");
            throw new PermissionDeniedException("权限不足：该操作仅管理员可执行");
        }

        // 3. 角色 × 操作 × 密级判定
        //    写操作（上传、删除、授权等）要求角色具备写权限 —— 查看者/访客为只读角色，一律拒绝
        if (permissionService.canPerformAction(currentUserRole, minLevel, write)) {
            log.debug("Role {} passes {} check for action {}", currentUserRole, minLevel, action);
            return;
        }

        // 4. 拒绝 — 记录审计日志
        saveDeniedAuditLog(requirePermission.resource(), action, minLevel.name());
        if (write) {
            throw new PermissionDeniedException("权限不足：当前角色为只读角色，不能执行" + action.getLabel() + "操作");
        }
        throw new PermissionDeniedException("权限不足：需要 " + minLevel + " 级别访问权限");
    }

    /**
     * 记录权限拒绝审计日志。
     */
    private void saveDeniedAuditLog(String resource, PermissionAction action, String securityLevel) {
        try {
            AuditLogEntity auditLog = new AuditLogEntity();
            try {
                auditLog.setUserId(SecurityContextUtil.getCurrentUserId());
            } catch (Exception e) {
                auditLog.setUserId(null);
            }
            auditLog.setAction("DENIED");
            auditLog.setResource(resource + ":" + action + ":" + securityLevel);
            auditLog.setSuccess(0);

            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIpAddress(IpUtils.getClientIp(request));
            }

            Map<String, Object> detail = new HashMap<>();
            detail.put("action", action);
            detail.put("requiredLevel", securityLevel);
            detail.put("timestamp", OffsetDateTime.now().toString());
            auditLog.setDetail(detail);

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save denied audit log: {}", e.getMessage(), e);
        }
    }
}
