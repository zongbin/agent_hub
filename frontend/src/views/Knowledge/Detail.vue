<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Refresh, Delete, MagicStick, Files } from '@element-plus/icons-vue'
import { knowledgeApi, type DocumentChunk, type DocumentVO } from '@/api/knowledge'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const doc = ref<DocumentVO | null>(null)
const chunks = ref<DocumentChunk[]>([])
const chunksLoading = ref(false)
/** 折叠面板当前展开的分块（手风琴模式，同一时刻只展开一个） */
const activeChunks = ref<number | string>('')

/** 加载文档详情 */
const loadDetail = async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    if (isNaN(id)) {
      ElMessage.error('无效的文档 ID')
      router.push('/knowledge')
      return
    }
    doc.value = await knowledgeApi.detail(id)
    await loadChunks(id)
  } catch (error: unknown) {
    const err = error as { message?: string; response?: { status?: number } }
    if (err.response?.status === 404) {
      ElMessage.error('文档不存在')
    } else if (err.response?.status === 403) {
      ElMessage.error('权限不足，无法访问该文档')
    } else {
      ElMessage.error(err.message || '加载文档详情失败')
    }
    router.push('/knowledge')
  } finally {
    loading.value = false
  }
}

/** 加载文档分块内容（仅已就绪的文档有分块） */
const loadChunks = async (id: number) => {
  chunks.value = []
  chunksLoading.value = true
  try {
    chunks.value = await knowledgeApi.chunks(id)
  } catch (error) {
    // 分块加载失败不影响详情展示
    console.warn('加载分块失败', error)
  } finally {
    chunksLoading.value = false
  }
}

