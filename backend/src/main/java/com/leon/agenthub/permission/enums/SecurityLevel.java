package com.leon.agenthub.permission.enums;

/**
 * 文档安全密级枚举。
 *
 * <p>定义三级密级：PUBLIC（公开）、INTERNAL（内部）、CONFIDENTIAL（机密）。
 * 角色权限矩阵基于此密级进行访问控制。
 */
public enum SecurityLevel {

    /** 公开 — 所有人可访问（含访客） */
    PUBLIC("PUBLIC", 0),

    /** 内部 — 仅正式员工可访问 */
    INTERNAL("INTERNAL", 1),

    /** 机密 — 仅授权人员可访问 */
    CONFIDENTIAL("CONFIDENTIAL", 2);

    private final String code;
    private final int level;

    SecurityLevel(String code, int level) {
        this.code = code;
        this.level = level;
    }

    public String getCode() {
        return code;
    }

    public int getLevel() {
        return level;
    }

    /**
     * 根据字符串代码解析密级枚举。
     *
     * @param code 密级字符串
     * @return 对应的 SecurityLevel
     * @throws IllegalArgumentException 未知密级值
     */
    public static SecurityLevel fromCode(String code) {
        for (SecurityLevel value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知安全密级: " + code);
    }

    /**
     * 判断当前密级是否高于或等于目标密级。
     *
     * @param other 目标密级
     * @return true 表示当前密级更高或相等
     */
    public boolean isHigherOrEqualTo(SecurityLevel other) {
        return this.level >= other.level;
    }
}
