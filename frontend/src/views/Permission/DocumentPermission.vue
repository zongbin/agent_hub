<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { DocPermissionItem, UserItem } from '@/types/permission'
import { permissionApi } from '@/api/permission'

const permissions = ref<DocPermissionItem[]>([])
const users = ref<UserItem[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const selectedDocId = ref<number | null>(null)
const selectedDocName = ref('')
const grantForm = ref({
  userId: null as number | null,
  permission: 'READ'
})

const permissionOptions = [
  { label: '只读', value: 'READ' },
  { label: '读写', value: 'WRITE' }
]

async function fetchPermissions() {
  loading.value = true
  try {
    permissions.value = await permissionApi.listDocumentPermissions(0)
  } catch {
    // 模拟数据演示
    permissions.value = []
  } finally {
    loading.value = false
  }
}

async function fetchUsers() {
  try {
    users.value = await permissionApi.listUsers()
  } catch {
    users.value = []
  }
}

function openGrantDialog(docId: number, docName: string) {
  selectedDocId.value = docId
  selectedDocName.value = docName
  grantForm.value = { userId: null, permission: 'READ' }
  dialogVisible.value = true
}

async function handleGrant() {
  if (!selectedDocId.value || !grantForm.value.userId) {
    ElMessage.warning('请选择用户')
    return
  }
  try {
    await permissionApi.grantDocumentPermission(selectedDocId.value, {
      userId: grantForm.value.userId,
      permission: grantForm.value.permission
    })
    ElMessage.success('授权成功')
    dialogVisible.value = false
    await fetchPermissions()
  } catch {
    ElMessage.error('授权失败')
  }
}

async function handleRevoke(perm: DocPermissionItem) {
  try {
    await ElMessageBox.confirm(
      `确认撤销用户 ${perm.granteeName} 对文档《${perm.documentName}》的权限？`,
      '提示',
      { type: 'warning' }
    )
    await permissionApi.revokeDocumentPermission(perm.documentId, { userId: perm.granteeId })
    ElMessage.success('撤销成功')
    await fetchPermissions()
  } catch {
    // 用户取消或失败
  }
}

const userNameMap = (userId: number) => {
  const user = users.value.find((u) => u.id === userId)
  return user ? user.realName || user.username : `用户${userId}`
}

onMounted(() => {
  fetchPermissions()
  fetchUsers()
})
</script>

<template>
  <div class="doc-permission">
    <!-- 说明卡片 -->
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="info-alert"
    >
      <template #title>
        文档级授权管理 — 为特定用户授予超出其角色默认权限的文档访问许可
      </template>
      <div class="alert-desc">
        角色权限矩阵中，访客(GUEST)仅可访问公开文档，查看者(VIEWER)可访问内部文档。
        通过文档授权，可以让特定用户突破角色限制访问机密文档。
      </div>
    </el-alert>

    <!-- 授权列表 -->
    <div class="doc-card">
      <div class="card-header">
        <span class="card-title">文档授权列表</span>
        <el-tag size="small" type="info">共 {{ permissions.length }} 条授权</el-tag>
      </div>

      <el-table :data="permissions" v-loading="loading" stripe>
        <el-table-column prop="documentName" label="文档名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="被授权人" width="140">
          <template #default="{ row }">
            {{ row.granteeName || userNameMap(row.granteeId) }}
          </template>
        </el-table-column>
        <el-table-column label="权限" width="100">
          <template #default="{ row }">
            <el-tag :type="row.permission === 'WRITE' ? 'warning' : 'success'" size="small">
              {{ row.permission === 'WRITE' ? '读写' : '只读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="授权时间" width="180">
          <template #default="{ row }">
            {{ row.createdAt || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" size="small" @click="handleRevoke(row)">
              撤销
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && permissions.length === 0" description="暂无文档授权记录">
        <el-button type="primary" size="small" @click="openGrantDialog(1, '示例文档')">
          添加授权
        </el-button>
      </el-empty>
    </div>

    <!-- 授权对话框 -->
    <el-dialog v-model="dialogVisible" title="授予文档权限" width="480px">
      <el-form :model="grantForm" label-width="100px">
        <el-form-item label="文档">
          <span class="doc-name">{{ selectedDocName || '请选择文档' }}</span>
        </el-form-item>
        <el-form-item label="授权用户" required>
          <el-select
            v-model="grantForm.userId"
            placeholder="选择用户"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="user in users"
              :key="user.id"
              :label="`${user.realName} (@${user.username}) — ${user.role}`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="权限类型">
          <el-radio-group v-model="grantForm.permission">
            <el-radio-button
              v-for="opt in permissionOptions"
              :key="opt.value"
              :label="opt.value"
            >
              {{ opt.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGrant">确认授权</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.doc-permission {
  animation: rise 0.4s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.info-alert {
  margin-bottom: 16px;
}

.alert-desc {
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 4px;
  line-height: 1.7;
}

.doc-card {
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 18px 10px 14px;
  background: var(--glass-weak);
  backdrop-filter: blur(14px);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px 14px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.doc-name {
  font-size: 13px;
  color: var(--text);
  font-weight: 500;
}
</style>
