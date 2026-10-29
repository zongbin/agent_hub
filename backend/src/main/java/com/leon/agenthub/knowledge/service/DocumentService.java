package com.leon.agenthub.knowledge.service;

import com.leon.agenthub.audit.annotation.AuditLog;
import com.leon.agenthub.auth.security.SecurityContextUtil;
import com.leon.agenthub.common.exception.BusinessException;
import com.leon.agenthub.knowledge.converter.DocumentConverter;
import com.leon.agenthub.knowledge.dto.DocumentQuery;
import com.leon.agenthub.knowledge.dto.DocumentVO;
import com.leon.agenthub.knowledge.model.Document;
import com.leon.agenthub.knowledge.repository.DocumentRepository;
import com.leon.agenthub.permission.annotation.RequirePermission;
import com.leon.agenthub.permission.service.DocumentAccessChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文档管理服务 — 文档列表、详情、删除等业务逻辑。
 *
 * <p>分层约定：Controller 只做参数接收与响应包装，所有业务编排与事务边界都收敛在本层。
 *
 * <p>事务约定：删除文档时「软删除标记」与「向量分块清理」必须处于同一事务，
 * 任一步失败则整体回滚，避免出现「文档已标记删除但仍可被检索命中」的中间态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final SecurityContextUtil securityContextUtil;
    private final DocumentAccessChecker documentAccessChecker;
    private final VectorStoreService vectorStoreService;

    /**
     * 分页查询文档列表（带权限过滤）。
     *
     * @param query 查询条件（密级、状态、分页）
     * @return 当前用户可见的文档分页
     */
    @RequirePermission(resource = "DOCUMENT", action = "READ")
    public Page<DocumentVO> list(DocumentQuery query) {
        Long userId = securityContextUtil.getCurrentUserId();
        String userRole = securityContextUtil.getCurrentUserRole().getCode();

        int page = query.getPageIndex();
        int size = query.getPageSize();
        PageRequest pageRequest = PageRequest.of(page, size);

        Page<Document> result = documentRepository.findPageWithPermission(
                query.getSecurityLevel(),
                query.getStatus() == null ? null : query.getStatus().getCode(),
                userId, userRole, pageRequest);
        return result.map(DocumentConverter::toVO);
    }

    /**
     * 查询文档详情。
     *
     * @param id 文档 ID
     * @return 文档视图对象
     */
    @RequirePermission(resource = "DOCUMENT", action = "READ")
    public DocumentVO detail(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));

        checkAccess(document, "访问");
        return DocumentConverter.toVO(document);
    }

    /**
     * 删除文档（逻辑删除 + 同步清理向量分块）。
     *
     * <p>整个流程在单一事务内完成：软删除标记与 vector_store 分块删除要么同时成功、要么同时回滚。
     *
     * @param id 文档 ID
     */
    @Transactional(rollbackFor = Exception.class)
    @RequirePermission(resource = "DOCUMENT", action = "WRITE")
    @AuditLog(action = "DELETE", resource = "DOCUMENT")
    public void delete(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));

        checkAccess(document, "删除");

        // 软删除：标记 deleted=1
        document.setDeleted(1);
        documentRepository.save(document);

        // 同步删除 vector_store 中的分块，避免已删文档继续被检索命中
        vectorStoreService.deleteByDocumentId(id);

        log.info("Document {} deleted (soft) and vector chunks removed", id);
    }

    /**
     * 文档级访问校验：角色密级 + 文档级授权。
     */
    private void checkAccess(Document document, String action) {
        Long userId = securityContextUtil.getCurrentUserId();
        String userRole = securityContextUtil.getCurrentUserRole().getCode();
        if (!documentAccessChecker.canAccess(userId, userRole, document.getId(), document.getSecurityLevel())) {
            throw new BusinessException(403, "权限不足，无法" + action + "该文档");
        }
    }
}