/** 删除文档 */
const handleDelete = async () => {
  if (!doc.value) return
  try {
    await ElMessageBox.confirm(`确认删除文档「${doc.value.title}」？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await knowledgeApi.delete(doc.value.id)
    ElMessage.success('文档已删除')
    router.push('/knowledge')
  } catch {
    // 用户取消
  }
}

/** 格式化文件大小 */
const formatFileSize = (bytes: number): string => {
  if (!bytes) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

/** 格式化日期 */
const formatDate = (dateStr: string): string => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

/** 状态标签样式 */
const statusTagType = (status: string): string => {
  const map: Record<string, string> = {
    PROCESSING: 'warning',
    ACTIVE: 'success',
    FAILED: 'danger'
  }
  return map[status] || 'info'
}

const statusLabel = (status: string): string => {
  const map: Record<string, string> = {
    PROCESSING: '处理中',
    ACTIVE: '已就绪',
    FAILED: '处理失败'
  }
  return map[status] || status
}

/** 安全等级标签样式 */
const securityTagType = (level: string): string => {
  const map: Record<string, string> = {
    PUBLIC: 'success',
    INTERNAL: 'warning',
    CONFIDENTIAL: 'danger'
  }
  return map[level] || 'info'
}

/** 安全等级显示文本 */
const securityLabel = (level: string): string => {
  const map: Record<string, string> = {
    PUBLIC: '公开',
    INTERNAL: '内部',
    CONFIDENTIAL: '机密'
  }
  return map[level] || level
}

onMounted(() => {
  loadDetail()
})
</script>

<template>
  <div v-loading="loading" class="detail-page">
    <div v-if="doc">
      <!-- 返回栏 -->
      <div class="mb-4">
        <el-button link class="back-btn" @click="router.back()">
          <el-icon class="mr-1"><ArrowLeft /></el-icon>
          返回列表
        </el-button>
      </div>

      <!-- 头部概览 -->
      <section class="doc-hero glass-card glow-strip">
        <div class="doc-hero-main">
          <div class="doc-hero-tags">
            <el-tag :type="securityTagType(doc.securityLevel)" size="small">
              {{ securityLabel(doc.securityLevel) }}
            </el-tag>
            <el-tag :type="statusTagType(doc.status)" size="small">
              {{ statusLabel(doc.status) }}
            </el-tag>
            <span v-if="doc.tags && doc.tags.length" class="tag-row">
              <el-tag v-for="tag in doc.tags" :key="tag" type="info" effect="plain" size="small">
                {{ tag }}
              </el-tag>
            </span>
          </div>
          <h1 class="doc-title">{{ doc.title }}</h1>
          <p class="doc-file">
            {{ doc.fileName }} · {{ doc.fileType?.toUpperCase() }} ·
            {{ formatFileSize(doc.fileSize) }}
          </p>
        </div>
        <div class="doc-hero-actions">
          <el-button
            v-if="doc.status === 'PROCESSING'"
            type="primary"
            plain
            @click="loadDetail"
          >
            <el-icon class="mr-1"><Refresh /></el-icon>
            刷新状态
          </el-button>
          <el-button type="danger" plain @click="handleDelete">
            <el-icon class="mr-1"><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </section>

      <!-- 元信息 -->
      <div class="meta-grid glass-card">
        <div class="meta-item">
          <span class="meta-label">分块数量</span>
          <span class="meta-value">{{ doc.chunkCount ?? 0 }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">文档 ID</span>
          <span class="meta-value">{{ doc.id }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">上传时间</span>
          <span class="meta-value">{{ formatDate(doc.createdAt) }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">安全等级</span>
          <span class="meta-value">{{ securityLabel(doc.securityLevel) }}</span>
        </div>
      </div>

      <!-- AI 摘要 -->
      <section class="panel glass-card">
        <h3 class="panel-title">
          <span class="panel-icon"><el-icon :size="15"><MagicStick /></el-icon></span>
          AI 摘要
        </h3>
        <div v-if="doc.summary" class="summary-box">{{ doc.summary }}</div>
        <el-skeleton v-else :rows="2" animated />
      </section>

      <!-- 分块内容 -->
      <section class="panel glass-card">
        <h3 class="panel-title">
          <span class="panel-icon"><el-icon :size="15"><Files /></el-icon></span>
          分块内容
          <span v-if="chunks.length" class="panel-count">共 {{ chunks.length }} 块</span>
        </h3>

        <div v-loading="chunksLoading">
          <el-empty v-if="!chunksLoading && chunks.length === 0" description="暂无分块内容" :image-size="60" />
          <el-collapse v-else v-model="activeChunks" accordion>
            <el-collapse-item
              v-for="chunk in chunks"
              :key="chunk.chunkIndex"
              :name="chunk.chunkIndex"
            >
              <template #title>
                <div class="chunk-title">
                  <el-tag size="small" type="info" effect="plain">#{{ chunk.chunkIndex + 1 }}</el-tag>
                  <span class="chunk-chars">{{ chunk.charCount }} 字</span>
                </div>
              </template>
              <pre class="chunk-content">{{ chunk.content }}</pre>
            </el-collapse-item>
          </el-collapse>
        </div>
      </section>

      <!-- 处理中提示 -->
      <el-alert
        v-if="doc.status === 'PROCESSING'"
        type="warning"
        :closable="false"
        show-icon
        class="mt-4"
      >
        <template #title>
          文档正在处理中，系统正在进行文本解析、分块和向量化，请稍后刷新查看结果。
        </template>
      </el-alert>

      <!-- 处理失败提示 -->
      <el-alert
        v-if="doc.status === 'FAILED'"
        type="error"
        :closable="false"
        show-icon
        class="mt-4"
      >
        <template #title>
          文档处理失败，请检查文件格式是否正确，或重新上传。
        </template>
      </el-alert>
    </div>

    <el-empty v-else-if="!loading" description="文档不存在或已删除">
      <el-button type="primary" @click="router.push('/knowledge')">返回列表</el-button>
    </el-empty>
  </div>
</template>

<style scoped>
.detail-page {
  animation: rise 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.back-btn {
  color: var(--text-dim);
}

.back-btn:hover {
  color: var(--accent-text);
}

/* ===== 头部概览 ===== */
.doc-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 24px 26px;
  margin-bottom: 14px;
  background:
    linear-gradient(120deg, rgba(124, 92, 255, 0.16) 0%, rgba(34, 211, 238, 0.08) 60%, transparent 100%),
    var(--veil);
}

.doc-hero-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.tag-row {
  display: inline-flex;
  gap: 6px;
}

.doc-title {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text);
  margin: 0 0 8px;
  word-break: break-all;
}

.doc-file {
  font-size: 12px;
  color: var(--text-faint);
  margin: 0;
}

.doc-hero-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

/* ===== 元信息 ===== */
.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  padding: 4px;
  margin-bottom: 14px;
  background: var(--border);
  overflow: hidden;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 14px 18px;
  background: var(--veil-solid);
}

.meta-label {
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-faint);
}

.meta-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  font-variant-numeric: tabular-nums;
}

/* ===== 面板 ===== */
.panel {
  padding: 20px 24px;
  margin-bottom: 14px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin: 0 0 16px;
}

.panel-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 9px;
  color: var(--primary);
  background: var(--primary-soft);
  border: 1px solid var(--primary-border);
}

.panel-count {
  font-size: 12px;
  font-weight: 400;
  color: var(--text-faint);
}

/* ===== 摘要 ===== */
.summary-box {
  padding: 16px 18px;
  border-radius: var(--radius-md);
  font-size: 13.5px;
  line-height: 1.95;
  color: var(--text-soft);
  background: var(--primary-softer);
  border: 1px solid var(--primary-border);
}

/* ===== 分块 ===== */
.chunk-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.chunk-chars {
  font-size: 11px;
  color: var(--text-faint);
}

.chunk-content {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.9;
  color: var(--text-soft);
}

@media (max-width: 900px) {
  .meta-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .doc-hero {
    flex-direction: column;
  }
}

/* ============================================================
   移动端自适应
   ============================================================ */
@media (max-width: 768px) {
  .doc-hero {
    padding: 16px 16px;
    gap: 12px;
  }

  .doc-title {
    font-size: 18px;
  }

  .doc-hero-actions {
    width: 100%;
  }

  .doc-hero-actions .el-button {
    flex: 1;
  }

  .meta-item {
    padding: 12px 14px;
  }

  .meta-value {
    font-size: 14px;
  }

  .panel {
    padding: 16px 14px;
  }

  .summary-box {
    padding: 12px 14px;
    font-size: 13px;
  }
}
</style>
