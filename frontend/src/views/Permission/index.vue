<script setup lang="ts">
import { ref } from 'vue'
import UserManagement from './UserManagement.vue'
import DocumentPermission from './DocumentPermission.vue'
import AuditLog from './AuditLog.vue'

const activeTab = ref('users')
</script>

<template>
  <div class="permission-page">
    <el-tabs v-model="activeTab" class="permission-tabs">
      <el-tab-pane label="用户角色" name="users">
        <UserManagement v-if="activeTab === 'users'" />
      </el-tab-pane>
      <el-tab-pane label="文档授权" name="documents">
        <DocumentPermission v-if="activeTab === 'documents'" />
      </el-tab-pane>
      <el-tab-pane label="审计日志" name="audit">
        <AuditLog v-if="activeTab === 'audit'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.permission-page {
  max-width: 1100px;
}

.permission-tabs {
  background: white;
  border-radius: 10px;
  padding: 16px 24px;
  border: 1px solid #E2E8F0;
}
</style>
