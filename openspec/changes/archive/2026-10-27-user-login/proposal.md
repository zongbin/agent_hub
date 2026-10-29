# Proposal: 用户登录功能

## Why

当前项目尚未实现用户认证功能，所有接口均无保护，任何人均可直接访问。作为企业知识管理平台，必须先建立身份认证机制，才能保障后续权限控制、审计日志和数据安全。MVP 技术设计中将"用户认证（JWT）"列为 P0 基础设施，是后续所有功能的前置依赖。

## What Changes

- 新增 `spring-boot-starter-security` 依赖，引入 Spring Security 安全过滤器链
- 新增 BCrypt 密码加密（`spring-security-crypto`），用户密码以哈希形式存储
- 新增 `auth` 模块下的 Controller、Service、DTO、JWT 工具类等完整登录体系
- 新增登录接口 `POST /api/auth/login`，校验用户名密码后签发 JWT
- 新增登出接口 `POST /api/auth/logout`（客户端丢弃 Token 即可，预留黑名单扩展点）
- 新增当前用户信息接口 `GET /api/auth/me`，返回登录用户的基本信息和角色
- 新增 JWT 认证过滤器 `JwtAuthenticationFilter`，拦截受保护请求并解析 Token
- 新增安全配置 `SecurityConfig`，配置白名单路径（登录、健康检查、Knife4j 文档等）
- 数据库 `users` 表已存在，无需新建；初始化一个管理员账号的 Flyway 迁移脚本

## Capabilities

### New Capabilities

- `user-auth`：用户认证能力，涵盖 JWT 登录签发、Token 校验过滤器、当前用户信息获取、登出机制，以及白名单路径的安全放行策略。该能力作为系统权限体系的基础，后续权限校验（RBAC）将在其上构建。

### Modified Capabilities

（无）

## Impact

- **代码影响**：新增 `backend/src/main/java/com/leon/agenthub/auth/` 下的多个类；新增 Flyway 迁移脚本 `V2__init_admin_user.sql`
- **依赖影响**：新增 `spring-boot-starter-security`（含 spring-security-crypto 用于 BCrypt）
- **配置影响**：`application.yml` 无需改动，JWT 配置已就绪
- **API 影响**：新增 3 个认证端点（login / logout / me），其余所有业务端点将受 JWT 过滤器保护
- **安全影响**：所有 `/api/` 路径（除白名单外）将要求携带有效 JWT，未登录请求返回 401
