# Tasks

## 1. 依赖与数据库准备

- [x] 1.1 在 `pom.xml` 中添加 `spring-boot-starter-security` 依赖，验证 `mvn dependency:resolve` 成功
- [x] 1.2 创建 Flyway 迁移脚本 `V2__init_admin_user.sql`，插入默认管理员账号（username=`admin`，password=BCrypt("admin123")，role=`ADMIN`，status=1），使用 `ON CONFLICT (username) DO NOT NULL` 保证幂等，验证脚本语法正确

## 2. JWT 工具类

- [x] 2.1 创建 `JwtUtil` 类（`auth/util` 包），实现 `generateToken(userId, username, role)` 方法，使用 `agenthub.jwt.secret` 配置签名，验证生成的 Token 可被 `jjwt` 解析
- [x] 2.2 在 `JwtUtil` 中实现 `parseToken(token)` 方法，返回包含 userId、username、role 的 Claims，验证过期和无效签名场景抛出对应异常
- [x] 2.3 在 `JwtUtil` 中实现 `validateToken(token)` 方法，返回 boolean 表示 Token 是否有效，验证对过期/篡改 Token 返回 false

## 3. DTO 与数据层

- [x] 3.1 创建 `LoginRequest` DTO（username、password 字段，使用 `@NotBlank` 校验），验证 Bean Validation 注解生效
- [x] 3.2 创建 `LoginResponse` DTO（token、tokenType、expiresIn、UserInfo），验证字段完整
- [x] 3.3 创建 `UserInfo` DTO（id、username、realName、role、department），验证字段完整
- [x] 3.4 创建 `UserMapper` 接口，定义 `selectByUsername(username)` 方法，验证 MyBatis-Plus 查询可用

## 4. Service 层

- [x] 4.1 创建 `AuthService.login(request)` 方法，实现：查询用户 → 校验状态 → BCrypt 比对密码 → 生成 JWT → 返回 LoginResponse，验证登录成功和失败场景
- [x] 4.2 创建 `AuthService.getCurrentUser(userId)` 方法，根据 userId 查询并返回 UserInfo，验证返回数据正确
- [x] 4.3 创建 `AuthService.logout()` 方法（MVP 仅返回成功，预留扩展点），验证接口可调用

## 5. Controller 层

- [x] 5.1 创建 `AuthController`，实现 `POST /auth/login` 接口，验证返回格式符合 `R<LoginResponse>`
- [x] 5.2 在 `AuthController` 中实现 `POST /auth/logout` 接口，验证返回 `R<Void>` 成功
- [x] 5.3 在 `AuthController` 中实现 `GET /auth/me` 接口，从 SecurityContext 获取当前用户，验证返回 `R<UserInfo>`

## 6. Spring Security 配置

- [x] 6.1 创建 `SecurityConfig` 类，配置 `SecurityFilterChain`：禁用 CSRF、设置 SessionCreationPolicy.STATELESS、配置白名单路径（login、actuator、swagger、knife4j）、其余路径 `authenticated()`
- [x] 6.2 创建 `JwtAuthenticationFilter` 类，继承 `OncePerRequestFilter`，实现：从 Header 提取 Token → 校验 → 设置 SecurityContext → 放行链，验证过滤器逻辑正确
- [x] 6.3 在 `SecurityConfig` 中注册 `JwtAuthenticationFilter` 到过滤器链（`addFilterBefore` 在 `UsernamePasswordAuthenticationFilter` 之前），验证过滤器被正确调用
- [x] 6.4 配置 `AuthenticationManager` 和 `BCryptPasswordEncoder` 为 Spring Bean，验证密码编码器可注入使用

## 7. 集成验证

- [x] 7.1 启动应用，执行 `curl -X POST http://localhost:8080/api/auth/login -d '{"username":"admin","password":"admin123"}'`，验证返回 JWT Token
- [x] 7.2 使用获取的 Token 访问 `curl -H "Authorization: Bearer {token}" http://localhost:8080/api/auth/me`，验证返回管理员用户信息
- [x] 7.3 不带 Token 访问 `/api/auth/me`，验证返回 401
- [x] 7.4 使用错误密码登录，验证返回 401 且提示"用户名或密码错误"
- [x] 7.5 访问 Knife4j 文档页面 `http://localhost:8080/api/doc.html`，验证无需登录可正常打开
