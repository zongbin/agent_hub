package com.leon.agenthub.knowledge.service;

import com.leon.agenthub.audit.annotation.AuditLog;
import com.leon.agenthub.auth.security.SecurityContextUtil;
import com.leon.agenthub.knowledge.dto.DocumentUploadRequest;
import com.leon.agenthub.knowledge.dto.DocumentVO;
import com.leon.agenthub.knowledge.model.Document;
import com.leon.agenthub.knowledge.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.OffsetDateTime;

/**
 * 文档上传服务 — 编排上传流程：权限校验 → 文件存储 → 创建记录 → 发布事件。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentUploadService {

    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;
    private final SecurityContextUtil securityContextUtil;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 处理文档上传。
     *
     * @param request 上传元信息
     * @param file    上传的文件
     * @return 文档视图对象
     */
    @Transactional
    @AuditLog(action = "UPLOAD", resource = "DOCUMENT")
    public DocumentVO upload(DocumentUploadRequest request, MultipartFile file) {
        // 获取当前用户信息
        Long userId = securityContextUtil.getCurrentUserId();
        String department = securityContextUtil.getCurrentUserDepartment();
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unnamed";
        }

        // 提取文件类型
        String fileType = extractFileType(originalFilename);

        // 创建文档记录（状态：2 = PROCESSING）
        Document document = new Document();
        document.setTitle(request.getTitle() != null ? request.getTitle() : originalFilename);
        document.setFileKey(originalFilename);
        document.setFileType(fileType);
        document.setFileSize(file.getSize());
        document.setSecurityLevel(request.getSecurityLevel().getCode());
        document.setOwnerId(userId);
        document.setDepartment(department);
        document.setStatus(2); // PROCESSING
        document.setChunkCount(0);
        document.setTags(new String[0]);
        document.setDeleted(0);
        document.setCreatedAt(OffsetDateTime.now());
        document.setUpdatedAt(OffsetDateTime.now());

        documentRepository.save(document);
        Long docId = document.getId();
        log.info("Document record created: docId={}, title={}", docId, document.getTitle());

        // 存储文件
        Path filePath = fileStorageService.store(file, docId);

        // 发布上传事件（触发异步解析）
        eventPublisher.publishEvent(new DocumentUploadedEvent(docId, filePath, originalFilename, fileType));

        // 返回 DocumentVO
        DocumentVO vo = new DocumentVO();
        vo.setId(docId);
        vo.setTitle(document.getTitle());
        vo.setFileName(originalFilename);
        vo.setFileType(fileType);
        vo.setFileSize(file.getSize());
        vo.setSecurityLevel(document.getSecurityLevel());
        vo.setStatus("PROCESSING");
        vo.setChunkCount(0);
        vo.setCreatedAt(document.getCreatedAt());

        return vo;
    }

    /**
     * 从文件名提取文件类型。
     */
    private String extractFileType(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "unknown";
    }
}
