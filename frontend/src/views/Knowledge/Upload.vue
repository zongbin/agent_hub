<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type UploadInstance } from 'element-plus';

const router = useRouter();
const uploadRef = ref<UploadInstance>();
const uploading = ref(false);

const form = reactive({
  securityLevel: 'INTERNAL',
  tags: '',
});

const handleUpload = async () => {
  // TODO: 对接后端上传接口
  uploading.value = true;
  try {
    ElMessage.success('上传成功');
    router.push('/knowledge/list');
  } finally {
    uploading.value = false;
  }
};
</script>

<template>
  <div>
    <el-card>
      <template #header>上传文档</template>
      <el-form :model="form" label-width="100px">
        <el-form-item label="文件">
          <el-upload
            ref="uploadRef"
            drag
            action="/api/knowledge/upload"
            :auto-upload="false"
            multiple
            accept=".docx,.pdf,.pptx,.xlsx,.txt,.md"
          >
            <div class="p-8 text-slate-400">
              拖拽文件到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="text-slate-400 text-sm">
                支持 docx / pdf / pptx / xlsx / txt / md，单文件最大 100MB
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="安全等级">
          <el-select v-model="form.securityLevel">
            <el-option label="公开" value="PUBLIC" />
            <el-option label="内部" value="INTERNAL" />
            <el-option label="机密" value="CONFIDENTIAL" />
          </el-select>
        </el-form-item>

        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="逗号分隔，如：产品,手册" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="uploading" @click="handleUpload">
            上传
          </el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
