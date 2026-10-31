package com.leon.agenthub.auth.security;

import com.leon.agenthub.common.exception.BusinessException;
import com.leon.agenthub.permission.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * SecurityContext 工具类 — 获取当前登录用户身份。
 *
 * <p>无状态工具类，所有方法均为静态，直接读取 {@link SecurityContextHolder} 的 ThreadLocal 上下文。
 */
public final class SecurityContextUtil {

    private SecurityContextUtil() {
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 用户 ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "未登录");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        if (principal instanceof UserDetails userDetails) {
            try {
                return Long.parseLong(userDetails.getUsername());
            } catch (NumberFormatException e) {
                throw new BusinessException(401, "用户身份无效");
            }
        }
        throw new BusinessException(401, "用户身份无效");
    }

    /**
     * 获取当前登录用户角色。
     *
     * @return 用户角色枚举
     */
    public static UserRole getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "未登录");
        }
        // 从认证对象的 authorities 中提取角色
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse("GUEST");
        try {
            return UserRole.fromCode(role);
        } catch (IllegalArgumentException e) {
            return UserRole.GUEST;
        }
    }

    /**
     * 获取当前登录用户部门。
     *
     * @return 部门名称，可能为 null
     */
    public static String getCurrentUserDepartment() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "未登录");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.leon.agenthub.auth.security.CustomUserDetails userDetails) {
            return userDetails.getDepartment();
        }
        return null;
    }
}
