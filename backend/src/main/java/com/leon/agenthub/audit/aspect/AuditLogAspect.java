package com.leon.agenthub.audit.aspect;

import com.leon.agenthub.audit.annotation.AuditLog;
import com.leon.agenthub.auth.security.SecurityContextUtil;
import com.leon.agenthub.common.util.IpUtils;
import com.leon.agenthub.permission.model.AuditLogEntity;
import com.leon.agenthub.permission.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 审计日志切面 — 拦截标注 @AuditLog 的方法，自动记录操作审计。
 *
 * <p>方法成功执行后记录 SUCCESS，异常抛出后记录 DENIED/FAILED。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;
    private final SecurityContextUtil securityContextUtil;

    /**
     * 方法成功返回后记录审计日志。
     */
    @AfterReturning("@annotation(auditAnnotation)")
    public void logSuccess(JoinPoint joinPoint, AuditLog auditAnnotation) {
        saveAuditLog(joinPoint, auditAnnotation.action(), auditAnnotation.resource(), true, null);
    }

    /**
     * 方法抛出异常后记录失败审计日志。
     */
    @AfterThrowing(pointcut = "@annotation(auditAnnotation)", throwing = "ex")
    public void logFailure(JoinPoint joinPoint, AuditLog auditAnnotation, Throwable ex) {
        saveAuditLog(joinPoint, auditAnnotation.action(), auditAnnotation.resource(), false, ex.getMessage());
    }

    private void saveAuditLog(JoinPoint joinPoint, String action, String resource, boolean success, String errorMsg) {
        try {
            AuditLogEntity auditLog = new AuditLogEntity();

            // 获取当前用户信息
            try {
                Long userId = securityContextUtil.getCurrentUserId();
                auditLog.setUserId(userId);
                // 从 Authentication 中取出 CustomUserDetails 获取用户名
                var authentication = org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof
                        com.leon.agenthub.auth.security.CustomUserDetails userDetails) {
                    auditLog.setUsername(userDetails.getUsername());
                }
            } catch (Exception e) {
                auditLog.setUserId(null);
            }

            auditLog.setAction(action);
            auditLog.setResource(resource);
            auditLog.setSuccess(success ? 1 : 0);

            // 获取 IP 地址 — 优先从方法参数中取 HttpServletRequest，回退到 RequestContextHolder
            HttpServletRequest request = extractRequest(joinPoint);
            if (request != null) {
                auditLog.setIpAddress(IpUtils.getClientIp(request));
            }

            // 构建详情
            Map<String, Object> detail = new HashMap<>();
            detail.put("timestamp", OffsetDateTime.now().toString());
            if (errorMsg != null) {
                detail.put("error", errorMsg);
            }
            auditLog.setDetail(detail);

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: action={}, resource={}, success={}", action, resource, success);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage(), e);
        }
    }

    /**
     * 从 JoinPoint 的方法参数中提取 HttpServletRequest。
     * 若参数中无 HttpServletRequest，则回退到 RequestContextHolder。
     */
    private HttpServletRequest extractRequest(JoinPoint joinPoint) {
        // 1. 遍历方法参数查找 HttpServletRequest
        Object[] args = joinPoint.getArgs();
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof HttpServletRequest httpServletRequest) {
                    return httpServletRequest;
                }
            }
        }

        // 2. 回退到 RequestContextHolder
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            log.debug("Failed to get request from RequestContextHolder: {}", e.getMessage());
        }

        return null;
    }
}
