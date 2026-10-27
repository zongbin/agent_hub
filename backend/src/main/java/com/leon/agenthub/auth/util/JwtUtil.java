package com.leon.agenthub.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类 — Token 生成、解析、校验。
 *
 * <p>使用 jjwt 0.12.x API，HS256 签名算法。
 */
@Slf4j
@Component
public class JwtUtil {

    /** JWT 签名密钥（从配置读取） */
    @Value("${agenthub.jwt.secret}")
    private String secret;

    /** Token 过期时间（毫秒），默认 7 天 */
    @Value("${agenthub.jwt.expiration-hours:168}")
    private long expirationHours;

    /** 请求头名称 */
    @Value("${agenthub.jwt.header:Authorization}")
    private String header;

    /** Token 前缀 */
    @Value("${agenthub.jwt.prefix:Bearer }")
    private String prefix;

    /** 根据配置密钥生成 SecretKey */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 过期时间（毫秒） */
    private long getExpirationMillis() {
        return expirationHours * 60 * 60 * 1000;
    }

    /**
     * 生成 JWT Token。
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param role     角色
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + getExpirationMillis());

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 解析 JWT Token，返回 Claims。
     *
     * @param token JWT 字符串
     * @return Claims 对象
     * @throws JwtException Token 无效或过期时抛出
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验 JWT Token 是否有效。
     *
     * @param token JWT 字符串
     * @return true 表示有效，false 表示无效或过期
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从请求头中提取 Token（去除前缀）。
     *
     * @param headerValue Authorization 头值
     * @return 纯 Token 字符串，格式不符返回 null
     */
    public String extractToken(String headerValue) {
        if (headerValue == null || !headerValue.startsWith(prefix)) {
            return null;
        }
        return headerValue.substring(prefix.length()).trim();
    }

    /** 获取过期时间（小时） */
    public long getExpirationHours() {
        return expirationHours;
    }

    /** 获取请求头名称 */
    public String getHeader() {
        return header;
    }

    /** 获取 Token 前缀 */
    public String getPrefix() {
        return prefix;
    }
}
