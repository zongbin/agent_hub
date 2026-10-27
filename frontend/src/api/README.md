# api 目录

后端 API 调用封装层，基于 axios 实例，按业务域拆分子文件。

## 文件命名规范

| 文件              | 职责                          |
| ----------------- | ----------------------------- |
| `auth.ts`         | 登录 / 登出 / Token 刷新       |
| `knowledge.ts`    | 文档上传 / 列表 / 详情 / 删除  |
| `qa.ts`           | 问答接口（含 SSE 流式封装）    |
| `agent.ts`        | SuperAgent 任务提交 / 计划确认 |
| `permission.ts`   | 用户 / 角色 / 审计日志接口      |
| `request.ts`      | axios 实例 + 拦截器（Token、错误解包） |

## 约定

- `request.ts` 导出配置好的 axios 实例，所有模块共用。
- 响应拦截器自动解包 `R<T>.data`，业务层只接收实际数据。
- SSE 端点使用 `fetch` + `ReadableStream`，不走 axios。
