package com.leon.agenthub.knowledge.controller;

import com.leon.agenthub.common.response.R;
import com.leon.agenthub.knowledge.dto.DocumentChunkVO;
import com.leon.agenthub.knowledge.dto.DocumentQuery;
import com.leon.agenthub.knowledge.dto.DocumentUploadRequest;
import com.leon.agenthub.knowledge.dto.DocumentVO;
import com.leon.agenthub.knowledge.service.DocumentService;
import com.leon.agenthub.knowledge.service.DocumentUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
 *
 * <p>分层约定：本层只负责参数接收、调用业务服务、包装 {@link R} 响应；
 * 权限校验、业务编排、事务边界一律在 Service 层完成（Controller 禁止开启事务）。
 */
@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentUploadService documentUploadService;
    private final DocumentService documentService;

    /**
     * 上传文档。
     */
    @PostMapping("/upload")
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
    public R<Map<String, Object>> list(DocumentQuery query) {
        Page<DocumentVO> page = documentService.list(query);

        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getContent());
        result.put("total", page.getTotalElements());
        result.put("page", query.getPage());
        result.put("size", query.getSize());

        return R.ok(result);
    }

    /**
     * 查询文档详情。
     */
    @GetMapping("/{id}")
    public R<DocumentVO> detail(@PathVariable Long id) {
        return R.ok(documentService.detail(id));
    }

    /**
     * 查询文档的分块内容。
     */
    @GetMapping("/{id}/chunks")
    public R<List<DocumentChunkVO>> chunks(@PathVariable Long id) {
        return R.ok(documentService.listChunks(id));
    }

    /**
     * 删除文档（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return R.ok();
    }
}
