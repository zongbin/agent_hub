package com.leon.agenthub.common.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 响应包装。
 *
 * <p>格式：{"code": 0, "message": "success", "data": {...}}
 */
@Data
public class R<T> implements Serializable {

    /** 状态码：0 成功，非 0 失败 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 0;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
