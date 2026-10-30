package com.leon.agenthub.permission.converter;

import org.springframework.stereotype.Component;

import com.leon.agenthub.auth.model.User;
import com.leon.agenthub.knowledge.model.Document;
import com.leon.agenthub.permission.dto.DocPermissionVO;
import com.leon.agenthub.permission.enums.DocumentPermissionType;
import com.leon.agenthub.permission.model.DocPermission;

/**
 * 文档级授权转换器 — 授权实体 → 视图对象。
 */
@Component
public class DocPermissionConverter {

    /** 文档已被删除时的占位标题 */
    private static final String DELETED_DOCUMENT = "文档已删除";

    /**
     * 授权实体 + 关联文档/用户 → 视图对象。
     *
     * @param entity   授权记录
     * @param document 关联文档（可为 null）
     * @param user     被授权人（可为 null）
     * @return 视图对象
     */
    public DocPermissionVO toVO(DocPermission entity, Document document, User user) {
        DocPermissionVO vo = new DocPermissionVO();
        vo.setId(entity.getId());
        vo.setDocumentId(entity.getDocumentId());
        vo.setDocumentTitle(document == null ? DELETED_DOCUMENT : document.getTitle());
        vo.setDocumentSecurityLevel(document == null ? null : document.getSecurityLevel());
        vo.setGranteeId(entity.getGranteeId());
        vo.setGranteeName(user == null ? null : user.getRealName());
        vo.setGranteeUsername(user == null ? null : user.getUsername());
        vo.setGranteeRole(user == null ? null : user.getRole());
        vo.setPermission(entity.getPermission());
        vo.setPermissionLabel(permissionLabel(entity.getPermission()));
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    /**
     * 权限类型中文标签（未知类型原样返回）。
     */
    private String permissionLabel(String permission) {
        try {
            return DocumentPermissionType.fromCode(permission).getLabel();
        } catch (IllegalArgumentException e) {
            return permission;
        }
    }
}
