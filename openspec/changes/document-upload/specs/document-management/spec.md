# Spec Delta

## Purpose

提供文档从上传到可被 RAG 检索的完整管理能力，包括文件接收、文本解析、分块、向量化入库、AI 摘要与标签生成，以及文档列表/详情/删除功能。

## ADDED Requirements

### Requirement: 文档上传接收
系统 SHALL 接收 Multipart 文件上传请求，验证文件类型（docx/pdf/pptx/xlsx/txt/md）和大小（≤100MB），并将原始文件持久化存储。

#### Scenario: 成功接收合法文件
- **WHEN** 用户上传一个 5MB 的 PDF 文件并指定密级为 INTERNAL
- **THEN** 系统存储文件，返回 201 状态，响应中包含文档 ID 和 status=PROCESSING

#### Scenario: 拒绝不支持的文件类型
- **WHEN** 用户上传一个 .exe 文件
- **THEN** 系统返回错误，提示"不支持的文件类型"

#### Scenario: 拒绝超大小限制文件
- **WHEN** 用户上传一个 150MB 的文件
- **THEN** 系统返回错误，提示"文件大小超过 100MB 限制"

#### Scenario: 未登录用户上传被拒绝
- **WHEN** 未携带有效 JWT Token 的请求调用上传接口
- **THEN** 系统返回 401 未授权错误

### Requirement: 文档文本解析
系统 SHALL 使用 Apache Tika 对支持的文档格式提取纯文本内容，解析失败时标记文档状态为 FAILED。

#### Scenario: 成功解析 Word 文档
- **WHEN** 系统处理一个已上传的 .docx 文件
- **THEN** 提取出文档的完整纯文本内容，不含二进制控制字符

#### Scenario: 成功解析 PDF 文档
- **WHEN** 系统处理一个已上传的 .pdf 文件
- **THEN** 提取出文档的文本内容（文本型 PDF）

#### Scenario: 解析失败标记
- **WHEN** 解析过程中 Tika 抛出异常（如文件损坏）
- **THEN** 文档状态被标记为 FAILED，错误信息记录到文档元数据

### Requirement: 文档文本分块
系统 SHALL 将解析后的文本按 512 token + 50 token 重叠的滑动窗口分块，优先按段落/标题边界切分。

#### Scenario: 长文本分块
- **WHEN** 解析后的文本超过 512 token
- **THEN** 系统生成多个文本块，相邻块之间有 50 token 的重叠

#### Scenario: 短文本单块
- **WHEN** 解析后的文本不超过 512 token
- **THEN** 系统生成单个文本块

#### Scenario: 段落边界优先
- **WHEN** 文本中存在段落分隔（换行/Markdown 标题）
- **THEN** 分块优先在段落边界处切分，不破坏段落完整性

### Requirement: 文档向量化入库
系统 SHALL 对每个文本分块调用灵积平台（DashScope）Embedding API 获取 1024 维向量，并将 chunk 内容和向量写入 `document_chunks` 表。API 端点、密钥、模型、维度、批量大小和超时时间均可通过配置文件配置。

#### Scenario: 分块向量写入成功
- **WHEN** 文本分块完成后
- **THEN** 每个 chunk 的内容和 embedding 被持久化到 `document_chunks` 表，包含 document_id、chunk_index、content、embedding

#### Scenario: Embedding 服务不可用
- **WHEN** Embedding 服务调用超时或返回错误
- **THEN** 文档状态被标记为 FAILED，记录错误信息，不写入部分数据

### Requirement: AI 摘要与标签生成
系统 SHALL 在文档上传完成后调用 LLM 生成文档摘要（≤200 字）和自动标签（3-5 个），回写到 `documents` 表。

#### Scenario: 摘要和标签生成成功
- **WHEN** 文档向量入库完成后
- **THEN** documents 表的 summary 和 tags 字段被更新，文档状态变为 ACTIVE

#### Scenario: 摘要生成失败降级
- **WHEN** LLM 服务调用失败
- **THEN** 文档仍标记为 ACTIVE，summary 使用文本前 200 字截断，tags 为空数组

### Requirement: 文档列表查询
系统 SHALL 提供分页查询文档列表接口，支持按密级、标签、状态筛选，仅返回当前用户有权限查看的文档。

#### Scenario: 分页查询
- **WHEN** 用户请求第 1 页，每页 20 条
- **THEN** 系统返回最多 20 条文档记录和总数

#### Scenario: 权限过滤
- **WHEN** 用户查询文档列表
- **THEN** 系统仅返回用户角色和密级权限范围内的文档，不包含无权限的 CONFIDENTIAL 文档

### Requirement: 文档详情查询
系统 SHALL 提供根据文档 ID 查询详情的接口，返回文档元信息（标题、密级、标签、摘要、状态、创建时间等）。

#### Scenario: 查询存在的文档
- **WHEN** 用户查询一个已存在的文档 ID
- **THEN** 系统返回该文档的完整元信息

#### Scenario: 查询不存在的文档
- **WHEN** 查询的文档 ID 不存在
- **THEN** 系统返回 404 错误

#### Scenario: 无权限文档拒绝
- **WHEN** 用户查询一个自己无权限查看的 CONFIDENTIAL 文档
- **THEN** 系统返回 403 权限不足错误

### Requirement: 文档删除
系统 SHALL 提供逻辑删除文档的接口，删除后文档不再出现在列表中，但数据保留在数据库中。

#### Scenario: 成功删除文档
- **WHEN** 用户删除自己有权限的文档
- **THEN** 文档被标记为删除（deleted=1），后续列表查询不返回该文档

#### Scenario: 删除关联分块
- **WHEN** 文档被删除
- **THEN** 文档状态标记为删除，检索时不再命中该文档的分块

### Requirement: 上传权限控制
系统 SHALL 要求上传用户具备 EDITOR 或以上角色，且上传操作的密级不得超过用户自身权限范围。

#### Scenario: EDITOR 角色上传成功
- **WHEN** EDITOR 角色用户上传文档
- **THEN** 系统接受上传请求

#### Scenario: VIEWER 角色上传被拒绝
- **WHEN** VIEWER 角色用户上传文档
- **THEN** 系统返回 403 权限不足错误

### Requirement: 上传审计日志
系统 SHALL 记录每次文档上传操作的审计日志，包含操作人、操作类型、文件名、成功/失败状态。

#### Scenario: 上传成功记录
- **WHEN** 用户成功上传文档
- **THEN** audit_logs 表中新增一条 action=UPLOAD、resource=DOCUMENT、success=1 的记录

#### Scenario: 上传失败记录
- **WHEN** 上传过程中发生错误
- **THEN** audit_logs 表中新增一条 action=UPLOAD、resource=DOCUMENT、success=0 的记录，包含错误详情
