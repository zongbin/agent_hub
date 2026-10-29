package com.leon.agenthub.permission.service;

import com.leon.agenthub.permission.repository.DocPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 文档访问权限判定 — 统一角色 × 密级 + 文档级授权的访问规则。
 *
 * <p>角色规则：
 * <ul>
 *   <li>ADMIN：可访问全部密级（PUBLIC / INTERNAL / CONFIDENTIAL）</li>
 *   <li>VIEWER / EDITOR：可访问 PUBLIC 与 INTERNAL</li>
 *   <li>GUEST：仅可访问 PUBLIC</li>
 * </ul>
 *
 * <p>文档级授权：CONFIDENTIAL 文档额外允许 `doc_permissions` 中被授权的用户访问。
 *
 * <p>文档列表、详情、删除与问答检索共用此规则，避免权限逻辑漂移。
 */
@Component
@RequiredArgsConstructor
public class DocumentAccessChecker {

    private final DocPermissionRepository docPermissionRepository;

    /**
     * 仅按角色与密级判断是否可访问（不涉及文档级授权）。
     *
     * @param userRole      用户角色代码（ADMIN/EDITOR/VIEWER/GUEST）
     * @param securityLevel 文档密级（PUBLIC/INTERNAL/CONFIDENTIAL）
     * @return true 表示该角色在密级维度可访问
     */
    public boolean canAccess(String userRole, String securityLevel) {
        if ("ADMIN".equals(userRole)) {
            return true;
        }
        if ("GUEST".equals(userRole)) {
            return "PUBLIC".equals(securityLevel);
        }
        if ("VIEWER".equals(userRole) || "EDITOR".equals(userRole)) {
            return !"CONFIDENTIAL".equals(securityLevel);
        }
        return false;
    }

    /**
     * 判断用户是否可访问指定文档（角色密级 + 文档级授权）。
     *
     * <p>CONFIDENTIAL 文档在角色无权时，若 `doc_permissions` 中存在该用户的授权则放行。
     *
     * @param userId        用户 ID
     * @param userRole      用户角色代码
     * @param documentId    文档 ID
     * @param securityLevel 文档密级
     * @return true 表示可访问
     */
    public boolean canAccess(Long userId, String userRole, Long documentId, String securityLevel) {
        if (canAccess(userRole, securityLevel)) {
            return true;
        }
        if (userId == null || documentId == null || !"CONFIDENTIAL".equals(securityLevel)) {
            return false;
        }
        return docPermissionRepository.existsByDocumentIdAndGranteeId(documentId, userId);
    }
}
