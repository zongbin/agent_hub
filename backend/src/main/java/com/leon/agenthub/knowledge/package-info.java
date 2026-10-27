/**
 * 知识库模块 — 文档上传、解析、分片、索引。
 *
 * <ul>
 *   <li>{@code controller}  — 文档上传 / 列表 / 详情 / 删除接口</li>
 *   <li>{@code service}     — 上传编排：本地磁盘存储 → 解析 → 分片 → 嵌入 → 入库</li>
 *   <li>{@code parser}      — Tika 封装，支持 docx / pptx / xlsx / pdf / txt / md</li>
 *   <li>{@code chunker}     — 文本分片策略（512 token + 50 重叠），预留语义分片扩展点</li>
 *   <li>{@code dto}         — DocumentUpload / DocumentVO / DocumentQuery</li>
 *   <li>{@code config}      — 分片参数、支持的文件类型配置</li>
 * </ul>
 *
 * <p>存储策略：MVP 使用本地文件系统（{@code agenthub.storage.root-path}），
 * 后续迁移到 MinIO / S3 时仅替换 service 层存储实现，上层接口不变。
 */
package com.leon.agenthub.knowledge;
