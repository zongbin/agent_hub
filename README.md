# 智知 AgentHub

企业知识管理平台 — 文档知识库 + RAG 问答 + Plan-and-Execute SuperAgent。

> 详细产品文档：`doc/产品文档/企业知识管理Agent_PRD_MVP.md`
> 技术设计：`doc/技术设计/智知AgentHub_技术设计文档_MVP.md`

---

## 目录结构

```
agent_hub/
├── backend/                   # Spring Boot 3 + Java 17 后端
│   ├── pom.xml                # Maven 依赖
│   └── src/main/
│       ├── java/com/company/agenthub/
│       │   ├── AgentHubApplication.java
│       │   ├── auth/          # JWT 认证
│       │   ├── qa/            # RAG 问答流水线
│       │   ├── knowledge/     # 文档上传/解析/索引
│       │   ├── agent/         # SuperAgent HTTP 桥接
│       │   ├── permission/    # RBAC + 安全等级
│       │   ├── audit/         # 审计日志
│       │   ├── connector/     # 飞书/企微连接器
│       │   └── common/        # 公共基础设施
│       └── resources/
│           ├── application.yml
│           └── db/migration/  # 建库脚本（schema.sql，全量重建用，手动执行）
├── frontend/                  # Vue 3 + TS + Element Plus + Vite
│   ├── package.json
│   ├── vite.config.ts
│   ├── index.html
│   └── src/
│       ├── main.ts            # 入口
│       ├── App.vue            # 根组件 + 布局
│       ├── router/            # Vue Router 配置
│       ├── views/             # 页面级组件
│       │   ├── Dashboard/
│       │   ├── QAM/
│       │   ├── Knowledge/
│       │   ├── Permission/
│       │   └── Login/
│       ├── api/               # API 调用封装（axios）
│       ├── stores/            # Pinia 全局状态
│       ├── components/        # 可复用业务组件
│       ├── utils/             # 工具函数
│       ├── constants/         # 枚举常量
│       ├── layouts/           # 自定义布局
│       └── assets/            # 样式与静态资源
├── doc/                       # 产品文档 / 技术设计 / 原型
└── CLAUDE.md                  # Claude Code 工作指南
```

## 技术栈

| 层        | 技术                                              |
| --------- | ------------------------------------------------- |
| 前端      | Vue 3 + TypeScript + Element Plus + TailwindCSS + Vite |
| 后端      | Spring Boot 3.3 + Java 21 + MyBatis-Plus 3.5           |
| 数据库    | PostgreSQL 16 + pgvector                             |
| 缓存/状态 | Redis 7 (Redisson)                                   |
| 文件存储  | 本地磁盘（后续迁移 MinIO）                             |
| Agent     | LangGraph (Python 微服务, HTTP 调用)               |
| LLM       | DeepSeek-V3 (chat) / bge-large-zh (embedding)     |

## 快速开始

### 前置条件

- JDK 21、Maven 3.8+
- Node.js 18+、npm 9+
- Docker Compose（启动 PostgreSQL / Redis）

### 后端

```bash
cd backend
mvn clean package -DskipTests
java -jar target/agent-hub-0.1.0-SNAPSHOT.jar
```

### 前端

```bash
cd frontend
npm install
npm run dev   # http://localhost:8000，代理 /api → localhost:8080
```

## 里程碑

- **M1（1–4 周）**：文档上传 + AI 解析 + 基础 Q&A
- **M2（5–8 周）**：权限系统 + SuperAgent v1（竞品分析场景）
- **M3（9–12 周）**：连接器 + 审计日志 + 上线
