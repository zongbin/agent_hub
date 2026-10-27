<script setup lang="ts">
import { ref } from 'vue';

const documents = ref<any[]>([]);
const searchKey = ref('');
</script>

<template>
  <div>
    <div class="flex justify-between mb-4">
      <el-input
        v-model="searchKey"
        placeholder="搜索文档..."
        class="w-64"
        clearable
      />
      <el-button type="primary" @click="$router.push('/knowledge/upload')">
        上传文档
      </el-button>
    </div>

    <el-table :data="documents" border>
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="fileType" label="类型" width="100" />
      <el-table-column prop="securityLevel" label="安全等级" width="120" />
      <el-table-column prop="createdAt" label="上传时间" width="180" />
      <el-table-column label="操作" width="180">
        <template #default>
          <el-button link type="primary">查看</el-button>
          <el-button link type="danger">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="documents.length === 0" description="暂无文档" />
  </div>
</template>
