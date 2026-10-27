package com.leon.agenthub.permission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leon.agenthub.permission.mapper.DocPermissionMapper;
import com.leon.agenthub.permission.model.DocPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文档级授权服务 — 查询和管理文档级细粒度授权。
 *
 * <p>用于机密文档的访问控制覆盖：即使用户角色不足以访问某密级文档，
 * 只要在 doc_permissions 表中有授权记录，即可放行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentPermissionService {

    private final DocPermissionMapper docPermissionMapper;

    /**
     * 检查用户是否拥有指定文档的访问权限。
     *
     * @param documentId 文档 ID
     * @param userId     用户 ID
     * @return true 表示用户被授权访问该文档
     */
    public boolean isDocumentGranted(Long documentId, Long userId) {
        if (documentId == null || userId == null) {
            return false;
        }
        DocPermission permission = docPermissionMapper.selectByDocumentAndUser(documentId, userId);
        return permission != null;
    }

    /**
     * 授予用户文档访问权限。
     *
     * <p>若该用户已有该文档的授权记录，则更新权限类型（覆盖），避免违反 UNIQUE 约束。
     *
     * @param documentId 文档 ID
     * @param userId     用户 ID
     * @param permission 权限类型（READ/WRITE）
     * @return 创建或更新后的授权记录
     */
    public DocPermission grantDocument(Long documentId, Long userId, String permission) {
        DocPermission existing = docPermissionMapper.selectByDocumentAndUser(documentId, userId);
        if (existing != null) {
            existing.setPermission(permission);
            docPermissionMapper.updateById(existing);
            log.info("Updated document {} permission for user {} to {}", documentId, userId, permission);
            return existing;
        }
        DocPermission docPermission = new DocPermission();
        docPermission.setDocumentId(documentId);
        docPermission.setGranteeId(userId);
        docPermission.setPermission(permission);
        docPermissionMapper.insert(docPermission);
        log.info("Granted document {} to user {} with permission {}", documentId, userId, permission);
        return docPermission;
    }

    /**
     * 查询文档的授权列表。
     *
     * @param documentId 文档 ID
     * @return 授权记录列表
     */
    public List<DocPermission> listByDocument(Long documentId) {
        return docPermissionMapper.selectList(
                new LambdaQueryWrapper<DocPermission>().eq(DocPermission::getDocumentId, documentId)
        );
    }

    /**
     * 撤销用户文档访问权限。
     *
     * @param documentId 文档 ID
     * @param userId     用户 ID
     * @return true 表示成功删除
     */
    public boolean revokeDocument(Long documentId, Long userId) {
        int rows = docPermissionMapper.deleteByDocumentAndUser(documentId, userId);
        log.info("Revoked document {} from user {}, affected rows: {}", documentId, userId, rows);
        return rows > 0;
    }
}
