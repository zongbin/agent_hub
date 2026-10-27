package com.leon.agenthub.permission.enums;

/**
 * 用户角色枚举。
 *
 * <p>定义四种角色：ADMIN、EDITOR、VIEWER、GUEST。
 * 角色权限矩阵定义了各角色对不同安全密级文档的访问权限。
 */
public enum UserRole {

    /** 管理员 — 全部权限 */
    ADMIN("ADMIN"),

    /** 编辑者 — 读写权限，可使用 SuperAgent */
    EDITOR("EDITOR"),

    /** 查看者 — 只读权限 */
    VIEWER("VIEWER"),

    /** 访客 — 仅可访问公开文档 */
    GUEST("GUEST");

    private final String code;

    UserRole(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 根据字符串代码解析角色枚举。
     *
     * @param code 角色字符串
     * @return 对应的 UserRole
     * @throws IllegalArgumentException 未知角色值
     */
    public static UserRole fromCode(String code) {
        for (UserRole value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知角色: " + code);
    }
}
