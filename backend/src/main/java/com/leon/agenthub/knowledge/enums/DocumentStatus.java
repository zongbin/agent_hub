package com.leon.agenthub.knowledge.enums;

/**
 * 文档处理状态 — 文档状态常量的唯一来源。
 *
 * <p>约定：
 * <ul>
 *   <li>{@code code} 为数据库 {@code documents.status} 列的值（int）</li>
 *   <li>枚举名即接口返回 / 前端使用的字符串（JSON 序列化按枚举名输出）</li>
 *   <li>业务代码禁止再出现 {@code "PROCESSING"} 这类字符串字面量或 {@code 0/1/2} 魔法数字</li>
 * </ul>
 */
public enum DocumentStatus {

    /** 处理中：已上传，解析与向量化尚未完成 */
    PROCESSING(2, "处理中"),

    /** 已就绪：解析完成，可被检索 */
    ACTIVE(1, "已就绪"),

    /** 处理失败：解析或向量化异常 */
    FAILED(0, "处理失败"),

    /** 已删除：由 {@code documents.deleted} 标记派生，不写入 status 列 */
    DELETED(-1, "已删除");

    /** 数据库存储值 */
    private final int code;

    /** 中文标签 */
    private final String label;

    DocumentStatus(int code, String label) {
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
     * @return 对应状态；null 或未知值兜底为 {@link #ACTIVE}（与历史行为一致）
     */
    public static DocumentStatus fromCode(Integer code) {
        if (code == null) {
            return ACTIVE;
        }
        for (DocumentStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return ACTIVE;
    }
}
