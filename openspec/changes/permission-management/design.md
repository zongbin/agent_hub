# Design

## Context

系统已完成 JWT 认证模块（user-auth），当前 `User` 实体已有 `role` 和 `department` 字段，但权限校验仅依赖 Spring Security 的 `anyRequest().authenticated()`，所有已认证用户可访问全部接口。缺少密级控制和文档级授权。`permission` 包目前仅有 `package-info.java`，尚未实现。

数据库方面已有 `users` 表（含 role 字段），`documents` 表尚未创建（当前为项目初期）。Flyway 已接入 pom.xml，可通过迁移脚本管理表结构。

## Goals / Non-Goals

**Goals:**
- 建立注解驱动的声明式权限校验机制
- 实现角色 × 密级权限矩阵
- 实现文档级细粒度授权（用户/部门）
- 实现审计日志 AOP 自动记录
- 提供权限管理 REST API

**Non-Goals:**
- 前端权限管理页面（本次不做）
- 字段级脱敏、水印（V2）
- 数据源权限继承（V2）
- 多角色支持（MVP 单角色即可）
- 权限缓存（MVP 直接查 DB，后续可加 Redis 缓存）

## Decisions

### Decision 1: 权限校验机制 — 自定义注解 + AOP

**选择:** 使用自定义 `@RequirePermission` 注解 + Spring AOP `@Around` 拦截器

**替代方案:**
- Spring Security `@PreAuthorize` + SpEL：表达能力有限，难以集成自定义密级逻辑
- 手动在 Service 层 if-else 校验：代码重复，难以维护

**理由:** 自定义注解可声明 `resource`、`action`、`minLevel` 三个维度，AOP 拦截器统一处理，与业务代码解耦。权限校验失败抛出 `PermissionDeniedException`，由 `GlobalExceptionHandler` 统一捕获返回 403。

### Decision 2: 权限判定顺序

**选择:** 管理员直通 → 文档级授权 → 角色密级匹配

**理由:** 管理员拥有最高权限可减少 DB 查询；文档级授权提供灵活性；角色密级作为兜底规则覆盖大部分场景。

### Decision 3: JWT 扩展载荷

**选择:** 在 JWT claims 中增加 `role` 和 `department` 字段

**理由:** 权限拦截器需要角色信息，若每次查 DB 会增加延迟。JWT 无状态特性使角色变更需重新登录生效，MVP 阶段可接受。

### Decision 4: 审计日志实现方式

**选择:** AOP 切面 + 自定义 `@AuditLog` 注解

**理由:** 与权限注解解耦，可独立控制。需记录的操作（文档访问、权限变更）在对应方法上标注，切面异步写入 `audit_logs` 表。

### Decision 5: 数据库迁移

**选择:** Flyway 迁移脚本，版本号延续现有序列

**新增表:**
- `doc_permissions`：文档级授权
- `audit_logs`：审计日志
- `documents` 表增加 `security_level` 列

## Risks / Trade-offs

- **角色变更延迟** → JWT 中的角色在过期前不会更新。缓解：MVP 可接受；后续加 Redis 缓存角色信息，管理员修改后主动失效旧 Token
- **权限校验性能** → 文档级授权需额外 DB 查询。缓解：MVP 数据量小（<500 文档）；后续引入 Redis 缓存权限结果
- **AOP 自调用限制** → Spring AOP 不拦截同一类内的方法调用。缓解：权限注解仅标注在 Controller 入口或独立 Service 方法上

## Migration Plan

1. 执行 Flyway 迁移脚本创建新表和字段
2. 部署新代码（向后兼容：新增字段有默认值，旧接口不受影响）
3. 管理员通过权限接口初始化用户角色

**回滚:** 代码回滚即可，新增字段和表不影响旧代码运行。

## Open Questions

- 审计日志是否异步写入（当前选择同步写入，MVP 阶段性能可接受）
- 文档密级默认值（建议：未设置时默认为 PUBLIC）
