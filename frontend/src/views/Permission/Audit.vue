<script setup lang="ts">
import { ref } from 'vue';

const logs = ref<any[]>([]);
const dateRange = ref([]);
</script>

<template>
  <div>
    <div class="flex gap-4 mb-4">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
      />
      <el-input placeholder="搜索操作人..." class="w-48" clearable />
      <el-button type="primary">查询</el-button>
    </div>

    <el-table :data="logs" border>
      <el-table-column prop="username" label="操作人" width="120" />
      <el-table-column prop="action" label="操作" width="150" />
      <el-table-column prop="resource" label="资源" />
      <el-table-column prop="ipAddress" label="IP" width="140" />
      <el-table-column prop="success" label="结果" width="80">
        <template #default="{ row }">
          <el-tag :type="row.success ? 'success' : 'danger'">
            {{ row.success ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="180" />
    </el-table>

    <el-empty v-if="logs.length === 0" description="暂无审计记录" />
  </div>
</template>
