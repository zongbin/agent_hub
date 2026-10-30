package com.leon.agenthub.permission.enums;

/**
 * 文档级授权类型枚举 — 对应 doc_permissions.permission 列。
 *
 * <p>用于机密文档的细粒度授权：READ 表示只读访问，WRITE 表示可写。
 */
public enum DocumentPermissionType {

    /** 只读 */
    READ("READ", "只读"),

    /** 读写 */
    WRITE("WRITE", "读写");

    private final String code;
    private final String label;

    DocumentPermissionType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 数据库存储值 / 接口出入参值。
     */
    public String getCode() {
        return code;
    }

    /**
     * 中文标签。
     */
    public String getLabel() {
        return label;
    }

    /**
     * 根据字符串代码解析授权类型。
     *
     * @param code 权限字符串（READ / WRITE）
     * @return 对应的授权类型
     * @throws IllegalArgumentException 未知权限值
     */
    public static DocumentPermissionType fromCode(String code) {
        for (DocumentPermissionType value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知文档权限类型: " + code);
    }
}
