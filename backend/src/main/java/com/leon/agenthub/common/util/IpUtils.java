package com.leon.agenthub.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 地址工具类 — 从 HttpServletRequest 中提取真实客户端 IP。
 *
 * <p>在反向代理（Nginx）场景下，优先从 X-Forwarded-For / X-Real-IP 头获取，
 * 回退到 request.getRemoteAddr()。
 */
public final class IpUtils {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";
    private static final String UNKNOWN = "unknown";

    private IpUtils() {
    }

    /**
     * 获取真实客户端 IP 地址。
     *
     * <p>优先级：X-Forwarded-For（取第一个） → X-Real-IP → RemoteAddr
     *
     * @param request HTTP 请求
     * @return 客户端 IP，无法获取时返回 "unknown"
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        // 1. X-Forwarded-For: client, proxy1, proxy2 — 取第一个即真实客户端 IP
        String ip = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip)) {
            int commaIndex = ip.indexOf(',');
            if (commaIndex > 0) {
                ip = ip.substring(0, commaIndex).trim();
            }
            return ip;
        }

        // 2. X-Real-IP
        ip = request.getHeader(HEADER_X_REAL_IP);
        if (ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip)) {
            return ip;
        }

        // 3. RemoteAddr（直连场景）
        return request.getRemoteAddr();
    }
}
