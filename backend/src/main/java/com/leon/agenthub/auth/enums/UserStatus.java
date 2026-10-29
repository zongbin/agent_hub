package com.leon.agenthub.auth.enums;

import com.leon.agenthub.common.exception.BusinessException;

/**
 * 用户账号状态 — 用户启停状态常量的唯一来源。
 *
 * <p>{@code code} 为数据库 {@code users.status} 列的值（int），接口与前端仍使用该数值。
 */
public enum UserStatus {

    /** 已启用 */
    ENABLED(1, "启用"),

    /** 已禁用 */
    DISABLED(0, "禁用");

    /** 数据库存储值 */
    private final int code;

    /** 中文标签 */
    private final String label;

    UserStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 数据库存储值。
     */
    public int getCode() {
        return code;
    }

    /**
     * 中文标签。
     */
    public String getLabel() {
        return label;
    }

    /**
     * 按数据库值解析状态。
     *
     * @param code 数据库存储值
     * @return 对应状态
     * @throws BusinessException 值为空或非法时抛出
     */
    public static UserStatus fromCode(Integer code) {
        if (code != null) {
            for (UserStatus status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
        }
        throw new BusinessException(400, "非法的用户状态：" + code);
    }

    /**
     * 判断数据库值是否为启用状态。
     *
     * @param code 数据库存储值
     * @return true 表示启用
     */
    public static boolean isEnabled(Integer code) {
        return code != null && ENABLED.code == code;
    }
}
