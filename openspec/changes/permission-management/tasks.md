# Tasks

## 1. 数据库迁移

- [x] 1.1 创建 Flyway 迁移脚本 V3__add_permission_tables.sql，新增 doc_permissions 表和 audit_logs 表，documents 表增加 security_level 列，验证脚本在空库上执行成功且表结构正确
- [x] 1.2 在 UserMapper 和 User 实体中确认 role 字段映射正确，验证登录查询可返回 role 和 department

## 2. 权限核心组件

- [x] 2.1 创建 SecurityLevel 枚举（PUBLIC/INTERNAL/CONFIDENTIAL）和 PermissionAction 枚举（READ/WRITE/DELETE/EXECUTE），验证枚举值与角色权限矩阵一致
- [x] 2.2 创建 @RequirePermission 注解，定义 resource、action、minLevel 属性，验证注解可被 Spring 扫描
- [x] 2.3 创建 PermissionDeniedException 异常类（HTTP 403），并在 GlobalExceptionHandler 中添加处理逻辑，验证异常返回格式为 R.fail(403, "权限不足")
- [x] 2.4 创建 PermissionAspect 切面类，实现权限判定逻辑（管理员直通 → 文档级授权 → 角色密级匹配），验证切面拦截注解标注的方法

## 3. JWT 扩展

- [x] 3.1 修改 JwtUtil.generateToken 方法，在 claims 中增加 role 和 department 字段，验证生成的 Token 包含扩展声明
- [x] 3.2 修改 JwtAuthenticationFilter，解析 JWT 后构造包含角色信息的认证对象存入 SecurityContext，验证 SecurityContextUtil 可获取 role 和 department
- [x] 3.3 修改 AuthService.getCurrentUser 返回信息中确保包含 role 和 department，验证 /auth/me 接口响应包含这两个字段

## 4. 文档级授权

- [x] 4.1 创建 DocPermission 实体和 DocPermissionMapper，验证 CRUD 操作可正常执行
- [x] 4.2 在 PermissionAspect 中集成文档级授权查询逻辑（按 user_id 和 department 查询 doc_permissions 表），验证授权用户可突破角色限制访问机密文档

## 5. 审计日志

- [x] 5.1 创建 AuditLog 实体和 AuditLogMapper，验证可插入和查询审计记录
- [x] 5.2 创建 @AuditLog 注解和 AuditLogAspect 切面，自动记录操作类型、用户、资源 ID、时间戳，验证标注注解的方法执行后自动写入 audit_logs 表
- [x] 5.3 在权限校验失败时（PermissionDeniedException）记录 DENIED 类型的审计日志，验证拒绝操作可被追踪

## 6. 权限管理 API

- [x] 6.1 创建 PermissionController，实现 GET /api/permissions/roles 角色列表接口，验证返回预定义的角色枚举
- [x] 6.2 实现 PUT /api/permissions/users/{id}/role 修改用户角色接口（仅 ADMIN 可调用），验证角色更新成功并记录审计日志
- [x] 6.3 实现 POST /api/permissions/documents/{id}/grant 授予文档权限接口，验证授权记录写入 doc_permissions 表
- [x] 6.4 实现 DELETE /api/permissions/documents/{id}/revoke 撤销文档权限接口，验证授权记录被删除
- [x] 6.5 实现 GET /api/permissions/audit-logs 审计日志查询接口（分页、按用户/时间筛选），验证返回正确的分页结果

## 7. 集成验证

- [x] 7.1 使用 @RequirePermission 标注一个测试接口，用不同角色用户访问验证权限矩阵生效（ADMIN 可访问所有密级，GUEST 仅可访问 PUBLIC）
- [x] 7.2 验证文档级授权覆盖场景：GUEST 用户被授予某机密文档权限后可正常访问
- [x] 7.3 验证审计日志完整性：执行登录、访问文档、权限变更操作后，audit_logs 表有对应记录

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
