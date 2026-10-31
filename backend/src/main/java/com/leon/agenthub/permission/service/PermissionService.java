package com.leon.agenthub.permission.service;

import com.leon.agenthub.permission.enums.SecurityLevel;
import com.leon.agenthub.permission.enums.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 权限服务 — 封装角色权限矩阵判定逻辑。
 *
 * <p>角色权限矩阵：
 * <ul>
 *   <li>ADMIN — 可访问任意密级</li>
 *   <li>EDITOR — 可访问 PUBLIC + INTERNAL，CONFIDENTIAL 只读</li>
 *   <li>VIEWER — 可访问 PUBLIC + INTERNAL（只读）</li>
 *   <li>GUEST — 仅可访问 PUBLIC</li>
 * </ul>
 */
@Slf4j
@Service
public class PermissionService {

    /**
     * 判断角色是否可以访问指定密级的资源。
     *
     * @param role      用户角色
     * @param level     资源安全密级
     * @return true 表示有权限访问
     */
    public boolean canAccess(UserRole role, SecurityLevel level) {
        if (role == null || level == null) {
            return false;
        }
        return switch (role) {
            case ADMIN -> true;
            case EDITOR -> level != SecurityLevel.CONFIDENTIAL || canEditConfidential();
            case VIEWER -> level == SecurityLevel.PUBLIC || level == SecurityLevel.INTERNAL;
            case GUEST -> level == SecurityLevel.PUBLIC;
        };
    }

    /**
     * 编辑者对机密文档仅有只读权限。
     */
    private boolean canEditConfidential() {
        // 编辑者可读机密文档，但不可写（由 action 校验控制）
        return true;
    }

    /**
     * 判断角色是否可以执行指定操作。
     *
     * @param role   用户角色
     * @param level  资源安全密级
     * @param write  是否为写操作
     * @return true 表示有权限执行
     */
    public boolean canPerformAction(UserRole role, SecurityLevel level, boolean write) {
        if (!canAccess(role, level)) {
            return false;
        }
        // 查看者和访客为只读角色 —— 任何写操作（上传、删除、授权）一律拒绝
        if (write && isReadOnlyRole(role)) {
            return false;
        }
        // 编辑者对机密文档只读
        if (write && role == UserRole.EDITOR && level == SecurityLevel.CONFIDENTIAL) {
            return false;
        }
        return true;
    }

    /**
     * 是否为只读角色（查看者 / 访客）。
     */
    public boolean isReadOnlyRole(UserRole role) {
        return role == UserRole.VIEWER || role == UserRole.GUEST;
    }
}
