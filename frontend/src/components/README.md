# components 目录

可复用的业务组件，区别于 Element Plus 通用组件。

## 文件命名规范

| 文件 / 目录          | 职责                              |
| -------------------- | --------------------------------- |
| `DocumentCard/`      | 文档列表卡片                      |
| `ChatMessage/`       | 问答消息气泡（支持 Markdown 渲染） |
| `PlanStep/`          | Agent 计划步骤展示 / 编辑          |
| `Citation/`          | 答案引用片段展示                   |

## 约定

- 每个组件一个目录，含 `index.vue` 和样式文件。
- 组件 props 必须显式声明 TypeScript interface。
- 不在此目录放页面级组件（页面组件放 `views/`）。
