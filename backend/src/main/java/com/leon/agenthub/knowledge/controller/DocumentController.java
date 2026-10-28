package com.leon.agenthub.knowledge.controller;

import com.leon.agenthub.auth.security.SecurityContextUtil;
import com.leon.agenthub.common.response.R;
import com.leon.agenthub.knowledge.dto.DocumentQuery;
import com.leon.agenthub.knowledge.dto.DocumentUploadRequest;
import com.leon.agenthub.knowledge.dto.DocumentVO;
import com.leon.agenthub.knowledge.model.Document;
import com.leon.agenthub.knowledge.repository.DocumentRepository;
import com.leon.agenthub.knowledge.service.DocumentUploadService;
import com.leon.agenthub.permission.annotation.RequirePermission;
import com.leon.agenthub.permission.enums.SecurityLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档控制器 — 文档上传、列表、详情、删除接口。
 */
@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentUploadService documentUploadService;
    private final DocumentRepository documentRepository;
    private final SecurityContextUtil securityContextUtil;

    /**
     * 上传文档。
     */
    @PostMapping("/upload")
    @RequirePermission(resource = "DOCUMENT", action = "WRITE", minLevel = SecurityLevel.PUBLIC)
    public R<DocumentVO> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") DocumentUploadRequest request) {
        DocumentVO vo = documentUploadService.upload(request, file);
        return R.ok(vo);
    }

    /**
     * 分页查询文档列表。
     */
    @GetMapping
    @RequirePermission(resource = "DOCUMENT", action = "READ")
    public R<Map<String, Object>> list(DocumentQuery query) {
        Long userId = securityContextUtil.getCurrentUserId();
        String userRole = securityContextUtil.getCurrentUserRole().getCode();

        PageRequest pageRequest = PageRequest.of(query.getPage() - 1, query.getSize());
        Page<Document> page = documentRepository.findPageWithPermission(
                query.getSecurityLevel(), query.getStatus(),
                userRole, pageRequest
        );

        // 转换为 VO
        List<DocumentVO> voList = page.getContent().stream().map(this::toVO).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("list", voList);
        result.put("total", page.getTotalElements());
        result.put("page", query.getPage());
        result.put("size", query.getSize());

        return R.ok(result);
    }

    /**
     * 查询文档详情。
     */
    @GetMapping("/{id}")
    @RequirePermission(resource = "DOCUMENT", action = "READ")
    public R<DocumentVO> detail(@PathVariable Long id) {
        Document document = documentRepository.findById(id).orElse(null);
        if (document == null) {
            return R.fail(404, "文档不存在");
        }

        // 权限检查
        Long userId = securityContextUtil.getCurrentUserId();
        String userRole = securityContextUtil.getCurrentUserRole().getCode();
        if (!hasDocumentAccess(document, userId, userRole)) {
            return R.fail(403, "权限不足，无法访问该文档");
        }

        return R.ok(toVO(document));
    }

    /**
     * 删除文档（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    @RequirePermission(resource = "DOCUMENT", action = "WRITE")
    public R<Void> delete(@PathVariable Long id) {
        Document document = documentRepository.findById(id).orElse(null);
        if (document == null) {
            return R.fail(404, "文档不存在");
        }

        // 权限检查
        Long userId = securityContextUtil.getCurrentUserId();
        String userRole = securityContextUtil.getCurrentUserRole().getCode();
        if (!hasDocumentAccess(document, userId, userRole)) {
            return R.fail(403, "权限不足，无法删除该文档");
        }

        // 软删除：标记 deleted=1
        document.setDeleted(1);
        documentRepository.save(document);
        log.info("Document {} deleted (soft)", id);
        return R.ok();
    }

    /**
     * 将 Document 实体转换为 DocumentVO。
     */
    private DocumentVO toVO(Document doc) {
        DocumentVO vo = new DocumentVO();
        vo.setId(doc.getId());
        vo.setTitle(doc.getTitle());
        vo.setFileName(doc.getFileKey());
        vo.setFileType(doc.getFileType());
        vo.setFileSize(doc.getFileSize());
        vo.setSecurityLevel(doc.getSecurityLevel());
        vo.setSummary(doc.getSummary());
        vo.setTags(doc.getTags());
        vo.setChunkCount(doc.getChunkCount());
        vo.setCreatedAt(doc.getCreatedAt());

        // 状态映射
        if (doc.getStatus() == 2) {
            vo.setStatus("PROCESSING");
        } else if (doc.getStatus() == 0 && doc.getDeleted() == 0) {
            vo.setStatus("FAILED");
        } else if (doc.getDeleted() == 1) {
            vo.setStatus("DELETED");
        } else {
            vo.setStatus("ACTIVE");
        }

        return vo;
    }

    /**
     * 检查用户是否有文档访问权限。
     */
    private boolean hasDocumentAccess(Document document, Long userId, String userRole) {
        if ("ADMIN".equals(userRole)) {
            return true;
        }
        if ("GUEST".equals(userRole)) {
            return "PUBLIC".equals(document.getSecurityLevel());
        }
        if ("VIEWER".equals(userRole) || "EDITOR".equals(userRole)) {
            return !"CONFIDENTIAL".equals(document.getSecurityLevel());
        }
        return false;
    }
}
