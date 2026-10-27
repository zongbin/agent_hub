package com.leon.agenthub.permission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 角色信息 DTO — 权限管理接口返回的角色详情。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleInfo implements Serializable {

    /** 角色代码：ADMIN/EDITOR/VIEWER/GUEST */
    private String code;

    /** 角色显示名称 */
    private String name;

    /** 角色描述 */
    private String description;
}
