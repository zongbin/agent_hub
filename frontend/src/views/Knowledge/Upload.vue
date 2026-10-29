<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type UploadInstance } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
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

/** 处理流程步骤 */
const steps = ['上传文件', '文本解析', '智能分块', '向量化入库', '生成摘要标签']
</script>

<template>
  <div class="upload-page">
    <section class="upload-card glass-card glow-strip">
      <!-- 头部 -->
      <header class="upload-head">
        <div class="upload-head-icon">
          <el-icon :size="22"><UploadFilled /></el-icon>
        </div>
        <div>
          <h1 class="upload-title">上传文档</h1>
          <p class="upload-desc">
            上传后系统将自动解析文档内容、生成摘要和标签，并向量化入库供知识库检索。
          </p>
        </div>
      </header>

      <el-form :model="form" label-width="100px" class="upload-form">
        <el-form-item label="文件" required>
          <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            accept=".docx,.pdf,.pptx,.xlsx,.txt,.md"
          >
            <div class="upload-drop">
              <div class="drop-orb">
                <el-icon class="text-4xl"><UploadFilled /></el-icon>
              </div>
              <div class="drop-text">拖拽文件到此处，或<em>点击上传</em></div>
            </div>
            <template #tip>
              <div class="upload-tip">
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
          <div class="upload-tip mt-1">标签有助于文档分类和检索</div>
        </el-form-item>

        <el-form-item label="处理流程">
          <div class="flow-steps">
            <div v-for="(step, index) in steps" :key="step" class="flow-step">
              <span class="flow-index">{{ index + 1 }}</span>
              <span class="flow-label">{{ step }}</span>
              <span v-if="index < steps.length - 1" class="flow-line"></span>
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="uploading"
            :disabled="!selectedFile"
            @click="handleUpload"
            class="w-full submit-btn"
          >
            {{ uploading ? '上传中...' : '开始上传' }}
          </el-button>
          <el-button @click="router.back()" class="w-full mt-2">取消</el-button>
        </el-form-item>
      </el-form>
    </section>
  </div>
</template>

<style scoped>
.upload-page {
  max-width: 760px;
  margin: 0 auto;
  animation: rise 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.upload-card {
  padding: 28px 30px 30px;
}

/* ===== 头部 ===== */
.upload-head {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding-bottom: 20px;
  margin-bottom: 22px;
  border-bottom: 1px solid var(--border);
}

.upload-head-icon {
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #7C5CFF, #22D3EE);
  box-shadow: 0 0 26px -6px rgba(124, 92, 255, 0.95);
}

.upload-title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text);
  margin: 2px 0 6px;
}

.upload-desc {
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--text-faint);
  margin: 0;
}

/* ===== 上传区 ===== */
.upload-drop {
  padding: 30px 0;
  text-align: center;
}

.drop-orb {
  width: 62px;
  height: 62px;
  margin: 0 auto 14px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent-text);
  background: var(--tint-cyan-bg);
  border: 1px solid var(--tint-cyan-border);
  animation: float 6s ease-in-out infinite;
}

.drop-text {
  font-size: 13px;
  color: var(--text-dim);
}

.drop-text em {
  color: var(--accent-text);
  font-style: normal;
}

.upload-tip {
  color: var(--text-faint);
  font-size: 12px;
}

/* ===== 处理流程 ===== */
.flow-steps {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 6px;
}

.flow-step {
  position: relative;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 6px 12px 6px 8px;
  border-radius: 999px;
  background: var(--surface-soft);
  border: 1px solid var(--border);
}

.flow-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 17px;
  height: 17px;
  border-radius: 50%;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #7C5CFF, #22D3EE);
}

.flow-label {
  font-size: 12px;
  color: var(--text-dim);
}

.submit-btn {
  font-weight: 600;
  letter-spacing: 0.05em;
}
</style>
