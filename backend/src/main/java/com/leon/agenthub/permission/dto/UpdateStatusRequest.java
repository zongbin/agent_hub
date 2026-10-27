package com.leon.agenthub.permission.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新用户状态请求 DTO。
 */
@Data
public class UpdateStatusRequest implements Serializable {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
