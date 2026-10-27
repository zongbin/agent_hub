<script setup lang="ts">
import { ref } from 'vue';

const stats = ref([
  { label: '文档总数', value: 0, icon: '📄' },
  { label: '问答次数', value: 0, icon: '💬' },
  { label: 'Agent 任务', value: 0, icon: '🤖' },
  { label: '知识片段', value: 0, icon: '🧩' },
]);
</script>

<template>
  <div>
    <el-row :gutter="16">
      <el-col v-for="s in stats" :key="s.label" :span="6">
        <el-card shadow="hover">
          <div class="flex items-center gap-4">
            <span class="text-3xl">{{ s.icon }}</span>
            <div>
              <div class="text-2xl font-bold">{{ s.value }}</div>
              <div class="text-slate-500 text-sm">{{ s.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="mt-6">
      <template #header>最近动态</template>
      <el-empty description="暂无数据" />
    </el-card>
  </div>
</template>
