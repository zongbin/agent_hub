# Design: 用户登录功能

## Context

项目当前无任何认证机制，所有接口公开访问。`users` 表已存在（含 username、password、role、department、status 字段），JWT 配置（`agenthub.jwt.*`）已在 `application.yml` 中就绪，`jjwt 0.12.6` 依赖已引入，统一响应包装 `R<T>` 和全局异常处理 `GlobalExceptionHandler` 已就绪。需要在此基础上构建完整的 JWT 认证体系。

## Goals / Non-Goals

**Goals:**
- 实现用户名密码登录，签发 JWT
- 实现 JWT 认证过滤器，保护业务接口
- 实现当前用户信息查询和登出
- 配置白名单路径放行公开接口
- 初始化管理员账号（Flyway 迁移脚本）

**Non-Goals:**
- SSO / OAuth2 集成（MVP 预留扩展点，不实现）
- Token 黑名单 / Redis 会话管理（MVP 仅客户端丢弃，预留接口）
- 注册 / 密码找回功能（MVP 仅管理员通过 SQL 创建用户）
- 多设备登录控制、Token 刷新机制

## Decisions

### 决策 1：Spring Security 过滤器链 + JWT 认证

**选择**：使用 Spring Security 6 的 `SecurityFilterChain` + 自定义 `JwtAuthenticationFilter`，过滤器在 UsernamePasswordAuthenticationFilter 之前执行。

**替代方案**：
- 手写 Servlet Filter：更灵活但需自行处理 Spring Security 集成，代码量更大
- Sa-Token / Shiro：引入额外框架，与项目 Spring Boot 3 + Java 21 体系不完全匹配

**理由**：Spring Security 是 Spring 生态原生安全框架，与 Spring Boot 3 无缝集成，过滤器链模式便于后续扩展 RBAC 权限注解。

### 决策 2：BCrypt 密码加密

**选择**：使用 `BCryptPasswordEncoder`（Spring Security 内置）进行密码哈希。

**替代方案**：
- MD5/SHA256 + Salt：需要自行管理盐值，安全性低于 BCrypt
- Argon2：安全性更高但 Spring Security 原生支持较弱

**理由**：BCrypt 自带盐值、可配置计算强度（cost factor），是 Spring Security 默认推荐的企业级密码哈希方案，满足 MVP 安全需求。

### 决策 3：JWT 工具类使用 jjwt 库

**选择**：使用项目已引入的 `jjwt 0.12.6`（api/impl/jackson 三件套）进行 JWT 签名和解析。

**理由**：依赖已存在，无需额外引入。jjwt 0.12.x 使用新 API（`Jwts.builder().subject()` 等），与项目版本一致。

### 决策 4：登录失败统一返回"用户名或密码错误"

**选择**：用户名不存在和密码错误返回相同提示信息。

**理由**：防止用户名枚举攻击，避免攻击者通过错误提示差异判断哪些用户名存在。

### 决策 5：安全配置放行策略

**选择**：以下路径放行 JWT 校验：
- `POST /api/auth/login` — 登录接口
- `/actuator/**` — 健康检查
- `/v3/api-docs/**`、`/swagger-ui/**`、`/doc.html`、`/webjars/**` — Knife4j / OpenAPI 文档
- `GET /api/auth/me` — 暂不放行（需登录后才能获取自身信息）

**理由**：登录接口必须公开；文档接口开发阶段便于调试；其余所有接口默认要求认证。

### 决策 6：初始化管理员账号

**选择**：通过 Flyway 迁移脚本 `V2__init_admin_user.sql` 插入默认管理员（用户名 `admin`，密码 `admin123`，BCrypt 哈希）。

**理由**：系统首次启动需要至少一个管理员账号；Flyway 保证幂等执行（`INSERT ... ON CONFLICT DO NOTHING`）。

## Risks / Trade-offs

- **[密码传输安全]** → 开发阶段使用 HTTP，生产环境通过 Nginx 配置 HTTPS 终止，密码在传输层加密。
- **[JWT 密钥管理]** → 当前使用 `application.yml` 中的 `JWT_SECRET` 环境变量配置，生产环境必须替换为强随机字符串。
- **[Token 无法主动失效]** → JWT 无状态设计意味着 Token 在过期前始终有效。MVP 阶段可接受；如需立即失效（如踢出用户），需后续引入 Redis 黑名单。
- **[Spring Security 默认禁用 CSRF]** → 基于 JWT 的无状态认证不需要 CSRF 防护，设计中将显式禁用 CSRF，使用 `SessionCreationPolicy.STATELESS`。
