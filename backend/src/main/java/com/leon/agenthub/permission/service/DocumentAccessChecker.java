package com.leon.agenthub.permission.service;

import com.leon.agenthub.permission.enums.DocumentPermissionType;
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
 * <p>文档级授权：{@code doc_permissions} 中的记录是管理员的显式授权，
 * 对<b>任意密级</b>的文档都生效（与向量检索的过滤规则保持一致）：
 * <ul>
 *   <li>READ 授权 → 仅可读（查看、检索问答）</li>
 *   <li>WRITE 授权 → 可读可写（删除等写操作）</li>
 * </ul>
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
     * <p>角色无权访问时，若 {@code doc_permissions} 中存在该用户的授权记录则放行
     * （READ 与 WRITE 授权都提供读权限）。
     *
     * @param userId        用户 ID
     * @param userRole      当前用户角色代码
     * @param documentId    文档 ID
     * @param securityLevel 文档密级
     * @return true 表示可访问
     */
    public boolean canAccess(Long userId, String userRole, Long documentId, String securityLevel) {
        if (canAccess(userRole, securityLevel)) {
            return true;
        }
        if (userId == null || documentId == null) {
            return false;
        }
        return docPermissionRepository.existsByDocumentIdAndGranteeId(documentId, userId);
    }

    /**
     * 判断用户是否可写指定文档（角色写权限 + WRITE 级文档授权）。
     *
     * <p>判定顺序：
     * <ol>
     *   <li>ADMIN —— 全部可写</li>
     *   <li>只读角色（VIEWER / GUEST）—— 一律不可写，即使持有该文档的 WRITE 授权</li>
     *   <li>持有该文档的 <b>WRITE</b> 授权 —— 管理员显式授予写权限</li>
     *   <li>角色本身对该密级有写权限（EDITOR 对 PUBLIC / INTERNAL）</li>
     * </ol>
     *
     * <p>只读（READ）授权不会带来任何写权限。
     *
     * @param userId        用户 ID
     * @param userRole      当前用户角色代码
     * @param documentId    文档 ID
     * @param securityLevel 文档密级
     * @return true 表示可写
     */
    public boolean canWrite(Long userId, String userRole, Long documentId, String securityLevel) {
        if ("ADMIN".equals(userRole)) {
            return true;
        }
        // 只读角色永远不可写 —— 角色矩阵优先于文档级授权
        if (!isWriteRole(userRole)) {
            return false;
        }
        if (userId != null && documentId != null
                && docPermissionRepository.existsByDocumentIdAndGranteeIdAndPermission(
                        documentId, userId, DocumentPermissionType.WRITE.getCode())) {
            return true;
        }
        return canWriteByRole(userRole, securityLevel);
    }

    /**
     * 该角色是否具备写权限（查看者与访客为只读角色）。
     */
    public boolean isWriteRole(String userRole) {
        return "ADMIN".equals(userRole) || "EDITOR".equals(userRole);
    }

    /**
     * 角色本身是否对该密级有写权限（与 {@link PermissionService#canPerformAction} 的角色语义对齐）。
     */
    private boolean canWriteByRole(String userRole, String securityLevel) {
        return "EDITOR".equals(userRole) && !"CONFIDENTIAL".equals(securityLevel);
    }
}
