package com.leon.agenthub.permission.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leon.agenthub.auth.model.User;
import com.leon.agenthub.auth.repository.UserRepository;
import com.leon.agenthub.common.dto.PageQuery;
import com.leon.agenthub.common.dto.PageResult;
import com.leon.agenthub.common.exception.BusinessException;
import com.leon.agenthub.knowledge.model.Document;
import com.leon.agenthub.knowledge.repository.DocumentRepository;
import com.leon.agenthub.permission.annotation.RequirePermission;
import com.leon.agenthub.permission.converter.DocPermissionConverter;
import com.leon.agenthub.permission.dto.DocPermissionVO;
import com.leon.agenthub.permission.enums.DocumentPermissionType;
import com.leon.agenthub.permission.model.DocPermission;
import com.leon.agenthub.permission.repository.DocPermissionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文档级授权服务 — 查询和管理文档级细粒度授权。
 *
 * <p>用于机密文档的访问控制覆盖：即使用户角色不足以访问某密级文档，
 * 只要在 doc_permissions 表中有授权记录，即可放行。
 *
 * <p>所有列表查询与写操作均为管理员专属（{@code adminOnly = true}）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentPermissionService {

    private final DocPermissionRepository docPermissionRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocPermissionConverter converter;

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
        DocPermission permission = docPermissionRepository.findByDocumentAndUser(documentId, userId);
        return permission != null;
    }

    /**
     * 分页查询全部文档授权。
     *
     * @param query 分页参数
     * @return 分页授权列表（含文档标题与被授权人信息）
     */
    @Transactional(readOnly = true)
    @RequirePermission(resource = "DOCUMENT_PERMISSION", action = "READ", adminOnly = true)
    public PageResult<DocPermissionVO> listAll(PageQuery query) {
        Page<DocPermission> page = docPermissionRepository.findAllByOrderByCreatedAtDesc(query.toPageRequest());
        List<DocPermissionVO> list = enrich(page.getContent());
        PageResult<DocPermissionVO> result = new PageResult<>();
        result.setList(list);
        result.setTotal(page.getTotalElements());
        result.setPage(page.getNumber() + 1);
        result.setSize(page.getSize());
        return result;
    }

    /**
     * 查询某个文档的授权列表。
     *
     * @param documentId 文档 ID
     * @return 授权记录列表
     */
    @Transactional(readOnly = true)
    @RequirePermission(resource = "DOCUMENT_PERMISSION", action = "READ", adminOnly = true)
    public List<DocPermissionVO> listByDocument(Long documentId) {
        return enrich(docPermissionRepository.findByDocumentId(documentId));
    }

    /**
     * 授予用户文档访问权限。
     *
     * <p>若该用户已有该文档的授权记录，则更新权限类型（覆盖），避免违反 UNIQUE 约束。
     *
     * @param documentId 文档 ID
     * @param userId     被授权用户 ID
     * @param permission 权限类型（READ/WRITE）
     * @return 授权后的记录（含文档与用户信息）
     */
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission(resource = "DOCUMENT_PERMISSION", action = "WRITE", adminOnly = true)
    public DocPermissionVO grant(Long documentId, Long userId, String permission) {
        if (documentId == null || userId == null) {
            throw new BusinessException(400, "文档与用户不能为空");
        }
        DocumentPermissionType type = parsePermission(permission);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        DocPermission existing = docPermissionRepository.findByDocumentAndUser(documentId, userId);
        DocPermission saved;
        if (existing != null) {
            if (type.getCode().equals(existing.getPermission())) {
                log.debug("Document {} already granted to user {} with {}", documentId, userId, type.getCode());
                saved = existing;
            } else {
                existing.setPermission(type.getCode());
                saved = docPermissionRepository.save(existing);
                log.info("Updated document {} permission for user {} to {}", documentId, userId, type.getCode());
            }
        } else {
            DocPermission docPermission = new DocPermission();
            docPermission.setDocumentId(documentId);
            docPermission.setGranteeId(userId);
            docPermission.setPermission(type.getCode());
            saved = docPermissionRepository.save(docPermission);
            log.info("Granted document {} to user {} with permission {}", documentId, userId, type.getCode());
        }
        return converter.toVO(saved, document, user);
    }

    /**
     * 撤销用户文档访问权限。
     *
     * @param documentId 文档 ID
     * @param userId     被撤销用户 ID
     * @return true 表示成功删除
     */
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission(resource = "DOCUMENT_PERMISSION", action = "WRITE", adminOnly = true)
    public boolean revoke(Long documentId, Long userId) {
        if (documentId == null || userId == null) {
            throw new BusinessException(400, "文档与用户不能为空");
        }
        int rows = docPermissionRepository.deleteByDocumentAndUser(documentId, userId);
        log.info("Revoked document {} from user {}, affected rows: {}", documentId, userId, rows);
        return rows > 0;
    }

    /**
     * 解析权限类型，非法值转为业务异常（缺省为只读）。
     */
    private DocumentPermissionType parsePermission(String permission) {
        if (permission == null || permission.isBlank()) {
            return DocumentPermissionType.READ;
        }
        try {
            return DocumentPermissionType.fromCode(permission);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "未知权限类型：" + permission);
        }
    }

    /**
     * 批量补齐文档与用户信息，避免逐条查询（N+1）。
     */
    private List<DocPermissionVO> enrich(List<DocPermission> permissions) {
        if (permissions.isEmpty()) {
            return List.of();
        }
        Set<Long> documentIds = permissions.stream()
                .map(DocPermission::getDocumentId)
                .collect(Collectors.toSet());
        Set<Long> userIds = permissions.stream()
                .map(DocPermission::getGranteeId)
                .collect(Collectors.toSet());

        Map<Long, Document> documents = documentRepository.findAllById(documentIds).stream()
                .collect(Collectors.toMap(Document::getId, Function.identity()));
        Map<Long, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return permissions.stream()
                .map(p -> converter.toVO(p, documents.get(p.getDocumentId()), users.get(p.getGranteeId())))
                .toList();
    }
}
