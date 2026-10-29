package com.leon.agenthub.knowledge.service;

import com.leon.agenthub.audit.annotation.AuditLog;
import com.leon.agenthub.auth.security.SecurityContextUtil;
import com.leon.agenthub.knowledge.dto.DocumentUploadRequest;
import com.leon.agenthub.knowledge.dto.DocumentVO;
import com.leon.agenthub.knowledge.enums.DocumentStatus;
import com.leon.agenthub.knowledge.model.Document;
import com.leon.agenthub.knowledge.repository.DocumentRepository;
import com.leon.agenthub.permission.annotation.RequirePermission;
import com.leon.agenthub.permission.enums.SecurityLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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

    /** 索引队列已满时的提示 */
    static final String QUEUE_FULL_MESSAGE = "文档索引队列已满，文档未能建立索引，请稍后重新上传";

    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;
    private final SecurityContextUtil securityContextUtil;
    private final ApplicationEventPublisher eventPublisher;
    private final DocumentIndexFailureHandler failureHandler;

    /**
     * 处理文档上传。
     *
     * @param request 上传元信息
     * @param file    上传的文件
     * @return 文档视图对象
     */
    @Transactional
    @RequirePermission(resource = "DOCUMENT", action = "WRITE", minLevel = SecurityLevel.PUBLIC)
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

        // 创建文档记录（状态：处理中）
        Document document = new Document();
        document.setTitle(request.getTitle() != null ? request.getTitle() : originalFilename);
        document.setFileKey(originalFilename);
        document.setFileType(fileType);
        document.setFileSize(file.getSize());
        document.setSecurityLevel(request.getSecurityLevel().getCode());
        document.setOwnerId(userId);
        document.setDepartment(department);
        document.setStatus(DocumentStatus.PROCESSING);
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

        // 发布上传事件（触发异步解析）—— 事务提交后再发布
        publishAfterCommit(new DocumentUploadedEvent(docId, filePath, originalFilename, fileType), docId);

        // 返回 DocumentVO
        DocumentVO vo = new DocumentVO();
        vo.setId(docId);
        vo.setTitle(document.getTitle());
        vo.setFileName(originalFilename);
        vo.setFileType(fileType);
        vo.setFileSize(file.getSize());
        vo.setSecurityLevel(document.getSecurityLevel());
        vo.setStatus(DocumentStatus.PROCESSING);
        vo.setChunkCount(0);
        vo.setCreatedAt(document.getCreatedAt());

        return vo;
    }

    /**
     * 在事务提交后再发布上传事件。
     *
     * <p>异步索引线程会立刻读取 {@code documents} 表，若在事务提交前发布，
     * 读到的可能是尚未提交的数据（PostgreSQL 默认 READ COMMITTED 下不可见），
     * 导致「Document not found」而把文档误判为失败。改为提交后发布即可消除该竞态。
     *
     * <p>若线程池队列已满导致任务被拒绝，则在事务完成后把文档标记为失败，
     * 避免它永久停留在「处理中」。
     */
    private void publishAfterCommit(DocumentUploadedEvent event, Long docId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 无事务上下文时直接发布（此时数据已提交，不存在可见性问题）
            publishSafely(event, docId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            private boolean rejected;

            @Override
            public void afterCommit() {
                rejected = !publishSafely(event, docId);
            }

            @Override
            public void afterCompletion(int status) {
                if (rejected) {
                    failureHandler.markFailed(docId, QUEUE_FULL_MESSAGE);
                }
            }
        });
    }

    /**
     * 发布事件，捕获任务被拒绝的情况。
     *
     * @return true 表示发布成功，false 表示任务被拒绝
     */
    private boolean publishSafely(DocumentUploadedEvent event, Long docId) {
        try {
            eventPublisher.publishEvent(event);
            return true;
        } catch (TaskRejectedException e) {
            log.error("Document index task rejected, queue is full: docId={}", docId, e);
            return false;
        }
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
