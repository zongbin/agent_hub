package com.leon.agenthub.common.dto;

import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.io.Serializable;

/**
 * 分页查询基类 — 所有分页查询参数 DTO 必须继承此类。
 *
 * <p>统一分页语义：
 * <ul>
 *   <li>{@code page} 从 <b>1</b> 开始（与前端 el-pagination 的 current-page 一致），服务层用 {@link #getPageIndex()} 转成 0 基</li>
 *   <li>{@code size} 为每页条数，默认 {@value #DEFAULT_SIZE}，上限 {@value #MAX_SIZE}（防止超大分页拖垮数据库）</li>
 * </ul>
 *
 * <p>示例：
 * <pre>{@code
 * @Data
 * @EqualsAndHashCode(callSuper = true)
 * public class DocumentQuery extends PageQuery {
 *     private String securityLevel;
 * }
 * }</pre>
 */
@Data
public class PageQuery implements Serializable {

    /** 默认每页条数 */
    public static final int DEFAULT_SIZE = 20;

    /** 每页条数上限 */
    public static final int MAX_SIZE = 200;

    /** 页码，从 1 开始 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = DEFAULT_SIZE;

    /**
     * 0 基页码（Spring Data 的 Pageable 语义）。
     */
    public int getPageIndex() {
        return page == null || page < 1 ? 0 : page - 1;
    }

    /**
     * 每页条数（已做默认值与上限保护）。
     */
    public int getPageSize() {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /**
     * 构造无排序的分页参数。
     */
    public PageRequest toPageRequest() {
        return PageRequest.of(getPageIndex(), getPageSize());
    }

    /**
     * 构造带排序的分页参数。
     *
     * @param sort 排序规则
     */
    public PageRequest toPageRequest(Sort sort) {
        return PageRequest.of(getPageIndex(), getPageSize(), sort);
    }
}
