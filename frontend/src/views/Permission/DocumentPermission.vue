<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { DocPermissionItem, UserItem } from '@/types/permission'
import type { DocumentVO } from '@/api/knowledge'
import { permissionApi } from '@/api/permission'
import { knowledgeApi } from '@/api/knowledge'
import { useResponsive } from '@/composables/useResponsive'

const { isMobile } = useResponsive()

/** 授权列表 */
const permissions = ref<DocPermissionItem[]>([])
const loading = ref(false)

/** 分页 */
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

/** 用户列表（被授权人候选） */
const users = ref<UserItem[]>([])

/** 文档列表（授权目标候选） */
const documents = ref<DocumentVO[]>([])
const documentsLoading = ref(false)

/** 授权对话框 */
const dialogVisible = ref(false)
const granting = ref(false)
const grantForm = reactive({
  documentId: null as number | null,
  userId: null as number | null,
  permission: 'READ'
})

/** 密级标签 */
const securityLabelMap: Record<string, string> = {
  PUBLIC: '公开',
  INTERNAL: '内部',
  CONFIDENTIAL: '机密'
}

const securityTagType: Record<string, 'success' | 'warning' | 'danger'> = {
  PUBLIC: 'success',
  INTERNAL: 'warning',
  CONFIDENTIAL: 'danger'
}

const permissionOptions = [
  { label: '只读', value: 'READ' },
  { label: '读写', value: 'WRITE' }
]

/** 角色中文名 */
const roleLabelMap: Record<string, string> = {
  ADMIN: '管理员',
  EDITOR: '编辑者',
  VIEWER: '查看者',
  GUEST: '访客'
}

/** 选中用户在该文档上是否已有授权（用于提示「将覆盖」） */
const existingGrant = ref<DocPermissionItem | null>(null)

/** 查询指定文档的授权列表，判断该用户是否已被授权 */
const findExistingGrant = async (documentId: number, userId: number): Promise<DocPermissionItem | null> => {
  try {
    const list = await permissionApi.listDocumentPermissions(documentId)
    return list.find((p) => p.granteeId === userId) ?? null
  } catch {
    return null
  }
}

/** 加载授权列表 */
const fetchPermissions = async () => {
  loading.value = true
  try {
    const res = await permissionApi.listDocumentGrants({
      page: pagination.page,
      size: pagination.size
    })
    permissions.value = res.list ?? []
    pagination.total = res.total ?? 0
  } catch (error) {
    ElMessage.error((error as Error).message || '加载文档授权失败')
  } finally {
    loading.value = false
  }
}

/** 加载候选用户 */
const fetchUsers = async () => {
  try {
    users.value = await permissionApi.listUsers()
  } catch (error) {
    ElMessage.error((error as Error).message || '加载用户列表失败')
  }
}

/** 加载候选文档（服务端关键字搜索） */
const searchDocuments = async (keyword: string) => {
  documentsLoading.value = true
  try {
    const res = await knowledgeApi.list({
      page: 1,
      size: 50,
      keyword: keyword.trim() || undefined
    })
    documents.value = res.list ?? []
  } catch (error) {
    ElMessage.error((error as Error).message || '加载文档列表失败')
  } finally {
    documentsLoading.value = false
  }
}

/** 打开授权对话框 */
const openGrantDialog = () => {
  grantForm.documentId = null
  grantForm.userId = null
  grantForm.permission = 'READ'
  dialogVisible.value = true
  searchDocuments('')
}

/** 提交授权 */
const handleGrant = async () => {
  if (!grantForm.documentId) {
    ElMessage.warning('请选择文档')
    return
  }
  if (!grantForm.userId) {
    ElMessage.warning('请选择被授权用户')
    return
  }
  granting.value = true
  try {
    await permissionApi.grantDocumentPermission(grantForm.documentId, {
      userId: grantForm.userId,
      permission: grantForm.permission
    })
    ElMessage.success('授权成功')
    dialogVisible.value = false
    await fetchPermissions()
  } catch (error) {
    ElMessage.error((error as Error).message || '授权失败')
  } finally {
    granting.value = false
  }
}

/** 撤销授权 */
const handleRevoke = async (perm: DocPermissionItem) => {
  const userLabel = perm.granteeName || perm.granteeUsername || `用户${perm.granteeId}`
  try {
    await ElMessageBox.confirm(
      `确认撤销「${userLabel}」对文档《${perm.documentTitle}》的授权？`,
      '撤销确认',
      { type: 'warning', confirmButtonText: '撤销', cancelButtonText: '取消' }
    )
    await permissionApi.revokeDocumentPermission(perm.documentId, { userId: perm.granteeId })
    ElMessage.success('已撤销授权')
    await fetchPermissions()
  } catch (error) {
    // 用户取消时忽略
  }
}

/** 分页变化 */
const handlePageChange = (page: number) => {
  pagination.page = page
  fetchPermissions()
}

/** 格式化时间 */
const formatTime = (time: string): string => {
  if (!time) return '—'
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  fetchPermissions()
}

