package com.leon.agenthub.permission.service;

import java.util.List;

import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import com.leon.agenthub.permission.enums.SecurityLevel;
import com.leon.agenthub.permission.enums.UserRole;
import com.leon.agenthub.permission.model.DocPermission;
import com.leon.agenthub.permission.repository.DocPermissionRepository;

import lombok.RequiredArgsConstructor;

/**
 * 文档访问过滤器 — 构造向量检索的「角色×密级 + 文档级授权」过滤表达式。
 *
 * <p>所有需要按用户身份过滤向量检索的场景（知识问答、Agent 工具调用）统一走这里，
 * 避免各处重复实现权限规则。
 */
@Component
@RequiredArgsConstructor
public class DocumentAccessFilter {

    private final DocPermissionRepository docPermissionRepository;

    /**
     * 构造检索过滤表达式。
     *
     * @param userRole 当前用户角色
     * @param userId   当前用户 ID（用于文档级授权判断）
     * @return 过滤表达式；管理员返回 null，表示不做任何过滤
     */
    public Filter.Expression buildFilter(UserRole userRole, Long userId) {
        if (userRole == UserRole.ADMIN) {
            return null;
        }
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op roleFilter = switch (userRole) {
            case GUEST -> b.in("securityLevel", SecurityLevel.PUBLIC.getCode());
            default -> b.in("securityLevel", SecurityLevel.PUBLIC.getCode(), SecurityLevel.INTERNAL.getCode());
        };
        List<Long> grantedIds = loadGrantedDocumentIds(userId);
        if (grantedIds.isEmpty()) {
            return roleFilter.build();
        }
        return b.or(roleFilter, b.in("documentId", grantedIds.toArray())).build();
    }

    /**
     * 查找用户被单独授权的文档 ID（doc_permissions 中按用户授权的记录）。
     */
    private List<Long> loadGrantedDocumentIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return docPermissionRepository.findByGranteeId(userId).stream()
                .map(DocPermission::getDocumentId)
                .distinct()
                .toList();
    }
}
