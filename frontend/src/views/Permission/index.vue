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
  width: 100%;
  animation: rise 0.45s cubic-bezier(0.22, 1, 0.36, 1) backwards;
}

.permission-tabs {
  background: var(--glass);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 6px 22px 22px;
  backdrop-filter: blur(16px);
  box-shadow: var(--shadow-card);
}

.permission-tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.permission-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 500;
  height: 48px;
  line-height: 48px;
}

/* ============================================================
   移动端自适应
   ============================================================ */
@media (max-width: 768px) {
  .permission-tabs {
    padding: 4px 10px 14px;
  }

  .permission-tabs :deep(.el-tabs__item) {
    font-size: 13px;
    height: 42px;
    line-height: 42px;
    padding: 0 10px;
  }
}
</style>
