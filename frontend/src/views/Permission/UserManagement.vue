<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import type { UserItem, RoleInfo, CreateUserRequest } from '@/types/permission'
import { permissionApi } from '@/api/permission'
import { useResponsive } from '@/composables/useResponsive'

const { isMobile } = useResponsive()

const users = ref<UserItem[]>([])
const roles = ref<RoleInfo[]>([])
const loading = ref(false)
const searchKey = ref<string>('')
const dialogVisible = ref(false)
const editingUser = ref<UserItem | null>(null)
const selectedRole = ref('')

/** 新建用户对话框 */
const createDialogVisible = ref(false)
const createForm = ref<CreateUserRequest>({
  username: '',
  password: '',
  realName: '',
  email: '',
  role: 'VIEWER',
  department: ''
})
const creating = ref(false)
const createFormRef = ref()

/** 角色中文映射 */
const roleLabelMap: Record<string, string> = {
  ADMIN: '管理员',
  EDITOR: '编辑者',
  VIEWER: '查看者',
  GUEST: '访客'
}

/** 角色标签类型 */
const roleTagType: Record<string, string> = {
  ADMIN: 'danger',
  EDITOR: 'warning',
  VIEWER: 'success',
  GUEST: 'info'
}

async function fetchUsers() {
  loading.value = true
  try {
    users.value = await permissionApi.listUsers()
  } catch {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchRoles() {
  try {
    roles.value = await permissionApi.listRoles()
  } catch {
    ElMessage.error('获取角色列表失败')
  }
}

function openEditDialog(user: UserItem) {
  editingUser.value = user
  selectedRole.value = user.role
  dialogVisible.value = true
}

async function handleRoleChange() {
  if (!editingUser.value) return
  try {
    await permissionApi.updateUserRole(editingUser.value.id, { role: selectedRole.value })
    ElMessage.success('角色修改成功')
    dialogVisible.value = false
    await fetchUsers()
  } catch {
    ElMessage.error('角色修改失败')
  }
}

function openCreateDialog() {
  createForm.value = {
    username: '',
    password: '',
    realName: '',
    email: '',
    role: 'VIEWER',
    department: ''
  }
  createDialogVisible.value = true
}

async function handleCreateUser() {
  const formRef = createFormRef.value as FormInstance
  if (formRef) {
    const valid = await formRef.validate().catch(() => false)
    if (!valid) return
  }
  creating.value = true
  try {
    await permissionApi.createUser(createForm.value)
    ElMessage.success('用户创建成功')
    createDialogVisible.value = false
    await fetchUsers()
  } catch (e: any) {
    ElMessage.error(e?.message || '用户创建失败')
  } finally {
    creating.value = false
  }
}

async function handleToggleStatus(user: UserItem) {
  const action = user.status === 1 ? '禁用' : '启用'
  const newStatus = user.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(`确认${action}用户 ${user.username}？`, '提示', {
      type: 'warning'
    })
    await permissionApi.updateUserStatus(user.id, { status: newStatus })
    ElMessage.success(`${action}成功`)
    await fetchUsers()
  } catch (e: any) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.message || `${action}失败`)
    }
  }
}

const filteredUsers = () => {
  if (!searchKey.value) return users.value
  const key = searchKey.value.toLowerCase()
  return users.value.filter(
    (u) =>
      u.username.toLowerCase().includes(key) ||
      u.realName?.toLowerCase().includes(key) ||
      u.department?.toLowerCase().includes(key)
  )
}

onMounted(() => {
  fetchUsers()
  fetchRoles()
})
</script>

