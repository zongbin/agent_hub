/**
 * 认证模块 — JWT 登录、Token 解析与刷新。
 *
 * <ul>
 *   <li>{@code controller}  — 登录 / 登出 / 刷新 Token 接口</li>
 *   <li>{@code service}     — JWT 生成、校验、用户详情加载</li>
 *   <li>{@code security}    — JwtFilter、SecurityContext 工具</li>
 *   <li>{@code dto}         — LoginRequest / LoginResponse / UserInfo</li>
 *   <li>{@code config}      — Spring Security / 白名单路径配置</li>
 * </ul>
 *
 * <p>MVP 仅做 JWT，SSO 集成点预留在此模块。
 */
package com.leon.agenthub.auth;
