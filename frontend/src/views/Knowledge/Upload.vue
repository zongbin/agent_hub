<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type UploadInstance } from 'element-plus'
import { knowledgeApi } from '@/api/knowledge'

const router = useRouter()
const uploadRef = ref<UploadInstance>()
const uploading = ref(false)
const uploadProgress = ref(0)

const form = reactive({
  title: '',
  securityLevel: 'INTERNAL',
  tags: ''
})

const selectedFile = ref<File | null>(null)

const handleFileChange = (uploadFile: { raw?: File }) => {
  selectedFile.value = uploadFile.raw || null
  // 自动填充标题（如果未手动输入）
  if (!form.title && uploadFile.raw?.name) {
    const name = uploadFile.raw.name
    form.title = name.replace(/\.[^.]+$/, '')
  }
}

const handleUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  uploading.value = true
  uploadProgress.value = 0

  try {
    const requestMeta = JSON.stringify({
      title: form.title || selectedFile.value.name,
      securityLevel: form.securityLevel,
      tags: form.tags
    })

    const res = await knowledgeApi.upload(selectedFile.value, requestMeta)
    ElMessage.success(`上传成功！文档 ID: ${res.id}，正在处理中...`)
    router.push('/knowledge')
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '上传失败，请重试')
  } finally {
    uploading.value = false
    uploadProgress.value = 0
  }
}
</script>

<template>
  <div class="max-w-2xl mx-auto">
    <el-card>
      <template #header>
        <div class="flex items-center gap-2">
          <el-icon :size="20"><UploadFilled /></el-icon>
          <span class="text-lg font-semibold">上传文档</span>
        </div>
        <p class="text-slate-400 text-sm mt-1 mb-0">
          上传后系统将自动解析文档内容、生成摘要和标签，并向量化入库供知识库检索。
        </p>
      </template>

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
            <div class="p-8 text-slate-400">
              <el-icon class="text-4xl mb-2"><UploadFilled /></el-icon>
              <div class="text-base">拖拽文件到此处，或<em>点击上传</em></div>
            </div>
            <template #tip>
              <div class="text-slate-400 text-xs">
                支持 docx / pdf / pptx / xlsx / txt / md，单文件最大 100MB
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item v-if="uploading" label="进度">
          <el-progress :percentage="uploadProgress" :stroke-width="10" striped striped-flow />
        </el-form-item>

        <el-form-item label="文档标题">
          <el-input v-model="form.title" placeholder="留空则使用文件名" />
        </el-form-item>

        <el-form-item label="安全等级">
          <el-select v-model="form.securityLevel" class="w-full">
            <el-option label="公开（所有人可访问）" value="PUBLIC" />
            <el-option label="内部（仅正式员工）" value="INTERNAL" />
            <el-option label="机密（仅授权人员）" value="CONFIDENTIAL" />
          </el-select>
        </el-form-item>

        <el-form-item label="标签">
          <el-input
            v-model="form.tags"
            placeholder="逗号分隔，如：产品,手册,规范"
          />
          <div class="text-slate-400 text-xs mt-1">标签有助于文档分类和检索</div>
        </el-form-item>

        <el-form-item label="处理流程">
          <el-steps :active="0" finish-status="success" simple>
            <el-step title="上传文件" />
            <el-step title="文本解析" />
            <el-step title="智能分块" />
            <el-step title="向量化入库" />
            <el-step title="生成摘要标签" />
          </el-steps>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="uploading"
            :disabled="!selectedFile"
            @click="handleUpload"
            class="w-full"
          >
            {{ uploading ? '上传中...' : '开始上传' }}
          </el-button>
          <el-button @click="router.back()" class="w-full mt-2">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script lang="ts">
import { UploadFilled } from '@element-plus/icons-vue'
</script>
