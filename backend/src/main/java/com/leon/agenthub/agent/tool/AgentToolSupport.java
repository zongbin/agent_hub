package com.leon.agenthub.agent.tool;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * 工具公共支持类 — 文件名清洗与子目录解析。
 *
 * <p>工具参数由大模型自由生成，可能包含 {@code ../} 等路径穿越片段，
 * 因此所有落盘/读取路径都必须先经过 {@link #sanitizeFileName(String)} 清洗。
 */
public final class AgentToolSupport {

    /** 非法文件名字符（含路径分隔符与控制字符） */
    private static final Pattern ILLEGAL_CHARS = Pattern.compile("[\\\\/:*?\"<>|\\x00-\\x1f]");

    /** 单文件名最大长度 */
    private static final int MAX_FILENAME_LENGTH = 200;

    private AgentToolSupport() {
    }

    /**
     * 清洗文件名：去除路径分隔符与非法字符，截断过长名称。
     *
     * @param fileName 原始文件名（可能为 null）
     * @return 安全的纯文件名
     */
    public static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unnamed";
        }
        // 只取最后一段路径，彻底去掉目录成分
        String name = Paths.get(fileName.replace('\\', '/')).getFileName().toString();
        name = ILLEGAL_CHARS.matcher(name).replaceAll("_");
        name = name.trim().replaceAll("^\\.+", "");
        if (name.isBlank()) {
            return "unnamed";
        }
        if (name.length() > MAX_FILENAME_LENGTH) {
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0 && name.length() - dotIndex < 20) {
                String ext = name.substring(dotIndex);
                name = name.substring(0, MAX_FILENAME_LENGTH - ext.length()) + ext;
            } else {
                name = name.substring(0, MAX_FILENAME_LENGTH);
            }
        }
        return name;
    }

    /**
     * 解析工具工作目录下的子目录路径（不存在则创建）。
     *
     * @param workDir 工作根目录
     * @param subDir  子目录名
     * @return 子目录路径
     */
    public static Path resolveDir(String workDir, String subDir) {
        return Paths.get(workDir, subDir).toAbsolutePath().normalize();
    }
}
