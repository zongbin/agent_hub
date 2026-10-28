package com.leon.agenthub.common.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JPA 属性转换器 — 将 String[] 与 PostgreSQL jsonb 列正确映射。
 *
 * <p>用于标签数组等 JSON 数组类型的持久化：Java 端为 String[]，PG 端为 jsonb。
 */
@Converter(autoApply = false)
public class JsonStringArrayConverter implements AttributeConverter<String[], String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(String[] attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize String[] to JSON", e);
        }
    }

    @Override
    public String[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return new String[0];
        }
        try {
            List<?> list = OBJECT_MAPPER.readValue(dbData, List.class);
            return list.stream().map(Object::toString).toArray(String[]::new);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse JSON to String[]", e);
        }
    }
}
