package com.leon.agenthub.auth.security;

import com.leon.agenthub.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * SecurityContext 工具类 — 获取当前登录用户身份。
 */
@Component
public class SecurityContextUtil {

    /**
     * 获取当前登录用户 ID。
     *
     * @return 用户 ID
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "未登录");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            try {
                return Long.parseLong(userDetails.getUsername());
            } catch (NumberFormatException e) {
                throw new BusinessException(401, "用户身份无效");
            }
        }
        throw new BusinessException(401, "用户身份无效");
    }
}
