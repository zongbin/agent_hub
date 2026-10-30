package com.leon.agenthub.common.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/**
 * 分页响应体 — 统一为 {@code {list, total, page, size}} 结构。
 *
 * <p>不直接返回 Spring Data 的 {@code Page} 对象，避免序列化结构不稳定、
 * 且与前端 el-pagination 的分页参数命名（page / size）保持一致。
 *
 * @param <T> 列表元素类型
 */
@Data
public class PageResult<T> implements Serializable {

    /** 当前页数据 */
    private List<T> list;

    /** 总条数 */
    private long total;

    /** 当前页码，从 1 开始 */
    private int page;

    /** 每页条数 */
    private int size;

    /**
     * 由 Spring Data 的 {@link Page} 转换而来。
     */
    public static <T> PageResult<T> of(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setList(page.getContent());
        result.setTotal(page.getTotalElements());
        result.setPage(page.getNumber() + 1);
        result.setSize(page.getSize());
        return result;
    }

    /**
     * 由 Spring Data 的 {@link Page} 转换而来，同时对元素做映射。
     *
     * @param page    分页结果
     * @param mapper  元素映射函数
     */
    public static <E, T> PageResult<T> of(Page<E> page, Function<E, T> mapper) {
        PageResult<T> result = new PageResult<>();
        result.setList(page.getContent().stream().map(mapper).toList());
        result.setTotal(page.getTotalElements());
        result.setPage(page.getNumber() + 1);
        result.setSize(page.getSize());
        return result;
    }
}
