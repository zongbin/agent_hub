package com.leon.agenthub.permission.aspect;

import com.leon.agenthub.auth.security.SecurityContextUtil;
import com.leon.agenthub.permission.annotation.RequirePermission;
import com.leon.agenthub.permission.enums.PermissionAction;
import com.leon.agenthub.permission.enums.SecurityLevel;
import com.leon.agenthub.permission.enums.UserRole;
import com.leon.agenthub.permission.exception.PermissionDeniedException;
import com.leon.agenthub.permission.repository.AuditLogRepository;
import com.leon.agenthub.permission.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * {@link PermissionAspect} 单元测试 — 覆盖角色 × 操作 × 密级的权限判定。
 *
 * <p>重点：查看者与访客为只读角色，不能执行上传、删除等写操作。
 */
class PermissionAspectTest {

    private AuditLogRepository auditLogRepository;
    private PermissionAspect aspect;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        aspect = new PermissionAspect(new PermissionService(), auditLogRepository);
    }

    /**
     * 构造一份注解 mock。
     */
    private RequirePermission annotation(PermissionAction action, SecurityLevel minLevel) {
        RequirePermission requirePermission = mock(RequirePermission.class);
        when(requirePermission.resource()).thenReturn("DOCUMENT");
        when(requirePermission.action()).thenReturn(action);
        when(requirePermission.minLevel()).thenReturn(minLevel);
        when(requirePermission.adminOnly()).thenReturn(false);
        return requirePermission;
    }

    private void mockRole(UserRole role, MockedStatic<SecurityContextUtil> mockedStatic) {
        mockedStatic.when(SecurityContextUtil::getCurrentUserRole).thenReturn(role);
    }

    @Test
    void 查看者不能执行写操作() {
        try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
            mockRole(UserRole.VIEWER, mockedStatic);

            PermissionDeniedException ex = assertThrows(PermissionDeniedException.class,
                    () -> aspect.checkPermission(annotation(PermissionAction.WRITE, SecurityLevel.PUBLIC)));

            assertTrue(ex.getMessage().contains("只读角色"));
        }
    }

    @Test
    void 访客不能执行写操作() {
        try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
            mockRole(UserRole.GUEST, mockedStatic);

            assertThrows(PermissionDeniedException.class,
                    () -> aspect.checkPermission(annotation(PermissionAction.WRITE, SecurityLevel.PUBLIC)));
        }
    }

    @Test
    void 编辑者可以执行写操作() {
        try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
            mockRole(UserRole.EDITOR, mockedStatic);

            assertDoesNotThrow(
                    () -> aspect.checkPermission(annotation(PermissionAction.WRITE, SecurityLevel.PUBLIC)));
        }
    }

    @Test
    void 管理员可以执行写操作() {
        try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
            mockRole(UserRole.ADMIN, mockedStatic);

            assertDoesNotThrow(
                    () -> aspect.checkPermission(annotation(PermissionAction.WRITE, SecurityLevel.CONFIDENTIAL)));
        }
    }

    @Test
    void 查看者可以执行读操作() {
        try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
            mockRole(UserRole.VIEWER, mockedStatic);

            assertDoesNotThrow(
                    () -> aspect.checkPermission(annotation(PermissionAction.READ, SecurityLevel.INTERNAL)));
        }
    }

    @Test
    void 访客读取机密文档被拒绝() {
        try (MockedStatic<SecurityContextUtil> mockedStatic = mockStatic(SecurityContextUtil.class)) {
            mockRole(UserRole.GUEST, mockedStatic);

            assertThrows(PermissionDeniedException.class,
                    () -> aspect.checkPermission(annotation(PermissionAction.READ, SecurityLevel.CONFIDENTIAL)));
        }
    }
}