<template>
  <div class="user-management">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-input
        v-model="searchKey"
        placeholder="搜索用户名、姓名、部门..."
        class="search-input"
        clearable
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-button type="primary" @click="openCreateDialog">
        <el-icon class="mr-1"><Plus /></el-icon>
        新增用户
      </el-button>

      <div class="role-legend">
        <span class="legend-label">角色说明</span>
        <el-tag
          v-for="role in roles"
          :key="role.code"
          :type="roleTagType[role.code] as any"
          size="small"
          class="legend-tag"
        >
          {{ role.name }} — {{ role.description }}
        </el-tag>
      </div>
    </div>

    <!-- 用户表格 -->
    <div class="table-card">
      <el-table
        :data="filteredUsers()"
        v-loading="loading"
        stripe
        :size="isMobile ? 'small' : 'default'"
      >
        <el-table-column prop="username" label="用户名" :min-width="isMobile ? 90 : 120" />
        <el-table-column prop="realName" label="姓名" :min-width="isMobile ? 70 : 100" />
        <el-table-column v-if="!isMobile" prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column v-if="!isMobile" prop="department" label="部门" min-width="120">
          <template #default="{ row }">
            {{ row.department || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="角色" :width="isMobile ? 84 : 120">
          <template #default="{ row }">
            <el-tag :type="roleTagType[row.role] as any" size="small">
              {{ roleLabelMap[row.role] || row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" :width="isMobile ? 76 : 90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="isMobile ? 130 : 200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditDialog(row)">
              修改角色
            </el-button>
            <el-button
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && filteredUsers().length === 0" description="暂无用户数据" />
    </div>

    <!-- 新增用户对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="新增用户"
      :width="isMobile ? '92%' : '480px'"
      :close-on-click-modal="false"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :label-width="isMobile ? '0' : '90px'"
        :label-position="isMobile ? 'top' : 'right'"
        class="create-form"
      >
        <el-form-item label="用户名" required>
          <el-input v-model="createForm.username" placeholder="3-32 位字符" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input
            v-model="createForm.password"
            type="password"
            placeholder="至少 6 位"
            show-password
          />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="createForm.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" placeholder="选填" />
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="createForm.department" placeholder="选填" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="createForm.role" placeholder="选择角色" style="width: 100%">
            <el-option
              v-for="role in roles"
              :key="role.code"
              :label="role.name"
              :value="role.code"
            >
              <el-tag :type="roleTagType[role.code] as any" size="small">{{ role.name }}</el-tag>
              <span class="role-desc">{{ role.description }}</span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateUser">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 修改角色对话框 -->
    <el-dialog v-model="dialogVisible" title="修改用户角色" :width="isMobile ? '92%' : '420px'">
      <div v-if="editingUser" class="edit-form">
        <div class="edit-row">
          <span class="edit-label">用户名：</span>
          <span class="edit-value">{{ editingUser.username }}</span>
        </div>
        <div class="edit-row">
          <span class="edit-label">姓名：</span>
          <span class="edit-value">{{ editingUser.realName }}</span>
        </div>
        <div class="edit-row">
          <span class="edit-label">当前角色：</span>
          <el-tag :type="roleTagType[editingUser.role] as any" size="small">
            {{ roleLabelMap[editingUser.role] || editingUser.role }}
          </el-tag>
        </div>
        <div class="edit-row">
          <span class="edit-label">新角色：</span>
          <el-select
            v-model="selectedRole"
            placeholder="选择新角色"
            :style="{ width: isMobile ? '100%' : '200px' }"
          >
            <el-option
              v-for="role in roles"
              :key="role.code"
              :label="role.name"
              :value="role.code"
            >
              <el-tag :type="roleTagType[role.code] as any" size="small">{{ role.name }}</el-tag>
              <span class="role-desc">{{ role.description }}</span>
            </el-option>
          </el-select>
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRoleChange">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-management {
  animation: rise 0.4s cubic-bezier(0.22, 1, 0.36, 1) both;
}

/* ===== 工具栏 ===== */
.toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.search-input {
  width: 280px;
  flex-shrink: 0;
}

.role-legend {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.legend-label {
  font-size: 12px;
  color: var(--text-faint);
  white-space: nowrap;
}

.legend-tag {
  font-size: 11px;
}

/* ===== 表格 ===== */
.table-card {
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 6px 10px 14px;
  background: var(--glass-weak);
  backdrop-filter: blur(14px);
}

/* ===== 表单 ===== */
.create-form {
  padding: 4px 0;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.edit-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.edit-label {
  font-size: 13px;
  color: var(--text-dim);
  width: 80px;
  flex-shrink: 0;
}

.edit-value {
  font-size: 13px;
  color: var(--text);
}

.role-desc {
  font-size: 12px;
  color: var(--text-faint);
  margin-left: 8px;
}

/* ============================================================
   移动端自适应
   ============================================================ */
@media (max-width: 768px) {
  .toolbar {
    gap: 10px;
  }

  .search-input {
    width: 100%;
  }

  .table-card {
    padding: 4px 6px 10px;
  }

  .edit-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .edit-label {
    width: auto;
  }
}
</style>
