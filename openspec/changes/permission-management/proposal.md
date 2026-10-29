# Proposal

## Why

当前系统已完成 JWT 认证基础能力，但缺少统一的权限校验层。知识库文档上传后无密级区分，所有已认证用户可访问任意文档，无法满足企业对敏感文档（机密/内部）的访问控制需求。需要建立角色 + 密级 + 文档级授权的三层权限体系，为后续知识库、SuperAgent 等模块提供安全基础。

## What Changes

- 新增统一权限注解 `@RequirePermission` + AOP 拦截器，在服务层方法级别进行权限校验
- 新增安全密级枚举（PUBLIC / INTERNAL / CONFIDENTIAL），文档表增加 `security_level` 字段
- 新增文档级授权表 `doc_permissions`，支持按用户或部门授予机密文档访问权限
- 扩展 JWT Token 载荷，携带用户角色和部门信息，供权限拦截器使用
- 新增权限管理 REST API：用户角色管理、文档授权/撤销、审计日志查询
- 新增审计日志模块（AuditAspect），自动记录文档访问、权限变更等操作

## Capabilities

### New Capabilities
- `permission`: 统一权限管理体系，涵盖角色权限矩阵、安全密级校验、文档级授权、审计日志

### Modified Capabilities
- `user-auth`: 扩展 JWT Token 载荷和 SecurityContext，携带角色（role）和部门（department）信息，供权限拦截器读取

## Impact

- **后端代码**: 新增 `permission` 包（注解、拦截器、枚举、异常）；修改 `auth` 包（JwtUtil、JwtAuthenticationFilter、SecurityContextUtil、AuthService）
- **数据库**: 新增 `doc_permissions` 表；`documents` 表新增 `security_level` 列；新增 `audit_logs` 表；Flyway 迁移脚本
- **API**: 新增 `/api/permissions/*` 路径下的 5 个 REST 接口
- **前端**: 后续需增加权限管理页面（本次不包含前端实现）
