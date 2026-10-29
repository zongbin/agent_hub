# Spec Delta

## MODIFIED Requirements

### Requirement: JWT Token 校验
系统 SHALL 对受保护的请求校验 JWT Token 的有效性，校验通过后放行，并在 SecurityContext 中注入包含角色和部门的用户身份信息。

#### Scenario: Token 有效
- **WHEN** 请求携带未过期且签名正确的 JWT
- **THEN** 系统放行请求，SecurityContext 中注入用户身份（含 userId、role、department）

#### Scenario: Token 缺失
- **WHEN** 受保护路径的请求未携带 JWT
- **THEN** 系统返回 401，提示"未登录"

#### Scenario: Token 过期
- **WHEN** 请求携带已过期的 JWT
- **THEN** 系统返回 401，提示"登录已过期"

#### Scenario: Token 签名无效
- **WHEN** 请求携带被篡改或签名不匹配的 JWT
- **THEN** 系统返回 401，提示"Token 无效"

#### Scenario: SecurityContext 包含角色信息
- **WHEN** JWT 校验通过
- **THEN** SecurityContext 中的认证对象可获取当前用户的 role 和 department

### Requirement: 获取当前用户信息
系统 SHALL 提供接口让已登录用户获取自身的基本信息、角色和部门。

#### Scenario: 获取当前用户信息
- **WHEN** 已登录用户请求当前用户信息接口
- **THEN** 系统返回用户 ID、用户名、真实姓名、角色、部门等信息

## ADDED Requirements

### Requirement: 用户角色字段
系统 SHALL 在用户信息中维护角色字段（ADMIN/EDITOR/VIEWER/GUEST），角色由管理员通过权限接口分配。

#### Scenario: 新用户默认角色
- **WHEN** 系统初始化脚本创建管理员账号
- **THEN** 管理员账号角色为 ADMIN

#### Scenario: 管理员修改用户角色
- **WHEN** 管理员调用权限接口修改用户角色
- **THEN** 用户角色变更生效，后续 JWT 签发包含新角色