/** 授权目标变化时，若已存在授权则回填权限类型 */
watch(
  () => [grantForm.documentId, grantForm.userId] as const,
  async ([documentId, userId]) => {
    existingGrant.value = null
    if (!documentId || !userId) {
      return
    }
    const grant = await findExistingGrant(documentId, userId)
    existingGrant.value = grant
    if (grant) {
      grantForm.permission = grant.permission
    }
  }
)

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
        <br />
        <b>只读(READ)</b>：仅可查看详情与在问答中检索该文档内容；
        <b>读写(WRITE)</b>：额外允许删除等写操作。对同一用户重复授权会覆盖原有权限。
      </div>
    </el-alert>

    <!-- 授权列表 -->
    <div class="doc-card">
      <div class="card-header">
        <span class="card-title">
          文档授权列表
          <span class="card-count">共 {{ pagination.total }} 条</span>
        </span>
        <div class="card-actions">
          <el-button size="small" @click="fetchPermissions">刷新</el-button>
          <el-button type="primary" size="small" @click="openGrantDialog">
            新增授权
          </el-button>
        </div>
      </div>

      <el-table
        :data="permissions"
        v-loading="loading"
        stripe
        :size="isMobile ? 'small' : 'default'"
      >
        <el-table-column
          prop="documentTitle"
          label="文档名称"
          :min-width="isMobile ? 130 : 200"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <div class="doc-cell">
              <span class="doc-title">{{ row.documentTitle }}</span>
              <el-tag
                v-if="row.documentSecurityLevel"
                :type="securityTagType[row.documentSecurityLevel] || 'info'"
                size="small"
                effect="plain"
              >
                {{ securityLabelMap[row.documentSecurityLevel] || row.documentSecurityLevel }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="被授权人" :min-width="isMobile ? 110 : 180">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="user-name">{{ row.granteeName || row.granteeUsername || `用户${row.granteeId}` }}</span>
              <span class="user-sub">
                @{{ row.granteeUsername || row.granteeId }}
                · {{ roleLabelMap[row.granteeRole] || row.granteeRole || '—' }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="权限" :width="isMobile ? 78 : 100">
          <template #default="{ row }">
            <el-tag :type="row.permission === 'WRITE' ? 'warning' : 'success'" size="small">
              {{ row.permissionLabel || (row.permission === 'WRITE' ? '读写' : '只读') }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column v-if="!isMobile" label="授权时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" :width="isMobile ? 76 : 100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" size="small" @click="handleRevoke(row)">
              撤销
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无文档授权记录" :image-size="80">
            <el-button type="primary" size="small" @click="openGrantDialog">新增授权</el-button>
          </el-empty>
        </template>
      </el-table>

      <div v-if="pagination.total > 0" class="pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="pagination.total"
          :current-page="pagination.page"
          :page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 授权对话框 -->
    <!-- append-to-body：弹窗位于 el-tabs 内，父级 backdrop-filter 会破坏 fixed 定位并被 overflow 裁剪 -->
    <el-dialog
      v-model="dialogVisible"
      title="授予文档权限"
      :width="isMobile ? '92%' : '520px'"
      append-to-body
    >
      <el-form
        :model="grantForm"
        :label-width="isMobile ? '0' : '100px'"
        :label-position="isMobile ? 'top' : 'right'"
      >
        <el-form-item label="文档" required>
          <el-select
            v-model="grantForm.documentId"
            placeholder="输入关键字搜索要授权的文档"
            filterable
            remote
            reserve-keyword
            :remote-method="searchDocuments"
            :loading="documentsLoading"
            style="width: 100%"
          >
            <el-option
              v-for="doc in documents"
              :key="doc.id"
              :label="doc.title"
              :value="doc.id"
            >
              <span class="opt-title">{{ doc.title }}</span>
              <span class="opt-tag">
                {{ securityLabelMap[doc.securityLevel] || doc.securityLevel }}
              </span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="授权用户" required>
          <el-select
            v-model="grantForm.userId"
            placeholder="选择被授权用户"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="user in users"
              :key="user.id"
              :label="`${user.realName || user.username} (@${user.username})`"
              :value="user.id"
            >
              <span class="opt-title">{{ user.realName || user.username }}</span>
              <span class="opt-tag">{{ roleLabelMap[user.role] || user.role }}</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="权限类型">
          <el-radio-group v-model="grantForm.permission">
            <el-radio-button
              v-for="opt in permissionOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </el-radio-button>
          </el-radio-group>
          <div v-if="existingGrant" class="form-tip">
            该用户已有此文档的「{{ existingGrant.permissionLabel }}」授权，提交后将覆盖。
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="granting" @click="handleGrant">确认授权</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.doc-permission {
  animation: rise 0.4s cubic-bezier(0.22, 1, 0.36, 1) backwards;
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
  gap: 12px;
  padding: 0 8px 14px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.card-count {
  margin-left: 8px;
  font-size: 12px;
  font-weight: 400;
  color: var(--text-faint);
}

.card-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

/* 文档单元格 */
.doc-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.doc-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 用户单元格 */
.user-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
}

.user-name {
  font-size: 13px;
  color: var(--text);
}

.user-sub {
  font-size: 11px;
  color: var(--text-faint);
}

/* 分页 */
.pager {
  display: flex;
  justify-content: flex-end;
  padding: 14px 8px 0;
}

.doc-name {
  font-size: 13px;
  color: var(--text);
  font-weight: 500;
}

.form-tip {
  font-size: 12px;
  color: var(--text-faint);
  line-height: 1.6;
  margin-top: 4px;
}

/* 下拉选项 */
.opt-title {
  margin-right: 10px;
}

.opt-tag {
  float: right;
  color: var(--text-faint);
  font-size: 12px;
}

/* ============================================================
   移动端自适应
   ============================================================ */
@media (max-width: 768px) {
  .doc-card {
    padding: 14px 6px 12px;
  }

  .card-header {
    padding: 0 6px 12px;
  }

  .alert-desc {
    font-size: 11.5px;
  }

  .pager {
    justify-content: center;
    padding: 12px 6px 0;
  }
}
</style>
