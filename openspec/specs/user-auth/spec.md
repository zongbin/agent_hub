# User-Auth Specification

## Purpose

提供基于 JWT 的用户认证能力，包括登录签发 Token、请求鉴权过滤器、当前用户信息获取，作为系统权限体系的基础设施。

## Requirements

### Requirement: 用户登录校验
系统 SHALL 接收用户名和密码，验证通过后签发 JWT 返回给客户端。

#### Scenario: 登录成功
- **WHEN** 用户提交正确的用户名和密码
- **THEN** 系统返回包含 JWT 的登录响应，状态码 200

#### Scenario: 用户名不存在
- **WHEN** 用户提交不存在的用户名
- **THEN** 系统返回 401，提示"用户名或密码错误"

#### Scenario: 密码错误
- **WHEN** 用户名存在但密码不匹配
- **THEN** 系统返回 401，提示"用户名或密码错误"

#### Scenario: 账号被禁用
- **WHEN** 用户提交正确的凭据但账号状态为禁用
- **THEN** 系统返回 403，提示"账号已被禁用"

### Requirement: JWT Token 校验
系统 SHALL 对受保护的请求校验 JWT Token 的有效性，校验通过后放行。

#### Scenario: Token 有效
- **WHEN** 请求携带未过期且签名正确的 JWT
- **THEN** 系统放行请求，SecurityContext 中注入用户身份

#### Scenario: Token 缺失
- **WHEN** 受保护路径的请求未携带 JWT
- **THEN** 系统返回 401，提示"未登录"

#### Scenario: Token 过期
- **WHEN** 请求携带已过期的 JWT
- **THEN** 系统返回 401，提示"登录已过期"

#### Scenario: Token 签名无效
- **WHEN** 请求携带被篡改或签名不匹配的 JWT
- **THEN** 系统返回 401，提示"Token 无效"

### Requirement: 白名单路径放行
系统 SHALL 对配置的公开路径不要求 JWT 校验，直接放行。

#### Scenario: 登录接口放行
- **WHEN** 请求访问登录接口
- **THEN** 系统放行，不要求 JWT

#### Scenario: 健康检查放行
- **WHEN** 请求访问健康检查接口
- **THEN** 系统放行，不要求 JWT

### Requirement: 获取当前用户信息
系统 SHALL 提供接口让已登录用户获取自身的基本信息和角色。

#### Scenario: 获取当前用户信息
- **WHEN** 已登录用户请求当前用户信息接口
- **THEN** 系统返回用户 ID、用户名、真实姓名、角色、部门等信息

### Requirement: 用户登出
系统 SHALL 提供登出接口，客户端丢弃 Token 即完成登出。

#### Scenario: 登出成功
- **WHEN** 已登录用户请求登出接口
- **THEN** 系统返回成功响应，客户端清除本地存储的 Token

### Requirement: 密码加密存储
系统 SHALL 使用 BCrypt 算法对用户密码进行哈希存储，不可明文保存。

#### Scenario: 管理员账号初始化
- **WHEN** 系统首次启动执行初始化脚本
- **THEN** 管理员账号的密码以 BCrypt 哈希形式存入数据库

#### Scenario: 登录时密码比对
- **WHEN** 用户登录提交密码
- **THEN** 系统使用 BCrypt 将提交密码与数据库哈希值比对，而非解密比较
