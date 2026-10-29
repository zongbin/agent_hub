# Spec Delta

## Purpose

建立统一的三层权限管理体系，通过角色权限矩阵控制密级文档访问、文档级细粒度授权、以及操作审计日志，为知识库和 SuperAgent 提供安全基础。

## ADDED Requirements

### Requirement: 角色权限矩阵校验
系统 SHALL 基于用户角色和安全密级进行访问控制，未满足密级要求的请求返回 403。

#### Scenario: 管理员访问任意密级
- **WHEN** 管理员角色用户请求访问机密文档
- **THEN** 系统放行请求

#### Scenario: 查看者访问内部文档
- **WHEN** 查看者角色用户请求访问内部文档
- **THEN** 系统放行请求（只读）

#### Scenario: 访客访问内部文档
- **WHEN** 访客角色用户请求访问内部文档
- **THEN** 系统返回 403，拒绝访问

#### Scenario: 访客访问机密文档
- **WHEN** 访客角色用户请求访问机密文档
- **THEN** 系统返回 403，拒绝访问

### Requirement: 文档级授权覆盖
系统 SHALL 支持通过 `doc_permissions` 表对特定用户或部门授予机密文档的访问权限，授权覆盖角色默认限制。

#### Scenario: 授权用户访问机密文档
- **WHEN** 查看者角色用户被授予某机密文档的 READ 权限
- **THEN** 系统放行该文档的读请求

#### Scenario: 同部门成员继承部门授权
- **WHEN** 某机密文档授予了部门 X 的 READ 权限
- **THEN** 部门 X 的所有正式员工均可访问该文档

#### Scenario: 未授权用户仍被拒绝
- **WHEN** 某机密文档仅授权了用户 A，用户 B 未在授权列表中
- **THEN** 用户 B 访问时返回 403

### Requirement: 权限注解声明式校验
系统 SHALL 提供 `@RequirePermission` 注解，通过 AOP 拦截器在服务方法执行前完成权限校验。

#### Scenario: 注解标注方法被拦截
- **WHEN** 请求命中标注了 `@RequirePermission` 的服务方法
- **THEN** 拦截器在执行方法前校验权限，校验失败抛出 PermissionDeniedException

#### Scenario: 未标注方法不触发权限校验
- **WHEN** 请求命中未标注 `@RequirePermission` 的方法
- **THEN** 系统不额外执行权限注解逻辑（仅 JWT 认证仍生效）

#### Scenario: 权限校验失败返回统一错误
- **WHEN** `@RequirePermission` 校验不通过
- **THEN** 返回 HTTP 403，错误码为权限不足

### Requirement: 审计日志记录
系统 SHALL 通过 AOP 切面自动记录文档访问、权限变更等关键操作到 `audit_logs` 表。

#### Scenario: 文档访问被记录
- **WHEN** 用户访问文档详情接口
- **THEN** 系统自动写入一条审计日志，包含用户 ID、操作类型、文档 ID、时间戳

#### Scenario: 权限变更被记录
- **WHEN** 管理员授予或撤销文档权限
- **THEN** 系统写入审计日志，记录操作类型和目标用户/文档

#### Scenario: 权限拒绝被记录
- **WHEN** 用户请求被权限校验拒绝
- **THEN** 系统写入审计日志，result 字段标记为 DENIED

### Requirement: 权限管理 REST API
系统 SHALL 提供权限管理接口，支持管理员查询角色列表、修改用户角色、授予/撤销文档权限、查询审计日志。

#### Scenario: 修改用户角色
- **WHEN** 管理员调用 PUT /api/permissions/users/{id}/role
- **THEN** 系统更新用户角色并返回成功

#### Scenario: 授予文档权限
- **WHEN** 管理员调用 POST /api/permissions/documents/{id}/grant
- **THEN** 系统在 doc_permissions 表中创建授权记录并返回成功

#### Scenario: 撤销文档权限
- **WHEN** 管理员调用 DELETE /api/permissions/documents/{id}/revoke
- **THEN** 系统删除对应的 doc_permissions 记录并返回成功

#### Scenario: 查询审计日志
- **WHEN** 管理员调用 GET /api/permissions/audit-logs
- **THEN** 系统返回分页的审计日志列表，支持按用户/时间筛选

### Requirement: JWT 扩展角色与部门
系统 SHALL 在 JWT Token 载荷中包含用户角色（role）和部门（department）信息，权限拦截器从 SecurityContext 中读取。

#### Scenario: 登录返回的 Token 包含角色
- **WHEN** 用户登录成功
- **THEN** JWT 载荷中包含 role 和 department 声明

### Requirement: 权限校验优先级
系统 SHALL 按"管理员直通 → 文档级授权 → 角色密级匹配"的顺序进行权限判定，任一环节通过即放行。

#### Scenario: 管理员绕过密级限制
- **WHEN** 管理员访问任意密级文档
- **THEN** 系统直接放行，不检查文档级授权

#### Scenario: 文档授权覆盖角色限制
- **WHEN** 用户角色不足以访问某密级但拥有文档级授权
- **THEN** 系统放行
