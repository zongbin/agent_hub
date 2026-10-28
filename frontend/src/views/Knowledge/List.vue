<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadInstance } from 'element-plus'
import axios from 'axios'
import { knowledgeApi, type DocumentVO, type DocumentListResponse } from '@/api/knowledge'

const router = useRouter()
const uploadRef = ref<UploadInstance>()
const loading = ref(false)
const documents = ref<DocumentVO[]>([])

const searchKey = ref('')
const filterSecurity = ref('')
const filterStatus = ref('')

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const uploading = ref(false)
const uploadProgress = ref(0)

const form = reactive({
  securityLevel: 'INTERNAL',
  tags: ''
})

const selectedFile = ref<File | null>(null)
const dialogVisible = ref(false)

const statusOptions = [
  { label: '全部', value: '' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已就绪', value: 'ACTIVE' },
  { label: '处理失败', value: 'FAILED' }
]

const securityOptions = [
  { label: '全部', value: '' },
  { label: '公开', value: 'PUBLIC' },
  { label: '内部', value: 'INTERNAL' },
  { label: '机密', value: 'CONFIDENTIAL' }
]

/** 加载文档列表 */
const loadDocuments = async () => {
  loading.value = true
  try {
    const res = await knowledgeApi.list({
      page: pagination.page,
      size: pagination.size,
      securityLevel: filterSecurity.value || undefined,
      status: filterStatus.value ? statusValueToInt(filterStatus.value) : undefined
    })
    documents.value = res.list ?? []
    pagination.total = res.total ?? 0
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '加载文档列表失败')
  } finally {
    loading.value = false
  }
}

const statusValueToInt = (status: string): number | undefined => {
  const map: Record<string, number> = { PROCESSING: 2, ACTIVE: 1, FAILED: 0 }
  return map[status]
}

const statusToInt = (status: string): number => {
  const map: Record<string, number> = { PROCESSING: 2, ACTIVE: 1, FAILED: 0, DELETED: 0 }
  return map[status] ?? 1
}

/** 搜索 */
const handleSearch = () => {
  pagination.page = 1
  loadDocuments()
}

/** 筛选变化 */
const handleFilterChange = () => {
  pagination.page = 1
  loadDocuments()
}

/** 分页变化 */
const handlePageChange = (page: number) => {
  pagination.page = page
  loadDocuments()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  loadDocuments()
}

/** 查看文档详情 */
const handleView = (row: DocumentVO) => {
  router.push(`/knowledge/${row.id}`)
}

/** 删除文档 */
const handleDelete = async (row: DocumentVO) => {
  try {
    await ElMessageBox.confirm(`确认删除文档「${row.title}」？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await knowledgeApi.delete(row.id)
    ElMessage.success('文档已删除')
    loadDocuments()
  } catch (error) {
    // 用户取消
  }
}

/** 文件选择变化 */
const handleFileChange = (uploadFile: { raw?: File }) => {
  selectedFile.value = uploadFile.raw || null
}

/** 快速上传 */
const handleQuickUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  uploading.value = true
  uploadProgress.value = 0

  try {
    const requestMeta = JSON.stringify({
      title: selectedFile.value.name,
      securityLevel: form.securityLevel,
      tags: form.tags
    })

    const res = await knowledgeApi.upload(selectedFile.value, requestMeta)
    ElMessage.success(`上传成功！文档 ID: ${res.id}，正在处理中...`)
    dialogVisible.value = false
    selectedFile.value = null
    loadDocuments()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

/** 格式化文件大小 */
const formatFileSize = (bytes: number): string => {
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
    minute: '2-digit'
  })
}

/** 状态标签样式 */
const statusTagType = (status: string): string => {
  const map: Record<string, string> = {
    PROCESSING: 'warning',
    ACTIVE: 'success',
    FAILED: 'danger',
    DELETED: 'info'
  }
  return map[status] || 'info'
}

/** 状态显示文本 */
const statusLabel = (status: string): string => {
  const map: Record<string, string> = {
    PROCESSING: '处理中',
    ACTIVE: '已就绪',
    FAILED: '处理失败',
    DELETED: '已删除'
  }
  return map[status] || status
}

onMounted(() => {
  loadDocuments()
})
</script>

<template>
  <div>
    <!-- 顶部操作栏 -->
    <div class="flex justify-between items-center mb-4">
      <div class="flex items-center gap-3">
        <el-input
          v-model="searchKey"
          placeholder="搜索文档标题..."
          class="w-64"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="filterSecurity"
          placeholder="安全等级"
          class="w-32"
          clearable
          @change="handleFilterChange"
        >
          <el-option
            v-for="opt in securityOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-select
          v-model="filterStatus"
          placeholder="状态"
          class="w-32"
          clearable
          @change="handleFilterChange"
        >
          <el-option
            v-for="opt in statusOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>
      <div class="flex items-center gap-2">
        <el-button type="primary" @click="dialogVisible = true">
          <el-icon class="mr-1"><Plus /></el-icon>
          上传文档
        </el-button>
        <el-button :icon="Refresh" @click="loadDocuments" circle title="刷新" />
      </div>
    </div>

    <!-- 文档表格 -->
    <el-table :data="documents" border v-loading="loading" stripe>
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link type="primary" @click="handleView(row)" :underline="false">
            {{ row.title }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column prop="fileType" label="类型" width="80">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.fileType?.toUpperCase() }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="fileSize" label="大小" width="100">
        <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="securityLevel" label="安全等级" width="100">
        <template #default="{ row }">
          <el-tag
            size="small"
            :type="row.securityLevel === 'CONFIDENTIAL' ? 'danger' : row.securityLevel === 'INTERNAL' ? 'warning' : 'success'"
          >
            {{ row.securityLevel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTagType(row.status)">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="chunkCount" label="分块数" width="80" align="center" />
      <el-table-column prop="createdAt" label="上传时间" width="170">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 空状态 -->
    <el-empty v-if="!loading && documents.length === 0" description="暂无文档" class="mt-8">
      <el-button type="primary" @click="dialogVisible = true">上传第一篇文档</el-button>
    </el-empty>

    <!-- 分页 -->
    <div class="flex justify-end mt-4">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- 上传对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="上传文档"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="文件" required>
          <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            accept=".docx,.pdf,.pptx,.xlsx,.txt,.md"
          >
            <div class="p-6 text-slate-400">
              <el-icon class="text-3xl mb-2"><UploadFilled /></el-icon>
              <div>拖拽文件到此处，或<em>点击上传</em></div>
            </div>
            <template #tip>
              <div class="text-slate-400 text-xs">
                支持 docx / pdf / pptx / xlsx / txt / md，单文件最大 100MB
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="安全等级">
          <el-select v-model="form.securityLevel" class="w-full">
            <el-option label="公开" value="PUBLIC" />
            <el-option label="内部" value="INTERNAL" />
            <el-option label="机密" value="CONFIDENTIAL" />
          </el-select>
        </el-form-item>

        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="逗号分隔，如：产品,手册" />
        </el-form-item>

        <el-form-item v-if="uploading" label="进度">
          <el-progress :percentage="uploadProgress" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleQuickUpload">
          上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Search, Plus, Refresh } from '@element-plus/icons-vue'
</script>
