<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { AuditLogItem } from '@/types/permission'
import { permissionApi } from '@/api/permission'

const logs = ref<AuditLogItem[]>([])
const loading = ref(false)
const total = ref(0)

const query = reactive({
  userId: undefined as number | undefined,
  dateRange: [] as string[],
  actionType: ''
})

const pagination = reactive({
  page: 1,
  size: 20
})

const actionTypeOptions = [
  { label: '全部', value: '' },
  { label: '登录', value: 'LOGIN' },
  { label: '登出', value: 'LOGOUT' },
  { label: '查看文档', value: 'VIEW' },
  { label: '编辑文档', value: 'EDIT' },
  { label: '删除文档', value: 'DELETE' },
  { label: '创建用户', value: 'CREATE_USER' },
  { label: '更新用户', value: 'UPDATE_USER' },
  { label: '修改角色', value: 'UPDATE_ROLE' },
  { label: '启用/禁用', value: 'TOGGLE_STATUS' },
  { label: '重置密码', value: 'RESET_PASSWORD' },
  { label: '授权', value: 'GRANT' },
  { label: '撤销', value: 'REVOKE' },
  { label: '权限拒绝', value: 'DENIED' }
]

const actionLabelMap: Record<string, string> = {
  LOGIN: '登录',
  LOGOUT: '登出',
  VIEW: '查看文档',
  VIEW_PUBLIC: '查看公开文档',
  VIEW_INTERNAL: '查看内部文档',
  VIEW_CONFIDENTIAL: '查看机密文档',
  EDIT: '编辑文档',
  DELETE: '删除文档',
  UPDATE_USER: '更新用户',
  UPDATE_ROLE: '修改角色',
  TOGGLE_STATUS: '启用/禁用用户',
  CREATE_USER: '创建用户',
  DELETE_USER: '删除用户',
  RESET_PASSWORD: '重置密码',
  GRANT: '授予权限',
  REVOKE: '撤销权限',
  DENIED: '权限拒绝'
}

const actionTagType: Record<string, string> = {
  LOGIN: 'success',
  LOGOUT: 'info',
  VIEW: 'primary',
  VIEW_PUBLIC: 'success',
  VIEW_INTERNAL: 'primary',
  VIEW_CONFIDENTIAL: 'warning',
  EDIT: 'warning',
  DELETE: 'danger',
  UPDATE_USER: 'warning',
  UPDATE_ROLE: 'warning',
  TOGGLE_STATUS: 'warning',
  CREATE_USER: 'success',
  DELETE_USER: 'danger',
  RESET_PASSWORD: 'warning',
  GRANT: 'success',
  REVOKE: 'danger',
  DENIED: 'danger'
}

async function fetchLogs() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.page,
      size: pagination.size
    }
    if (query.userId) params.userId = query.userId
    if (query.dateRange && query.dateRange.length === 2) {
      params.startTime = query.dateRange[0]
      params.endTime = query.dateRange[1]
    }
    const result = await permissionApi.queryAuditLogs(params)
    logs.value = result.list
    total.value = result.total
  } catch {
    // 演示数据
    logs.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchLogs()
}

function handleReset() {
  query.userId = undefined
  query.dateRange = []
  query.actionType = ''
  pagination.page = 1
  fetchLogs()
}

function handlePageChange(page: number) {
  pagination.page = page
  fetchLogs()
}

function handleSizeChange(size: number) {
  pagination.size = size
  pagination.page = 1
  fetchLogs()
}

function formatTime(time: string) {
  if (!time) return '—'
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

/** 资源类型中文映射 */
const resourceLabelMap: Record<string, string> = {
  USER: '用户',
  DOCUMENT: '文档',
  SYSTEM: '系统'
}

function getActionLabel(action: string) {
  return actionLabelMap[action] || action
}

function getActionTagType(action: string) {
  return actionTagType[action] || 'info'
}

function getResourceLabel(resource: string) {
  if (!resource) return '—'
  // 资源格式可能为 "USER" 或 "DOCUMENT:docId" 等，取前缀匹配
  const prefix = resource.split(':')[0]
  return resourceLabelMap[prefix] || resource
}

onMounted(() => {
  fetchLogs()
})
</script>

<template>
  <div class="audit-log">
    <!-- 查询工具栏 -->
    <div class="query-card">
      <el-form :inline="true" class="query-form">
        <el-form-item label="操作人">
          <el-input
            v-model.number="query.userId"
            placeholder="用户 ID"
            clearable
            style="width: 140px"
          />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="query.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="query.actionType" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="opt in actionTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 审计日志表格 -->
    <div class="table-card">
      <el-table :data="logs" v-loading="loading" stripe>
        <el-table-column label="操作人" width="130">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="24" class="user-avatar">
                {{ row.username?.[0] || '?' }}
              </el-avatar>
              <span>{{ row.username || '系统' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-tag :type="getActionTagType(row.action) as any" size="small">
              {{ getActionLabel(row.action) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="资源" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ getResourceLabel(row.resource) }}
          </template>
        </el-table-column>
        <el-table-column label="IP 地址" width="130">
          <template #default="{ row }">
            <span class="ip-cell">{{ row.ipAddress || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">
              {{ row.success === 1 ? '成功' : '拒绝' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && logs.length === 0" description="暂无审计记录" />

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.audit-log {
  display: flex;
  flex-direction: column;
  gap: 14px;
  animation: rise 0.4s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.query-card,
.table-card {
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--glass-weak);
  backdrop-filter: blur(14px);
}

.query-card {
  padding: 16px 18px 2px;
}

.table-card {
  padding: 6px 10px 16px;
}

.query-form {
  margin: 0;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  background: linear-gradient(135deg, #7C5CFF, #22D3EE);
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  flex-shrink: 0;
}

.ip-cell {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  color: var(--text-dim);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
