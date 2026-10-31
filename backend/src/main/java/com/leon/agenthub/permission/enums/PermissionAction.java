package com.leon.agenthub.permission.enums;

/**
 * 权限操作类型枚举 — 对应 {@code @RequirePermission} 注解的 {@code action} 属性。
 *
 * <p>角色 × 操作的判定规则由 {@code PermissionService#canPerformAction} 统一实现：
 * <ul>
 *   <li>ADMIN — 全部操作</li>
 *   <li>EDITOR — 读写（机密文档只读）</li>
 *   <li>VIEWER / GUEST — 只读，任何写操作一律拒绝</li>
 * </ul>
 */
public enum PermissionAction {

    /** 读操作（查看、检索、列表） */
    READ("READ", "读取"),

    /** 写操作（上传、更新、授权等） */
    WRITE("WRITE", "写入"),

    /** 删除操作 */
    DELETE("DELETE", "删除");

    private final String code;
    private final String label;

    PermissionAction(String code, String label) {
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
     * 是否为写操作（写操作对只读角色一律拒绝）。
     */
    public boolean isWrite() {
        return this != READ;
    }
}
