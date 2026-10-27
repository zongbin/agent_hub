<script setup lang="ts">
import { ref } from 'vue';

const users = ref<any[]>([]);
const roleOptions = [
  { label: '管理员', value: 'ADMIN' },
  { label: '编辑', value: 'EDITOR' },
  { label: '查看者', value: 'VIEWER' },
  { label: '访客', value: 'GUEST' },
];
</script>

<template>
  <div>
    <div class="flex justify-between mb-4">
      <el-input v-model="searchKey" placeholder="搜索用户..." class="w-64" clearable />
      <el-button type="primary">添加用户</el-button>
    </div>

    <el-table :data="users" border>
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="role" label="角色" width="120" />
      <el-table-column prop="department" label="部门" width="150" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default>
          <el-button link type="primary">编辑</el-button>
          <el-button link type="danger">禁用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="users.length === 0" description="暂无用户" />
  </div>
</template>
