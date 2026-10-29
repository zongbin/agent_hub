package com.leon.agenthub.auth.enums;

import com.leon.agenthub.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link UserStatus} 单元测试 — 覆盖 code 解析与非法值处理。
 */
class UserStatusTest {

    @Test
    void resolvesKnownCodes() {
        assertEquals(UserStatus.ENABLED, UserStatus.fromCode(1));
        assertEquals(UserStatus.DISABLED, UserStatus.fromCode(0));
    }

    @Test
    void rejectsUnknownCode() {
        assertThrows(BusinessException.class, () -> UserStatus.fromCode(5));
        assertThrows(BusinessException.class, () -> UserStatus.fromCode(null));
    }

    @Test
    void isEnabledOnlyForEnabledCode() {
        assertTrue(UserStatus.isEnabled(1));
        assertFalse(UserStatus.isEnabled(0));
        assertFalse(UserStatus.isEnabled(null));
    }
}
