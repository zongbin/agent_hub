# 智知 AgentHub — MVP 技术设计文档

> **版本**：V1.0
> **日期**：2026-10-27
> **作者**：架构团队
> **状态**：Draft → Review
> **适用对象**：中小型企业（50-500人）

---

## 目录

1. [项目概述](#1-项目概述)
2. [需求分析优先级](#2-需求分析优先级)
3. [MVP 范围定义](#3-mvp-范围定义)
4. [总体架构设计](#4-总体架构设计)
5. [模块详细设计](#5-模块详细设计)
6. [数据库设计](#6-数据库设计)
7. [API 设计](#7-api-设计)
8. [SuperAgent 详细设计](#8-superagent-详细设计)
9. [权限与安全设计](#9-权限与安全设计)
10. [部署架构](#10-部署架构)
11. [技术选型与依据](#11-技术选型与依据)
12. [开发计划](#12-开发计划)
13. [风险与缓解](#13-风险与缓解)

---

## 1. 项目概述

### 1.1 一句话定位

让企业散落各处的文档，能在一个入口用自然语言秒问秒答；复杂任务交给超级智能体自主规划执行。

### 1.2 核心痛点与解决方案

| 痛点 | 解决方案 | MVP 优先级 |
|------|---------|-----------|
| 资料分散，找文档要翻5个地方 | 统一上传 + 语义检索 | **P0** |
| 新人问重复问题，老员工被问烦 | RAG 智能问答，7×24在线 | **P0** |
| 写报告/方案要东拼西凑 | SuperAgent 自主规划，5分钟搞定 | **P0** |
| 文档版本混乱，不知道哪个最新 | AI 检测过期 + 权限控制 | **P0/P1** |
| 操作不合规，事后无法追溯 | 审计日志 | **P1** |
| 飞书/企业微信文档需手动搬运 | 连接器同步 | **P1** |

### 1.3 产品架构总览

```
┌──────────────────────────────────────────────────────────────┐
│                     智知 AgentHub                             │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │  知识库管理    │  │  智能问答     │  │   SuperAgent     │   │
│  │  (上传/解析/   │  │  (RAG        │  │  (Plan &         │   │
│  │   管理)        │  │   检索/生成)  │  │   Execute)       │   │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘   │
│         │                 │                    │             │
│  ┌──────┴─────────────────┴────────────────────┴─────────┐   │
│  │              权限与安全层 (RBAC + 密级)                  │   │
│  │         (认证 / 授权 / 审计 / 加密)                      │   │
│  └───────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

---

## 2. 需求分析优先级

### 2.1 优先级矩阵

```
            高业务价值
               ▲
               │
    ┌──────────┼──────────┐
    │  P0:立即做 │  P0:立即做 │
    │  知识库上传 │  智能问答   │
    │  权限控制  │  SuperAgent │
    ├──────────┼──────────┤
    │  P2:以后做 │  P1:跟进做 │
    │  知识图谱  │  审计日志   │
    │  多模型适配 │  连接器     │
    │  字段级脱敏 │  文档过期检测│
    └──────────┴──────────┘
               │
               ▼
            低业务价值
    低复杂度 ──────► 高复杂度
```

### 2.2 MVP 阶段划分

| 阶段 | 时间 | 交付物 | 验收标准 |
|------|------|--------|---------|
| **M1** | 第1-4周 | 文档上传 + AI解析 + 基础问答 | 上传100篇文档，问答准确率≥70% |
| **M2** | 第5-8周 | 权限系统 + SuperAgent v1（竞品分析） | 4角色生效，Agent完成率≥80% |
| **M3** | 第9-12周 | 连接器 + 审计日志 + 上线运营 | 飞书/企微同步，系统稳定运行 |

---

## 3. MVP 范围定义

### 3.1 MVP 功能清单

| 模块 | 功能 | 优先级 | 工作量（人天） |
|------|------|--------|--------------|
| **知识库** | 拖拽上传 PDF/Word/PPT/Excel/Markdown | P0 | 5 |
| **知识库** | AI 解析 + 摘要生成 + 自动标签 | P0 | 5 |
| **知识库** | 文档列表/分类/筛选/搜索 | P0 | 3 |
| **知识库** | 文档过期检测（AI判断） | P1 | 3 |
| **智能问答** | RAG 语义检索 + 答案生成 | P0 | 8 |
| **智能问答** | 答案带原文引用（可点击跳转） | P0 | 3 |
| **智能问答** | 问题模式自动判断（简单vs复杂） | P0 | 2 |
| **SuperAgent** | Plan-and-Execute 框架搭建 | P0 | 10 |
| **SuperAgent** | 工具层：知识搜索/文档阅读/内容生成/数据分析 | P0 | 8 |
| **SuperAgent** | 执行面板（计划展示+逐步执行+控制按钮） | P0 | 5 |
| **SuperAgent** | 竞品分析场景 | P0 | 3 |
| **SuperAgent** | 项目方案 + 会议纪要场景 | P1 | 5 |
| **权限** | 4个角色 + 3级密级 | P0 | 5 |
| **权限** | 权限校验中间件 | P0 | 3 |
| **权限** | 权限矩阵配置页 | P0 | 2 |
| **审计** | 操作日志记录 | P1 | 3 |
| **审计** | 审计日志查询页 | P1 | 2 |
| **连接器** | 飞书文档同步 | P1 | 5 |
| **基础设施** | 用户认证（SSO/用户名密码） | P0 | 3 |
| **基础设施** | Docker Compose 部署 | P0 | 2 |

### 3.2 MVP 不做（明确排除）

- ❌ 知识图谱（V2）
- ❌ 字段级脱敏、IP白名单、水印（V2）
- ❌ 多模型适配（MVP 只接 DeepSeek）（V2）
- ❌ 数据源权限继承（手动配置）（V2）
- ❌ Agent 市场 / 自定义编排（V2）
- ❌ 移动端 App（V2）

---

## 4. 总体架构设计

### 4.1 分层架构

```
┌─────────────────────────────────────────────────────────────────┐
│                       前端层 (Frontend)                          │
│  Vue 3 + TypeScript + Element Plus + TailwindCSS                │
│  ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌───────────────┐      │
│  │ Dashboard│ │ Q&A聊天  │ │ 知识库   │ │  权限管理     │      │
│  │ 概览页   │ │ 页面     │ │ 页面     │ │  页面         │      │
│  └─────────┘ └──────────┘ └──────────┘ └───────────────┘      │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTPS / SSE
┌──────────────────────────┴──────────────────────────────────────┐
│                     API 网关层 (Nginx)                           │
│              负载均衡 / 静态资源 / SSL终止                       │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────┴──────────────────────────────────────┐
│                     后端服务层 (Backend)                         │
│  Spring Boot 3.x + Java 17                                     │
│  ┌───────────┐ ┌───────────┐ ┌────────────┐ ┌────────────┐    │
│  │ Auth      │ │ Q&A       │ │ Knowledge  │ │ SuperAgent │    │
│  │ Service   │ │ Service   │ │ Service    │ │ Service    │    │
│  │ (认证授权) │ │ (RAG问答) │ │ (文档管理) │ │ (Agent编排)│    │
│  └─────┬─────┘ └─────┬─────┘ └─────┬──────┘ └─────┬──────┘    │
│        │             │              │               │           │
│  ┌─────┴─────────────┴──────────────┴───────────────┴──────┐    │
│  │              权限拦截器 (Permission Filter)               │    │
│  └─────────────────────────────────────────────────────────┘    │
│  ┌───────────┐ ┌───────────┐ ┌────────────┐ ┌────────────┐    │
│  │ Audit     │ │ Connector │ │ LLM Client │ │ Embedding  │    │
│  │ Service   │ │ Service   │ │ (模型调用) │ │ Service    │    │
│  │ (审计日志) │ │ (连接器)  │ │            │ │ (向量化)   │    │
│  └───────────┘ └───────────┘ └────────────┘ └────────────┘    │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────┴──────────────────────────────────────┐
│                      数据存储层 (Storage)                        │
│  ┌──────────────────┐ ┌──────────┐ ┌──────────────────┐        │
│  │ PostgreSQL       │ │ MinIO    │ │ Redis            │        │
│  │ 元数据/权限/审计  │ │ 文件存储  │ │ 缓存/会话/SSE    │        │
│  │ + pgvector 向量库 │ │ 原始文档  │ │ 限流/Agent状态   │        │
│  │   语义检索        │ │          │ │                  │        │
│  └──────────────────┘ └──────────┘ └──────────────────┘        │
└─────────────────────────────────────────────────────────────────┘
                           │
┌──────────────────────────┴──────────────────────────────────────┐
│                    外部服务层 (External)                         │
│  ┌──────────────┐ ┌───────────────┐ ┌────────────────────┐     │
│  │ DeepSeek API │ │  LangGraph     │ │ 飞书/企业微信 API  │     │
│  │ (LLM + Embed)│ │  (Agent编排)   │ │ (连接器)           │     │
│  └──────────────┘ └───────────────┘ └────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 核心流程

#### 4.2.1 文档上传与索引流程

```
用户上传文档
    │
    ▼
① 文件接收（Multipart）→ MinIO 存储原始文件
    │
    ▼
② 文档解析（Apache Tika / python-docx）→ 提取纯文本
    │
    ▼
③ 文本分块（Chunker：512 tokens，重叠50 tokens）
    │
    ▼
④ Embedding 向量化（DeepSeek-Embedding / BAAI/bge-large-zh）
    │
    ▼
⑤ 向量写入 PostgreSQL pgvector（带文档权限关联）
    │
    ▼
⑥ 元数据写入 PostgreSQL（文档名/密级/标签/上传者/摘要）
    │
    ▼
⑦ AI 生成摘要 + 自动标签（LLM 调用）
    │
    ▼
⑧ 返回成功，前端刷新列表
```

#### 4.2.2 RAG 问答流程

```
用户输入问题
    │
    ▼
① 意图判断（简单问题 vs 复杂任务）
    │
    ├── 简单问题 ──▶ RAG 问答链路 ─────────────────┐
    │                                              │
    ▼                                              │
② 问题 Embedding 向量化                           │
    │                                              │
    ▼                                              │
③ pgvector 语义检索（Top-K=5，带权限过滤）          │
    │                                              │
    ▼                                              │
④ 检索结果重排序（ReRanker / 时间衰减）            │
    │                                              │
    ▼                                              │
⑤ 构建 Prompt（问题 + 上下文 + 引用信息）          │
    │                                              │
    ▼                                              │
⑥ LLM 生成答案（流式输出 SSE）                     │
    │                                              │
    ▼                                              │
⑦ 返回答案 + 引用来源 + 反馈按钮                   │
    │                                              │
    ├── 复杂任务 ──▶ SuperAgent 链路 ───────────────┘
```

#### 4.2.3 SuperAgent 流程

```
用户输入复杂任务
    │
    ▼
① 任务分析 → 确认交付物类型（报告/方案/表格）
    │
    ▼
② Planner 生成执行计划（JSON 结构化）
    │
    ▼
③ 前端展示计划 → 用户可修改/确认
    │
    ▼
④ Executor 逐步执行
    │   ├── Step N → 调用工具 → 获取结果 → 更新状态
    │   ├── 权限检查（每步都做）
    │   └── SSE 推送进度到前端
    │
    ▼
⑤ 汇总结果 → 生成最终交付物
    │
    ▼
⑥ 返回结果 + 引用溯源 + 反馈
```

---

## 5. 模块详细设计

### 5.1 知识库模块

#### 5.1.1 文档解析策略

| 文档类型 | 解析方案 | 依赖库 |
|---------|---------|--------|
| PDF | 文本型：pdfplumber；扫描型：OCR（暂不支持 MVP） | pdfplumber / PyMuPDF |
| Word | python-docx | python-docx |
| PPTX | python-pptx | python-pptx |
| Excel | openpyxl（转为 Markdown 表格） | openpyxl |
| Markdown | 直接读取 | - |

#### 5.1.2 文本分块策略

```java
// 分块配置
public class ChunkConfig {
    private int chunkSize = 512;        // 每块 token 数
    private int chunkOverlap = 50;      // 重叠 token 数
    private String separator = "\n\n";  // 优先按段落切分
    private boolean respectMarkdown = true; // 保持 Markdown 结构
}
```

**分块规则**：
1. 优先按 Markdown 标题/段落边界切分
2. 超长段落按句子边界切分
3. 每块保留元信息：文档ID、页码、章节路径

#### 5.1.3 AI 摘要与标签生成

```json
{
  "summary": "本文档描述了API开发规范v3.2，涵盖接口设计、错误码定义、安全认证等方面的标准...",
  "tags": ["API", "规范", "开发", "安全"],
  "category": "技术文档",
  "confidence": 0.92
}
```

### 5.2 智能问答模块

#### 5.2.1 RAG Pipeline

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Query        │    │  Retrieval   │    │  Generation  │
│  Processing   │───▶│  (pgvector)  │───▶│  (LLM)       │
│              │    │              │    │              │
│ • 意图识别   │    │ • Embedding  │    │ • Prompt组装 │
│ • 查询改写   │    │ • 语义检索   │    │ • 流式生成   │
│ • 权限注入   │    │ • 权限过滤   │    │ • 引用标注   │
└──────────────┘    └──────────────┘    └──────────────┘
```

#### 5.2.2 Prompt 模板

```markdown
你是企业知识助手「智知」，请基于以下知识库内容回答用户问题。

## 规则
1. 仅基于提供的上下文回答，不要编造信息
2. 每个关键论点必须标注来源 [来源:N]
3. 如果知识库中没有相关信息，诚实告知
4. 回答简洁清晰，使用 Markdown 格式

## 用户信息
- 角色: {userRole}
- 部门: {userDepartment}

## 上下文（检索到的文档片段）
{context}

## 用户问题
{question}

## 你的回答
```

#### 5.2.3 引用溯源机制

每个检索到的 chunk 携带元信息：
```json
{
  "chunkId": "uuid",
  "docId": "doc-001",
  "docName": "API开发规范v3.2",
  "pageNumber": 12,
  "sectionPath": "第三章 > 接口设计 > 认证方式",
  "chunkIndex": 3,
  "securityLevel": "internal",
  "uploader": "王强",
  "uploadTime": "2026-10-25"
}
```

### 5.3 权限管理模块

#### 5.3.1 角色权限矩阵

| 角色 | 🟢 公开 | 🔵 内部 | 🔴 机密 | SuperAgent | 用户管理 | 系统配置 |
|------|---------|---------|---------|-----------|---------|---------|
| **管理员** | 读写删 | 读写删 | 读写删 | ✅ | ✅ | ✅ |
| **编辑者** | 读写 | 读写 | 只读 | ✅ | ❌ | ❌ |
| **查看者** | 只读 | 只读 | ❌ | ❌ | ❌ | ❌ |
| **访客** | 只读 | ❌ | ❌ | ❌ | ❌ | ❌ |

#### 5.3.2 权限校验流程

```
请求到达
    │
    ▼
① JWT 解析 → 获取 userId + role
    │
    ▼
② 查询用户角色 + 部门
    │
    ▼
③ 查询目标资源密级 + 授权列表
    │
    ▼
④ 权限判断
    │
    ├── 角色权限足够 → ✅ 放行
    │
    ├── 在文档授权白名单 → ✅ 放行
    │
    └── 不满足 → ❌ 拒绝 + 记录审计日志
```

#### 5.3.3 权限注解设计

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String resource();           // 资源类型: DOCUMENT, AGENT, ADMIN
    String action();             // 操作: READ, WRITE, DELETE, EXECUTE
    SecurityLevel minLevel() default SecurityLevel.PUBLIC;
}

// 使用示例
@RequirePermission(resource = "DOCUMENT", action = "READ", minLevel = SecurityLevel.INTERNAL)
public Document getDocument(String docId) { ... }
```

---

## 6. 数据库设计

### 6.1 ER 关系图

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   users       │     │   roles       │     │ departments  │
│──────────────│     │──────────────│     │──────────────│
│ id (PK)      │     │ id (PK)      │     │ id (PK)      │
│ username     │     │ name         │     │ name         │
│ display_name │     │ description  │     │ parent_id    │
│ email        │     │ permissions  │     └──────┬───────┘
│ role_id (FK) │     └──────┬───────┘            │
│ dept_id (FK) │            │                    │
│ status       │     ┌──────┴───────┐            │
└──────┬───────┘     │ user_roles   │     ┌──────┴───────┐
       │             │──────────────│     │              │
       │             │ user_id (FK) │     │              │
       │             │ role_id (FK) │     │              │
       │             └──────────────┘     │              │
       │                                  │              │
       ▼                                  ▼              │
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  documents    │     │ doc_permissions│   │   tags       │
│──────────────│     │──────────────│     │──────────────│
│ id (PK)      │     │ id (PK)      │     │ id (PK)      │
│ name         │     │ doc_id (FK)  │     │ name         │
│ file_path    │     │ user_id (FK) │     └──────┬───────┘
│ file_type    │     │ dept_id (FK) │            │
│ file_size    │     │ permission   │     ┌──────┴───────┐
│ security_level│    └──────────────┘     │ doc_tags     │
│ category     │                         │──────────────│
│ summary      │     ┌──────────────┐     │ doc_id (FK)  │
│ ai_tags      │     │ audit_logs   │     │ tag_id (FK)  │
│ uploader_id  │     │──────────────│     └──────────────┘
│ status       │     │ id (PK)      │
│ page_count   │     │ user_id (FK) │     ┌──────────────�│
│ created_at   │     │ action       │     │ agent_tasks  │
│ updated_at   │     │ resource_type│     │──────────────│
└──────────────┘     │ resource_id  │     │ id (PK)      │
                     │ detail       │     │ user_id (FK) │
                     │ ip_address   │     │ input        │
                     │ result       │     │ plan (JSON)  │
                     │ created_at   │     │ status       │
                     └──────────────┘     │ result       │
                                          │ created_at   │
┌──────────────┐     ┌──────────────┐     │ updated_at   │
│qa_sessions   │     │qa_messages   │     └──────────────┘
│──────────────│     │──────────────│
│ id (PK)      │     │ id (PK)      │
│ user_id (FK) │     │ session_id   │
│ created_at   │     │ role         │
│ updated_at   │     │ content      │
└──────────────┘     │ citations    │
                     │ feedback     │
                     │ created_at   │
                     └──────────────┘
```

### 6.2 核心表 DDL

```sql
-- 用户表
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(64) UNIQUE NOT NULL,
    password    VARCHAR(256) NOT NULL,  -- BCrypt 加密
    display_name VARCHAR(64) NOT NULL,
    email       VARCHAR(128) UNIQUE,
    role        VARCHAR(32) NOT NULL DEFAULT 'VIEWER', -- ADMIN, EDITOR, VIEWER, GUEST
    department  VARCHAR(64),
    status      SMALLINT DEFAULT 1,  -- 1:活跃 0:禁用
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

-- 文档表
CREATE TABLE documents (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(256) NOT NULL,
    file_path       VARCHAR(512) NOT NULL,       -- MinIO 路径
    file_type       VARCHAR(16) NOT NULL,        -- PDF/DOCX/PPTX/XLSX/MD
    file_size       BIGINT NOT NULL,
    security_level  VARCHAR(16) NOT NULL DEFAULT 'PUBLIC', -- PUBLIC, INTERNAL, CONFIDENTIAL
    category        VARCHAR(64),
    summary         TEXT,                        -- AI 生成摘要
    ai_tags         JSONB,                       -- AI 标签 ["API","规范"]
    uploader_id     BIGINT REFERENCES users(id),
    status          VARCHAR(16) DEFAULT 'ACTIVE', -- ACTIVE, EXPIRED, ARCHIVED
    page_count      INTEGER,
    chunk_count     INTEGER DEFAULT 0,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

-- 文档级权限表（用于机密文档的细粒度授权）
CREATE TABLE doc_permissions (
    id              BIGSERIAL PRIMARY KEY,
    doc_id          BIGINT REFERENCES documents(id),
    user_id         BIGINT REFERENCES users(id),    -- 用户级授权
    department      VARCHAR(64),                     -- 部门级授权
    permission      VARCHAR(16) DEFAULT 'READ',     -- READ, WRITE
    granted_by      BIGINT REFERENCES users(id),
    created_at      TIMESTAMP DEFAULT NOW()
);

-- 审计日志表
CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    username        VARCHAR(64),                     -- 冗余存储，防关联丢失
    action          VARCHAR(32) NOT NULL,            -- VIEW, EDIT, DELETE, AGENT_RUN, LOGIN, LOGOUT
    resource_type   VARCHAR(32) NOT NULL,            -- DOCUMENT, AGENT_TASK, USER, SYSTEM
    resource_id     VARCHAR(64),
    detail          JSONB,                           -- 操作详情
    ip_address      VARCHAR(64),
    user_agent      VARCHAR(512),
    result          VARCHAR(16) DEFAULT 'SUCCESS',   -- SUCCESS, DENIED, WARNING
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Agent 任务表
CREATE TABLE agent_tasks (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    session_id      VARCHAR(64),                     -- 关联 QA 会话
    input_text      TEXT NOT NULL,                   -- 用户原始输入
    plan_json       JSONB,                           -- 执行计划
    status          VARCHAR(16) DEFAULT 'PLANNING',  -- PLANNING, RUNNING, PAUSED, COMPLETED, FAILED
    current_step    INTEGER DEFAULT 0,
    steps_total     INTEGER DEFAULT 0,
    result_text     TEXT,                            -- 最终结果
    result_citations JSONB,                          -- 引用溯源
    error_message   TEXT,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

-- Agent 执行步骤表（用于断点续执和审计）
CREATE TABLE agent_steps (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT REFERENCES agent_tasks(id),
    step_index      INTEGER NOT NULL,
    step_name       VARCHAR(256),
    tool_name       VARCHAR(64),
    tool_input      JSONB,
    tool_output     JSONB,
    status          VARCHAR(16) DEFAULT 'PENDING',   -- PENDING, RUNNING, DONE, SKIPPED, FAILED
    error_message   TEXT,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW()
);

-- QA 会话表
CREATE TABLE qa_sessions (
    id              VARCHAR(64) PRIMARY KEY,         -- UUID
    user_id         BIGINT REFERENCES users(id),
    mode            VARCHAR(16) DEFAULT 'QA',        -- QA, AGENT
    title           VARCHAR(256),                    -- 自动生成标题
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

-- QA 消息表
CREATE TABLE qa_messages (
    id              BIGSERIAL PRIMARY KEY,
    session_id      VARCHAR(64) REFERENCES qa_sessions(id),
    role            VARCHAR(16) NOT NULL,            -- user, assistant
    content         TEXT NOT NULL,
    citations       JSONB,                           -- 引用溯源
    feedback        SMALLINT,                        -- 1:👍 -1:👎 NULL:未反馈
    created_at      TIMESTAMP DEFAULT NOW()
);

-- 索引
CREATE INDEX idx_documents_uploader ON documents(uploader_id);
CREATE INDEX idx_documents_security ON documents(security_level);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_category ON documents(category);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);
CREATE INDEX idx_agent_tasks_user ON agent_tasks(user_id);
CREATE INDEX idx_agent_tasks_status ON agent_tasks(status);
CREATE INDEX idx_qa_messages_session ON qa_messages(session_id);
```

### 6.3 pgvector 向量表设计

向量存储直接利用 PostgreSQL 的 `pgvector` 扩展，与业务数据同库，通过 `doc_id` 关联文档表的权限信息，实现权限过滤与向量检索一体化。

```sql
-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 文档向量分块表
CREATE TABLE document_chunks (
    id              BIGSERIAL PRIMARY KEY,
    doc_id          BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index     INTEGER NOT NULL,
    chunk_text      TEXT NOT NULL,
    token_count     INTEGER DEFAULT 0,
    embedding       vector(1024),  -- 与 Embedding 模型维度一致
    section_path    VARCHAR(512),  -- 章节路径，如 "第三章 > 接口设计"
    page_number     INTEGER,
    created_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(doc_id, chunk_index)
);

-- HNSW 索引（推荐：比 IVFFlat 查询更快，适合 < 100万向量）
CREATE INDEX idx_chunks_embedding_hnsw ON document_chunks
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- 文档 ID 索引（加速权限 JOIN 过滤）
CREATE INDEX idx_chunks_doc_id ON document_chunks(doc_id);
```

**向量检索 SQL 示例**：

```sql
-- 带权限过滤的语义检索（通过 JOIN documents 表实现权限控制）
SELECT
    dc.id,
    dc.doc_id,
    dc.chunk_index,
    dc.chunk_text,
    d.name AS doc_name,
    d.security_level,
    dc.embedding <=> :query_embedding AS distance  -- 余弦距离
FROM document_chunks dc
JOIN documents d ON d.id = dc.doc_id
WHERE d.status = 'ACTIVE'
  AND d.security_level IN ('PUBLIC', 'INTERNAL')  -- 权限过滤条件
  AND d.id NOT IN (                                -- 排除无权限的机密文档
      SELECT doc_id FROM doc_permissions
      WHERE user_id IS NULL                        -- 非授权用户
  )
ORDER BY dc.embedding <=> :query_embedding ASC    -- 余弦距离升序（越近越相似）
LIMIT 5;
```

> **说明**：`<=>` 是 pgvector 的余弦距离操作符。值为 0 表示完全相同，2 表示完全不同。检索结果按距离从小到大排列，即相似度从高到低。

**pgvector 索引选型**：

| 索引类型 | 适用场景 | 查询速度 | 构建速度 | 内存占用 | MVP 推荐 |
|---------|---------|---------|---------|---------|---------|
| HNSW | < 100万向量，高精度 | 极快 | 慢 | 高 | ✅ 推荐 |
| IVFFlat | > 100万向量 | 快 | 快 | 低 | 备选 |
| 无索引（暴力扫描） | < 1万向量 | 慢 | 无 | 无 | 开发测试 |

> **决策**：MVP 阶段向量数预计 < 50万（约 500 篇文档 × 1000 chunk/篇），HNSW 索引查询延迟 < 50ms，完全满足性能需求。

---

## 7. API 设计

### 7.1 RESTful API 概览

#### 认证相关
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录，返回 JWT |
| POST | `/api/auth/logout` | 用户登出 |
| GET | `/api/auth/me` | 获取当前用户信息 |

#### 知识库相关
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/documents/upload` | 上传文档（Multipart） |
| GET | `/api/documents` | 文档列表（分页+筛选） |
| GET | `/api/documents/{id}` | 文档详情 |
| DELETE | `/api/documents/{id}` | 删除文档 |
| PATCH | `/api/documents/{id}` | 更新文档元信息（密级/标签） |
| GET | `/api/documents/{id}/content` | 获取文档原文 |
| GET | `/api/documents/{id}/download` | 下载原始文件 |

#### 问答相关
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/qa/ask` | 简单问答（同步返回） |
| POST | `/api/qa/sessions` | 创建问答会话 |
| GET | `/api/qa/sessions` | 会话列表 |
| GET | `/api/qa/sessions/{id}/messages` | 会话消息历史 |
| POST | `/api/qa/messages/{id}/feedback` | 消息反馈（👍/👎） |

#### SuperAgent 相关
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/agent/plan` | 生成执行计划 |
| POST | `/api/agent/tasks` | 创建 Agent 任务 |
| GET | `/api/agent/tasks/{id}` | 查询任务状态 |
| POST | `/api/agent/tasks/{id}/pause` | 暂停任务 |
| POST | `/api/agent/tasks/{id}/resume` | 恢复任务 |
| POST | `/api/agent/tasks/{id}/skip` | 跳过当前步 |
| PATCH | `/api/agent/tasks/{id}/plan` | 修改计划 |
| GET | `/api/agent/tasks/{id}/stream` | SSE 流式推送执行进度 |

#### 权限相关
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/permissions/roles` | 角色列表 |
| PUT | `/api/permissions/users/{id}/role` | 修改用户角色 |
| POST | `/api/permissions/documents/{id}/grant` | 授予文档权限 |
| DELETE | `/api/permissions/documents/{id}/revoke` | 撤销文档权限 |
| GET | `/api/permissions/audit-logs` | 审计日志查询 |

### 7.2 关键 API 请求/响应示例

#### 7.2.1 上传文档

```http
POST /api/documents/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: <binary>
category: 技术文档
securityLevel: INTERNAL
tags: API,规范
```

```json
// Response 201
{
    "code": 0,
    "message": "success",
    "data": {
        "id": "doc-20261027-001",
        "name": "API开发规范v3.2.pdf",
        "fileType": "PDF",
        "fileSize": 4404019,
        "securityLevel": "INTERNAL",
        "summary": "本文档描述了API开发规范v3.2，涵盖接口设计、错误码定义...",
        "aiTags": ["API", "规范", "开发", "安全"],
        "chunkCount": 42,
        "status": "PROCESSING",
        "createdAt": "2026-10-27T10:30:00Z"
    }
}
```

#### 7.2.2 简单问答

```http
POST /api/qa/ask
Authorization: Bearer {token}

{
    "question": "报销流程是什么？",
    "sessionId": "sess-abc123"
}
```

```json
// Response 200
{
    "code": 0,
    "data": {
        "answer": "根据《费用报销管理制度》，报销流程如下：\n\n1. 填写报销单 → OA系统填写，上传发票\n2. 主管审批 → ≤5000元直属主管，>5000元需总监\n3. 财务审核 → 3个工作日内完成\n4. 打款 → 审核通过后5个工作日内",
        "citations": [
            {
                "docId": "doc-001",
                "docName": "费用报销管理制度",
                "section": "第三章 > 报销流程",
                "pageNumber": 5,
                "relevanceScore": 0.92
            },
            {
                "docId": "doc-002",
                "docName": "OA操作手册",
                "section": "常用操作 > 报销",
                "pageNumber": 18,
                "relevanceScore": 0.85
            }
        ],
        "mode": "QA",
        "confidence": 0.88
    }
}
```

#### 7.2.3 SuperAgent 任务

```http
POST /api/agent/tasks
Authorization: Bearer {token}

{
    "input": "分析竞品X的产品功能，输出对比报告",
    "sessionId": "sess-abc123"
}
```

```json
// Response 201
{
    "code": 0,
    "data": {
        "taskId": "task-20261027-001",
        "status": "PLANNING",
        "plan": {
            "steps": [
                {"index": 1, "name": "搜索竞品X相关资料", "tool": "knowledge_search", "status": "PENDING"},
                {"index": 2, "name": "搜索我方产品文档", "tool": "knowledge_search", "status": "PENDING"},
                {"index": 3, "name": "提取功能对比点", "tool": "document_read", "status": "PENDING"},
                {"index": 4, "name": "生成对比分析表", "tool": "data_analysis", "status": "PENDING"},
                {"index": 5, "name": "输出完整报告", "tool": "content_generation", "status": "PENDING"}
            ]
        },
        "streamUrl": "/api/agent/tasks/task-20261027-001/stream"
    }
}
```

### 7.3 SSE 事件流设计

```text
// SSE 事件类型
event: agent.plan
data: {"steps": [...]}

event: agent.step.start
data: {"stepIndex": 1, "stepName": "搜索竞品X相关资料"}

event: agent.step.progress
data: {"stepIndex": 1, "message": "正在检索知识库...", "progress": 0.5}

event: agent.step.complete
data: {"stepIndex": 1, "result": "找到3篇相关文档", "citations": [...]}

event: agent.step.skip
data: {"stepIndex": 3, "reason": "权限不足，已跳过"}

event: agent.complete
data: {"result": "报告已生成...", "citations": [...]}

event: agent.error
data: {"stepIndex": 4, "error": "LLM调用超时"}
```

---

## 8. SuperAgent 详细设计

### 8.1 架构选型：Plan-and-Execute

```
┌─────────────────────────────────────────────────────────────┐
│                  智知 SuperAgent 架构                        │
│                                                             │
│  ┌─────────────────┐                                        │
│  │  Task Router     │  ← 判断简单问题 vs 复杂任务            │
│  │  (意图分类器)    │                                        │
│  └────────┬────────┘                                        │
│           │                                                 │
│           ▼ 复杂任务                                         │
│  ┌─────────────────┐     ┌─────────────────┐               │
│  │   Planner        │────▶│   Plan Store    │               │
│  │   (规划器)       │     │   (Redis/DB)    │               │
│  │                 │     │  持久化计划      │               │
│  │  LLM生成JSON计划 │     └─────────────────┘               │
│  └────────┬────────┘                                        │
│           │ 用户确认/修改                                     │
│           ▼                                                 │
│  ┌─────────────────┐     ┌─────────────────┐               │
│  │   Executor       │────▶│   State Store   │               │
│  │   (执行器)       │     │   (Redis/DB)    │               │
│  │                 │     │  断点续执        │               │
│  │  LangGraph 编排  │     └─────────────────┘               │
│  └────────┬────────┘                                        │
│           │                                                 │
│           ▼                                                 │
│  ┌─────────────────────────────────────────┐               │
│  │           Tool Layer (工具层)            │               │
│  │                                         │               │
│  │  🔍 knowledge_search  (知识搜索)        │               │
│  │  📄 document_read     (文档阅读)        │               │
│  │  ✍️ content_generate  (内容生成)        │               │
│  │  📊 data_analysis     (数据分析)        │               │
│  │  🔗 api_call          (API调用)         │               │
│  └─────────────────────────────────────────┘               │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 Planner 设计

#### 8.2.1 规划 Prompt

```markdown
你是一个企业知识管理Agent的规划器。用户会给你一个复杂任务，你需要将其分解为可执行的步骤。

## 可用工具
1. knowledge_search - 在知识库中搜索相关文档（参数：query, limit）
2. document_read - 读取指定文档内容（参数：doc_id, section）
3. content_generate - 基于上下文生成内容（参数：template, context）
4. data_analysis - 生成对比表格、统计计算（参数：data, format）

## 规则
1. 每个步骤只能调用一个工具
2. 步骤之间有数据依赖的，必须按顺序执行
3. 步骤数量控制在 3-8 步
4. 输出严格的 JSON 格式

## 用户任务
{task}

## 输出格式（严格JSON）
{
    "goal": "任务目标描述",
    "steps": [
        {
            "index": 1,
            "name": "步骤名称（中文，用户可见）",
            "tool": "tool_name",
            "params": {"key": "value"},
            "reason": "为什么需要这一步"
        }
    ]
}
```

#### 8.2.2 规划输出示例

```json
{
    "goal": "分析竞品X与我司产品的功能差异，输出对比报告",
    "steps": [
        {
            "index": 1,
            "name": "搜索竞品X相关资料",
            "tool": "knowledge_search",
            "params": {"query": "竞品X 产品功能", "limit": 5},
            "reason": "获取竞品信息是对比分析的基础"
        },
        {
            "index": 2,
            "name": "搜索我方产品文档",
            "tool": "knowledge_search",
            "params": {"query": "产品功能 我方", "limit": 5},
            "reason": "获取我方产品信息用于对比"
        },
        {
            "index": 3,
            "name": "提取功能对比点",
            "tool": "document_read",
            "params": {"doc_ids": ["$step1.result[0].doc_id", "$step2.result[0].doc_id"], "section": "功能"},
            "reason": "深入阅读关键文档提取功能细节"
        },
        {
            "index": 4,
            "name": "生成对比分析表",
            "tool": "data_analysis",
            "params": {"data": "$step3.result", "format": "comparison_table"},
            "reason": "结构化展示对比结果"
        },
        {
            "index": 5,
            "name": "输出完整报告",
            "tool": "content_generation",
            "params": {"template": "competitor_analysis", "context": "$step4.result"},
            "reason": "生成最终交付物"
        }
    ]
}
```

### 8.3 Executor 设计（LangGraph）

```python
from langgraph.graph import StateGraph, END
from typing import TypedDict, List, Optional

class AgentState(TypedDict):
    task_id: str
    user_id: str
    user_role: str
    input_text: str
    plan: dict
    current_step: int
    steps_total: int
    step_results: list
    status: str  # PLANNING, RUNNING, PAUSED, COMPLETED, FAILED
    result: Optional[str]
    citations: list
    error: Optional[str]

# 定义节点
def plan_node(state: AgentState) -> AgentState:
    """规划节点：调用LLM生成执行计划"""
    plan = planner.generate_plan(state["input_text"], state["user_role"])
    state["plan"] = plan
    state["steps_total"] = len(plan["steps"])
    state["status"] = "PLANNING"
    return state

def execute_node(state: AgentState) -> AgentState:
    """执行节点：执行当前步骤"""
    step = state["plan"]["steps"][state["current_step"]]
    
    # 权限检查
    if not permissionChecker.check(state["user_role"], step["tool"]):
        state["step_results"].append({"skipped": True, "reason": "权限不足"})
        return state
    
    # 工具调用
    result = tool_executor.execute(step["tool"], step["params"], state["step_results"])
    state["step_results"].append(result)
    state["current_step"] += 1
    return state

def check_node(state: AgentState) -> str:
    """检查节点：决定下一步"""
    if state["status"] == "PAUSED":
        return "pause"
    if state["current_step"] >= state["steps_total"]:
        return "finalize"
    return "continue"

def finalize_node(state: AgentState) -> AgentState:
    """汇总节点：生成最终结果"""
    state["result"] = result_summarizer.summarize(state["step_results"], state["plan"])
    state["status"] = "COMPLETED"
    return state

# 构建图
workflow = StateGraph(AgentState)
workflow.add_node("plan", plan_node)
workflow.add_node("execute", execute_node)
workflow.add_node("finalize", finalize_node)

workflow.set_entry_point("plan")
workflow.add_conditional_edges("plan", lambda s: "execute" if s["status"] == "PLANNING" else END)
workflow.add_conditional_edges("execute", check_node, {
    "continue": "execute",
    "pause": END,
    "finalize": "finalize"
})
workflow.add_edge("finalize", END)

agent_graph = workflow.compile()
```

### 8.4 工具层设计

#### 8.4.1 Knowledge Search Tool

```java
@Component
public class KnowledgeSearchTool implements AgentTool {

    @Override
    public String getName() { return "knowledge_search"; }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext context) {
        String query = (String) params.get("query");
        int limit = (int) params.getOrDefault("limit", 5);
        Long userId = context.getUserId();
        String userRole = context.getUserRole();

        // 1. 问题 Embedding
        float[] embedding = embeddingService.embed(query);

        // 2. pgvector 语义检索（带权限过滤，通过 JOIN documents 表实现）
        List<ChunkSearchResult> results = chunkRepository.searchBySimilarity(
            embedding,
            limit,
            userId,
            userRole
        );

        // 3. 组装结果
        List<Citation> citations = convertToCitations(results);
        return ToolResult.success(citations);
    }
}
```

#### 8.4.2 Document Read Tool

```java
@Component
public class DocumentReadTool implements AgentTool {

    @Override
    public String getName() { return "document_read"; }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext context) {
        String docId = (String) params.get("doc_id");
        String section = (String) params.get("section");

        // 1. 权限校验
        Document doc = documentService.getById(docId);
        if (!permissionService.canRead(context.getUserId(), doc)) {
            return ToolResult.skipped("权限不足，无法读取文档: " + doc.getName());
        }

        // 2. 获取内容
        String content = documentService.getChunkContent(docId, section);

        // 3. 返回
        return ToolResult.success(Map.of(
            "doc_name", doc.getName(),
            "doc_security", doc.getSecurityLevel(),
            "content", content,
            "page_section", section
        ));
    }
}
```

#### 8.4.3 Content Generate Tool

```java
@Component
public class ContentGenerateTool implements AgentTool {

    @Override
    public String getName() { return "content_generation"; }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext context) {
        String template = (String) params.get("template");
        Object data = params.get("data");

        // 1. 选择 Prompt 模板
        String promptTemplate = promptService.getTemplate(template);

        // 2. 填充上下文
        String prompt = promptTemplate.replace("{{data}}", toJson(data));

        // 3. 调用 LLM
        String content = llmClient.generate(prompt, LLMModel.DEEPSEEK_CHAT);

        // 4. 返回
        return ToolResult.success(Map.of("content", content));
    }
}
```

### 8.5 模型路由策略（成本控制）

| 阶段 | 模型 | 理由 | 单次成本估算 |
|------|------|------|-------------|
| **规划阶段** | DeepSeek-V3 / Sonnet | 需要强推理能力生成结构化计划 | ¥0.01-0.03 |
| **知识搜索** | DeepSeek-Embedding | 向量化，不需要推理 | ¥0.001 |
| **文档阅读** | 不涉及LLM | 直接读取 | ¥0 |
| **内容生成** | DeepSeek-V3 | 需要高质量输出 | ¥0.02-0.05 |
| **数据分析** | DeepSeek-Chat / Qwen-mini | 结构化输出，简单推理 | ¥0.005-0.01 |
| **意图分类** | Qwen-mini / 规则引擎 | 简单分类 | ¥0.001 |

**成本估算**：单个 SuperAgent 任务（5步）≈ ¥0.05-0.10

### 8.6 人在回路（HITL）设计

```
读操作（搜索/阅读）→ 自动执行，无需确认
写操作（API调用/发送消息）→ 暂停等待用户确认
    │
    ▼
┌─────────────────────────────┐
│ ⚠️ 需要您的确认              │
│                             │
│ Agent 想要调用外部API:       │
│ • API: OA审批接口           │
│ • 参数: {请假3天}           │
│                             │
│ [✅ 确认执行] [❌ 取消]      │
└─────────────────────────────┘
```

---

## 9. 权限与安全设计

### 9.1 认证方案

**MVP 方案**：JWT + Spring Security
- 用户名密码登录（BCrypt 加密）
- JWT Token 有效期：8小时
- Refresh Token 有效期：7天
- 后续可无缝接入 SSO（CAS/OAuth2）

### 9.2 数据加密

| 数据类型 | 加密方式 | 说明 |
|---------|---------|------|
| 用户密码 | BCrypt (cost=12) | 单向哈希 |
| JWT 签名 | HS256 | 服务端密钥签名 |
| 文件存储 | MinIO SSE | 服务端加密 |
| 敏感配置 | AES-256 | 数据库连接/LLM Key |
| 传输 | TLS 1.2+ | HTTPS 强制 |

### 9.3 安全审计

```java
@Aspect
@Component
public class AuditAspect {

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        AuditLog log = new AuditLog();
        log.setUserId(SecurityContext.getUserId());
        log.setAction(auditable.action());
        log.setResourceType(auditable.resourceType());
        log.setIpAddress(RequestContext.getIp());

        try {
            Object result = pjp.proceed();
            log.setResult("SUCCESS");
            return result;
        } catch (PermissionDeniedException e) {
            log.setResult("DENIED");
            log.setDetail(Map.of("reason", e.getMessage()));
            throw e;
        } finally {
            auditLogService.save(log);
        }
    }
}
```

### 9.4 异常访问检测

| 规则 | 阈值 | 动作 |
|------|------|------|
| 单用户高频访问 | 同一文档 > 10次/小时 | 标记警告 |
| 异常时段访问 | 凌晨2:00-5:00 | 记录审计 |
| 机密文档批量访问 | > 5篇/天 | 通知管理员 |
| Agent 异常任务 | 步骤全部失败 | 通知管理员 |

---

## 10. 部署架构

### 10.1 最小配置（50-200人）

```
┌─────────────────────────────────────────┐
│           单机 Docker Compose            │
│  ┌─────────┐ ┌──────────┐ ┌──────────┐ │
│  │  Nginx   │ │ Spring   │ │ Vue      │ │
│  │  :80     │ │ Boot     │ │ Static   │ │
│  │          │ │ :8080    │ │          │ │
│  └─────────┘ └──────────┘ └──────────┘ │
│  ┌─────────────┐ ┌──────────┐           │
│  │ PostgreSQL   │ │  Redis   │           │
│  │ + pgvector   │ │  :6379   │           │
│  │  :5432       │ │          │           │
│  └─────────────┘ └──────────┘           │
│  ┌─────────┐                            │
│  │  MinIO   │                            │
│  │  :9000   │                            │
│  └─────────┘                            │
└─────────────────────────────────────────┘

服务器配置：8C16G，500G SSD
```

### 10.2 推荐配置（200-500人）

```
┌─────────────────────────────────────────────────────┐
│                  负载均衡 (Nginx/ALB)                 │
└────────┬────────────────────────────┬──────────────┘
         │                            │
┌────────┴────────┐        ┌──────────┴────────┐
│  应用服务器 #1    │        │  应用服务器 #2     │
│  ┌────────────┐  │        │  ┌────────────┐   │
│  │ Spring Boot│  │        │  │ Spring Boot│   │
│  │ (主)       │  │        │  │ (备)       │   │
│  └────────────┘  │        │  └────────────┘   │
│  ┌────────────┐  │        │                    │
│  │ Vue SPA    │  │        │                    │
│  └────────────┘  │        │                    │
└────────┬────────┘        └────────────────────┘
         │
┌────────┴────────────────────────────────────────────┐
│                 数据层                                │
│  ┌──────────────────┐ ┌──────────┐ ┌────────┐       │
│  │ PostgreSQL       │ │  Redis   │ │ MinIO  │       │
│  │ + pgvector       │ │ Sentinel │ │        │       │
│  │ 主从复制          │ │          │ │        │       │
│  └──────────────────┘ └──────────┘ └────────┘       │
└─────────────────────────────────────────────────────┘

应用服务器：2 × 8C16G
数据库服务器：1 × 8C16G + 500G SSD（pgvector 与业务数据同库）
```

### 10.3 Docker Compose 配置

```yaml
version: '3.8'

services:
  nginx:
    image: nginx:1.24
    ports: ["80:80", "443:443"]
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./frontend/build:/usr/share/nginx/html

  backend:
    build: ./backend
    ports: ["8080:8080"]
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=postgres
      - REDIS_HOST=redis
      - MINIO_ENDPOINT=http://minio:9000
      - DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY}
    depends_on: [postgres, redis, minio]

  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: agenthub
      POSTGRES_USER: agenthub
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    ports: ["5432:5432"]

  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data
    ports: ["6379:6379"]

  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_PASSWORD}
    volumes:
      - minio_data:/data
    ports: ["9000:9000"]

volumes:
  pgdata:
  redis_data:
  minio_data:
```

---

## 11. 技术选型与依据

### 11.1 技术栈总览

| 层级 | 技术 | 版本 | 选型理由 |
|------|------|------|---------|
| **前端** | Vue 3 + TypeScript | 3.x | 渐进式框架，学习曲线低，适合快速开发 |
| **前端** | Element Plus | 最新 | 企业级UI组件库，专为 Vue 3 设计 |
| **前端** | TailwindCSS | 3.x | 原子化CSS，快速定制Agent面板 |
| **后端** | Spring Boot | 3.x + Java 17 | 企业级标准，团队熟悉度高 |
| **ORM** | MyBatis-Plus | 3.x | 比JPA灵活，SQL可控 |
| **向量库** | PostgreSQL + pgvector | 16 | 与业务数据同库，无需额外运维，权限过滤天然通过JOIN实现 |
| **缓存** | Redis | 7 | 会话/状态/SSE/限流 |
| **文件存储** | MinIO | Latest | 兼容S3，私有化部署 |
| **Agent编排** | LangGraph | 0.x | Plan-and-Execute原生支持 |
| **LLM** | DeepSeek | V3 | 中文能力强，性价比高 |
| **Embedding** | BAAI/bge-large-zh | - | 中文语义理解优秀 |
| **反向代理** | Nginx | 1.24 | 负载均衡/静态资源/SSL |
| **部署** | Docker Compose | - | 一键部署，适合中小型企业 |

### 11.2 为什么选 pgvector 而非独立向量库？

| 维度 | pgvector（选用） | Milvus / Qdrant |
|------|-----------------|-----------------|
| **运维复杂度** | 极低（PG 扩展，无需额外服务） | 高（需独立部署 etcd/minio 等组件） |
| **权限过滤** | 天然支持（JOIN documents 表） | 需额外维护权限标签映射 |
| **数据一致性** | 强一致（与业务数据同事务） | 最终一致（需同步机制） |
| **性能（< 50万向量）** | HNSW 索引 < 50ms | < 50ms（相当） |
| **性能（> 100万向量）** | 开始出现瓶颈 | 优势明显 |
| **扩展性** | 单机，垂直扩展 | 分布式，水平扩展 |
| **存储成本** | 低（无需额外存储组件） | 中（需独立存储和计算资源） |
| **团队学习成本** | 低（纯 SQL） | 中（新概念 + 新工具链） |
| **备份恢复** | 与 PG 统一备份 | 需独立备份方案 |
| **MVP 推荐** | ✅ 推荐 | 数据量增长后考虑 |

> **决策**：MVP 阶段向量数预计 < 50万，pgvector + HNSW 索引完全满足性能要求（查询 < 50ms），且零额外运维成本。向量数据与业务数据在同一数据库中，权限过滤天然通过 SQL JOIN 实现，无需维护额外的权限同步。后续若数据量增长到百万级以上，可平滑迁移到 Milvus 或 Qdrant。

### 11.3 为什么选 LangGraph 而非自研？

| 维度 | LangGraph | 自研 |
|------|-----------|------|
| Plan-and-Execute | ✅ 原生支持 | 需自行实现 |
| 状态持久化 | ✅ Checkpoint | 需自行实现 |
| 断点续执 | ✅ 内置 | 需自行实现 |
| 流式输出 | ✅ 原生支持 | 需自行实现 |
| 学习成本 | 中等（Python） | 低但开发量大 |
| 社区生态 | ✅ 活跃 | 无 |

> **决策**：LangGraph 用 Python 编写，MVP 阶段可作为独立微服务部署，Java 后端通过 HTTP 调用。后续如性能瓶颈可迁移到 Java 实现。

---

## 12. 开发计划

### 12.1 里程碑甘特图

```
Week:  1  2  3  4  5  6  7  8  9  10 11 12
       ├──┼──┼──┼──┼──┼──┼──┼──┼──┼──┼──┤
M1     [==== 文档上传 + AI解析 + 基础问答 ====]
       ├──┤
       基础设施搭建
          ├──┤
          认证模块
             ├─────┤
             文档上传+解析
                   ├────┤
                   pgvector对接
                        ├────┤
                         RAG问答
                              ├────┤
M2                           [==== 权限系统 + SuperAgent ====]
                                  ├────┤
                                  权限模块
                                       ├────────┤
                                        SuperAgent框架
                                              ├────┤
                                              场景实现
M3                                              [==== 连接器 + 上线 ====]
                                                  ├────┤
                                                  连接器
                                                       ├────┤
                                                       审计+优化
                                                            ├─┤
                                                           上线
```

### 12.2 团队分工（建议）

| 角色 | 人数 | 职责 |
|------|------|------|
| 前端工程师 | 1-2 | Vue页面、Agent交互面板、SSE实时更新 |
| 后端工程师 | 2 | Spring Boot服务、API、权限、RAG Pipeline |
| AI工程师 | 1 | LangGraph Agent、Prompt工程、模型调优 |
| 全栈/DevOps | 1 | Docker部署、CI/CD、监控 |

### 12.3 关键依赖与并行任务

```
[Week 1-2] 并行任务：
  ├── 前端：项目搭建 + 基础布局 + 路由
  ├── 后端：项目搭建 + 数据库设计 + 认证模块
  └── AI：LangGraph环境搭建 + pgvector扩展配置

[Week 3-4] 并行任务：
  ├── 前端：知识库页面 + 上传组件
  ├── 后端：文档上传API + 解析服务
  └── AI：Embedding接入 + 向量入库

[Week 5-6] 并行任务：
  ├── 前端：Q&A聊天页面
  ├── 后端：权限系统 + RAG Pipeline
  └── AI：Prompt调优 + 引用溯源
```

---

## 13. 风险与缓解

### 13.1 技术风险

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| LLM 调用超时/失败 | 中 | 高 | 重试机制（指数退避）+ 降级提示 |
| 向量检索不准确 | 中 | 高 | ReRanker + Query改写 + Prompt优化 |
| 大文件解析OOM | 低 | 中 | 流式解析 + 文件大小限制（100MB） |
| SSE连接断开 | 中 | 中 | 自动重连 + 状态恢复 |
| LangGraph学习曲线 | 高 | 中 | 提前1周技术预研 + 参考官方示例 |
| 中文Embedding效果不佳 | 低 | 高 | 备选方案：text2vec-base-chinese |

### 13.2 业务风险

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| 用户上传敏感文档 | 中 | 高 | 密级设置 + 权限控制 + 审计日志 |
| Agent输出错误信息 | 中 | 中 | 引用溯源 + 免责声明 + 反馈机制 |
| 冷启动内容不足 | 高 | 中 | 提供示例文档 + 引导上传 |
| 用户期望过高 | 中 | 低 | 明确MVP边界 + 引导文案 |

### 13.3 性能风险

| 风险 | 阈值 | 缓解措施 |
|------|------|---------|
| 问答响应时间 | 目标 < 3s，告警 > 5s | 缓存常见问题 + 异步预处理 |
| Agent任务时间 | 目标 < 5min，告警 > 10min | 步骤超时限制（2min/步）+ 并行执行 |
| 并发用户数 | 目标 200并发 | Redis缓存 + 连接池优化 |
| 文档处理速度 | 目标 < 30s/篇 | 异步队列 + 进度反馈 |

---

## 附录

### A. 术语表

| 术语 | 说明 |
|------|------|
| RAG | Retrieval-Augmented Generation，检索增强生成 |
| Plan-and-Execute | 先生成完整计划再逐步执行的Agent架构 |
| SSE | Server-Sent Events，服务端推送事件 |
| HITL | Human-in-the-Loop，人在回路 |
| Embedding | 将文本转换为高维向量的过程 |
| ReRanker | 对检索结果进行二次排序的模型 |
| Chunk | 文档被切分后的文本片段 |

### B. 参考项目

- LangGraph Plan-and-Execute 示例：https://github.com/langchain-ai/langgraph
- pgvector 官方文档：https://github.com/pgvector/pgvector
- Spring AI（备选）：https://docs.spring.io/spring-ai

### C. 待确认事项

- [ ] 确认 DeepSeek API Key 和配额
- [ ] 确认飞书/企业微信连接器优先级
- [ ] 确认是否需要接入现有SSO系统
- [ ] 确认服务器资源配置
- [ ] 确认数据备份策略（每日/实时）

---

> 📌 **文档维护**：本文档随开发进展持续更新，重大变更需团队评审。
