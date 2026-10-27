<script setup lang="ts">
import { ref } from 'vue';

const doc = ref<any>(null);
</script>

<template>
  <div v-if="doc">
    <el-card>
      <template #header>
        <div class="flex justify-between items-center">
          <span class="text-lg font-bold">{{ doc.title }}</span>
          <el-tag>{{ doc.securityLevel }}</el-tag>
        </div>
      </template>
      <div class="mb-4">
        <el-tag v-for="tag in doc.tags" :key="tag" class="mr-2">{{ tag }}</el-tag>
      </div>
      <div class="mb-4">
        <h3 class="font-bold mb-2">AI 摘要</h3>
        <p class="text-slate-600">{{ doc.summary }}</p>
      </div>
      <div>
        <h3 class="font-bold mb-2">原文预览</h3>
        <el-empty description="原文预览占位" />
      </div>
    </el-card>
  </div>
  <el-empty v-else description="加载中..." />
</template>
