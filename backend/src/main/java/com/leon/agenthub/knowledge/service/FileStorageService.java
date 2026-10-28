package com.leon.agenthub.knowledge.service;

import com.leon.agenthub.common.exception.BusinessException;
import com.leon.agenthub.knowledge.config.KnowledgeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

/**
 * 文件存储服务 — 将上传的文件保存到本地文件系统。
 *
 * <p>存储路径结构：{rootPath}/{docId}/{filename}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    /** Windows / Linux 均不允许的文件名字符（含路径分隔符与控制字符） */
    private static final Pattern ILLEGAL_CHARS = Pattern.compile("[\\\\/:*?\"<>|\\x00-\\x1f]");

    /** Windows 保留设备名（大小写不敏感） */
    private static final Pattern RESERVED_NAME = Pattern.compile(
            "^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?$", Pattern.CASE_INSENSITIVE);

    /** 单文件名最大长度（留余量，避免触及 Windows MAX_PATH 260） */
    private static final int MAX_FILENAME_LENGTH = 200;

    private final KnowledgeProperties knowledgeProperties;

    /**
     * 存储上传文件。
     *
     * @param file  上传的文件
     * @param docId 文档 ID
     * @return 存储后的文件完整路径
     */
    public Path store(MultipartFile file, Long docId) {
        String rootPath = knowledgeProperties.getStorage().getRootPath();
        String originalFilename = file.getOriginalFilename();
        String safeFilename = sanitizeFilename(originalFilename);

        Path targetDir = Paths.get(rootPath, String.valueOf(docId));
        Path targetPath = targetDir.resolve(safeFilename);

        try {
            Files.createDirectories(targetDir);
            // 使用 Files.copy 替代 transferTo，提供更明确的错误语义，并支持原子替换
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("File stored: docId={}, original={}, path={}", docId, originalFilename, targetPath);
            return targetPath;
        } catch (IOException e) {
            log.error("Failed to store file: docId={}, filename={}, path={}", docId, originalFilename, targetPath, e);
            throw new BusinessException(500, "文件存储失败: " + e.getMessage());
        }
    }

    /**
     * 获取已存储文件的路径。
     *
     * @param docId    文档 ID
     * @param filename 文件名
     * @return 文件完整路径
     */
    public Path getFilePath(Long docId, String filename) {
        String rootPath = knowledgeProperties.getStorage().getRootPath();
        return Paths.get(rootPath, String.valueOf(docId), filename);
    }

    /**
     * 清洗文件名，移除非法字符、截断过长名称，保证跨平台安全。
     *
     * @param filename 原始文件名（可能为 null）
     * @return 安全的文件名
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed";
        }

        // 提取纯文件名（去除路径注入，如 "../../etc/passwd"）
        String name = Paths.get(filename).getFileName().toString();

        // 替换非法字符为下划线
        name = ILLEGAL_CHARS.matcher(name).replaceAll("_");

        // 去除首尾空格与点号（避免隐藏文件或空名）
        name = name.trim().replaceAll("^\\.+", "");

        // 如果清洗后为空，使用默认名
        if (name.isBlank()) {
            return "unnamed";
        }

        // 检查 Windows 保留设备名
        if (RESERVED_NAME.matcher(name).matches()) {
            name = "_" + name;
        }

        // 截断过长文件名（保留扩展名）
        if (name.length() > MAX_FILENAME_LENGTH) {
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex > name.length() - 20) {
                // 有合理扩展名：保留扩展名
                String ext = name.substring(dotIndex);
                name = name.substring(0, MAX_FILENAME_LENGTH - ext.length()) + ext;
            } else {
                name = name.substring(0, MAX_FILENAME_LENGTH);
            }
        }

        return name;
    }
}
