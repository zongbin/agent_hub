package com.leon.agenthub.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA 属性转换器 — 将 String[] 与 PostgreSQL 原生 text[] 列映射。
 *
 * <p>使用 PostgreSQL 数组字面量格式：{a,b,c}。
 * 作为 Hibernate 6 原生数组支持（@JdbcTypeCode(SqlTypes.ARRAY)）的备选方案。
 */
@Converter(autoApply = false)
public class PgTextArrayConverter implements AttributeConverter<String[], String> {

    @Override
    public String convertToDatabaseColumn(String[] attribute) {
        if (attribute == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < attribute.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            // 转义特殊字符
            sb.append(attribute[i].replace("\\", "\\\\").replace("\"", "\\\""));
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty() || "{}".equals(dbData)) {
            return new String[0];
        }
        // 去除 { 和 }
        String inner = dbData.substring(1, dbData.length() - 1);
        if (inner.isEmpty()) {
            return new String[0];
        }
        return inner.split(",");
    }
}
