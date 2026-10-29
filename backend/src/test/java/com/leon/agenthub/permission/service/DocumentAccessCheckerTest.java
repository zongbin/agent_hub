package com.leon.agenthub.permission.service;

import com.leon.agenthub.permission.repository.DocPermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link DocumentAccessChecker} 单元测试 — 覆盖角色 × 密级矩阵与文档级授权。
 */
class DocumentAccessCheckerTest {

    private DocPermissionRepository docPermissionRepository;
    private DocumentAccessChecker checker;

    @BeforeEach
    void setUp() {
        docPermissionRepository = mock(DocPermissionRepository.class);
        checker = new DocumentAccessChecker(docPermissionRepository);
    }

    @Test
    void adminCanAccessAllLevels() {
        assertTrue(checker.canAccess("ADMIN", "PUBLIC"));
        assertTrue(checker.canAccess("ADMIN", "INTERNAL"));
        assertTrue(checker.canAccess("ADMIN", "CONFIDENTIAL"));
    }

    @Test
    void editorCannotAccessConfidential() {
        assertTrue(checker.canAccess("EDITOR", "PUBLIC"));
        assertTrue(checker.canAccess("EDITOR", "INTERNAL"));
        assertFalse(checker.canAccess("EDITOR", "CONFIDENTIAL"));
    }

    @Test
    void viewerCannotAccessConfidential() {
        assertTrue(checker.canAccess("VIEWER", "PUBLIC"));
        assertTrue(checker.canAccess("VIEWER", "INTERNAL"));
        assertFalse(checker.canAccess("VIEWER", "CONFIDENTIAL"));
    }

    @Test
    void guestCanOnlyAccessPublic() {
        assertTrue(checker.canAccess("GUEST", "PUBLIC"));
        assertFalse(checker.canAccess("GUEST", "INTERNAL"));
        assertFalse(checker.canAccess("GUEST", "CONFIDENTIAL"));
    }

    @Test
    void unknownRoleIsDenied() {
        assertFalse(checker.canAccess("UNKNOWN", "PUBLIC"));
    }

    @Test
    void grantedUserCanAccessConfidentialDocument() {
        when(docPermissionRepository.existsByDocumentIdAndGranteeId(10L, 99L)).thenReturn(true);

        assertTrue(checker.canAccess(99L, "VIEWER", 10L, "CONFIDENTIAL"));
    }

    @Test
    void ungrantedUserCannotAccessConfidentialDocument() {
        when(docPermissionRepository.existsByDocumentIdAndGranteeId(10L, 99L)).thenReturn(false);

        assertFalse(checker.canAccess(99L, "VIEWER", 10L, "CONFIDENTIAL"));
    }

    @Test
    void grantDoesNotApplyToInternalDocumentForGuest() {
        when(docPermissionRepository.existsByDocumentIdAndGranteeId(10L, 99L)).thenReturn(true);

        assertFalse(checker.canAccess(99L, "GUEST", 10L, "INTERNAL"));
    }

    @Test
    void grantIsNotCheckedWhenRoleAlreadyAllows() {
        assertTrue(checker.canAccess(99L, "EDITOR", 10L, "INTERNAL"));
    }
}
