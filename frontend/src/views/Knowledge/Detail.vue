<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
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

onMounted(() => {
  loadDetail()
})
</script>

<template>
  <div v-loading="loading">
    <div v-if="doc">
      <!-- 返回栏 -->
      <div class="mb-4">
        <el-button link @click="router.back()">
          <el-icon class="mr-1"><ArrowLeft /></el-icon>
          返回列表
        </el-button>
      </div>

      <el-card>
        <!-- 头部 -->
        <template #header>
          <div class="flex justify-between items-center">
            <div class="flex items-center gap-3">
              <span class="text-lg font-bold">{{ doc.title }}</span>
              <el-tag :type="securityTagType(doc.securityLevel)" size="small">
                {{ doc.securityLevel }}
              </el-tag>
              <el-tag :type="statusTagType(doc.status)" size="small">
                {{ statusLabel(doc.status) }}
              </el-tag>
            </div>
            <div class="flex items-center gap-2">
              <el-button
                v-if="doc.status === 'PROCESSING'"
                link
                type="primary"
                @click="loadDetail"
              >
                <el-icon><Refresh /></el-icon>
                刷新状态
              </el-button>
              <el-button link type="danger" @click="handleDelete">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </div>
        </template>

        <!-- 元信息 -->
        <el-descriptions :column="3" border class="mb-4">
          <el-descriptions-item label="文件名">{{ doc.fileName }}</el-descriptions-item>
          <el-descriptions-item label="文件类型">{{ doc.fileType?.toUpperCase() }}</el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ formatFileSize(doc.fileSize) }}</el-descriptions-item>
          <el-descriptions-item label="分块数量">{{ doc.chunkCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="上传时间">{{ formatDate(doc.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="文档 ID">{{ doc.id }}</el-descriptions-item>
        </el-descriptions>

        <!-- 标签 -->
        <div v-if="doc.tags && doc.tags.length > 0" class="mb-4">
          <h3 class="font-bold mb-2 text-sm text-slate-500">标签</h3>
          <div class="flex flex-wrap gap-2">
            <el-tag v-for="tag in doc.tags" :key="tag" type="info" effect="plain">
              {{ tag }}
            </el-tag>
          </div>
        </div>

        <!-- AI 摘要 -->
        <div class="mb-4">
          <h3 class="font-bold mb-2 text-sm text-slate-500">
            <el-icon class="mr-1"><MagicStick /></el-icon>
            AI 摘要
          </h3>
          <div v-if="doc.summary" class="bg-slate-50 rounded p-4 text-slate-700 leading-relaxed">
            {{ doc.summary }}
          </div>
          <el-skeleton v-else :rows="2" animated />
        </div>

        <!-- 分块内容 -->
        <div class="mb-4">
          <h3 class="font-bold mb-2 text-sm text-slate-500">
            <el-icon class="mr-1"><Files /></el-icon>
            分块内容
            <span v-if="chunks.length" class="font-normal text-slate-400">
              （共 {{ chunks.length }} 块）
            </span>
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
                  <div class="flex items-center gap-2 text-sm">
                    <el-tag size="small" type="info" effect="plain">#{{ chunk.chunkIndex + 1 }}</el-tag>
                    <span class="text-slate-400 text-xs">{{ chunk.charCount }} 字</span>
                  </div>
                </template>
                <pre class="whitespace-pre-wrap font-sans text-sm text-slate-700 leading-relaxed m-0">{{ chunk.content }}</pre>
              </el-collapse-item>
            </el-collapse>
          </div>
        </div>

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
      </el-card>
    </div>

    <el-empty v-else-if="!loading" description="文档不存在或已删除">
      <el-button type="primary" @click="router.push('/knowledge')">返回列表</el-button>
    </el-empty>
  </div>
</template>

<script lang="ts">
import { ArrowLeft, Refresh, Delete, MagicStick, Files } from '@element-plus/icons-vue'
</script>
