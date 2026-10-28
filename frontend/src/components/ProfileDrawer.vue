<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { User, Lock, Close } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useProfile } from '@/composables/useProfile'

const authStore = useAuthStore()

const {
  profileFormRef,
  profileLoading,
  profileForm,
  syncProfileFromStore,
  handleSaveProfile,
  passwordFormRef,
  passwordLoading,
  passwordForm,
  passwordRules,
  handleChangePassword,
  setPasswordSuccessHandler
} = useProfile()

/** 抽屉显示状态 */
const visible = defineModel<boolean>({ default: false })

/** 当前激活的 tab */
const activeTab = ref('profile')

// 同步密码成功处理：抽屉中修改密码后关闭抽屉并提示重新登录
setPasswordSuccessHandler(async () => {
  visible.value = false
  await ElMessageBox.confirm('密码已修改，请使用新密码重新登录。', '提示', {
    confirmButtonText: '重新登录',
    showCancelButton: false,
    closeOnClickModal: false,
    closeOnPressEscape: false,
    type: 'success'
  }).catch(() => {})
  await authStore.logout()
  window.location.href = '/login'
})

/** 打开抽屉时同步用户信息 */
watch(visible, (val) => {
  if (val) {
    syncProfileFromStore()
    activeTab.value = 'profile'
  }
})

/** 关闭抽屉 */
function handleClose() {
  visible.value = false
}

/** 角色中文映射 */
const roleLabelMap: Record<string, string> = {
  ADMIN: '管理员',
  EDITOR: '编辑者',
  VIEWER: '查看者',
  GUEST: '访客'
}

function getRoleLabel(role: string | undefined) {
  if (!role) return ''
  return roleLabelMap[role] || role
}
</script>

<template>
  <el-drawer
    v-model="visible"
    :size="420"
    direction="rtl"
    :with-header="false"
    :before-close="handleClose"
    class="profile-drawer"
  >
    <div class="drawer-container">
      <!-- 抽屉顶部用户信息 -->
      <div class="drawer-header">
        <div class="user-summary">
          <el-avatar :size="48" class="summary-avatar">
            {{ authStore.user?.realName?.[0] || authStore.user?.username?.[0] || 'U' }}
          </el-avatar>
          <div class="summary-info">
            <div class="summary-name">{{ authStore.user?.realName || authStore.user?.username }}</div>
            <div class="summary-meta">
              <el-tag type="info" size="small">{{ getRoleLabel(authStore.user?.role) }}</el-tag>
              <span v-if="authStore.user?.department" class="summary-dept">
                {{ authStore.user.department }}
              </span>
            </div>
          </div>
        </div>
        <el-button :icon="Close" circle text class="close-btn" @click="handleClose" />
      </div>

      <!-- Tab 切换 -->
      <el-tabs v-model="activeTab" class="drawer-tabs">
        <!-- 个人资料 -->
        <el-tab-pane label="个人资料" name="profile">
          <template #label>
            <span class="tab-label">
              <el-icon><User /></el-icon>
              个人资料
            </span>
          </template>
          <el-form
            ref="profileFormRef"
            :model="profileForm"
            label-width="72px"
            class="drawer-form"
          >
            <el-form-item label="用户名">
              <el-input :model-value="authStore.user?.username" disabled />
            </el-form-item>
            <el-form-item label="角色">
              <el-tag type="info" size="small">{{ getRoleLabel(authStore.user?.role) }}</el-tag>
            </el-form-item>
            <el-form-item label="姓名">
              <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="部门">
              <el-input v-model="profileForm.department" placeholder="请输入部门" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="profileLoading" @click="handleSaveProfile">
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 修改密码 -->
        <el-tab-pane label="修改密码" name="password">
          <template #label>
            <span class="tab-label">
              <el-icon><Lock /></el-icon>
              修改密码
            </span>
          </template>
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="72px"
            class="drawer-form"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                placeholder="请输入原密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码（6-64 位）"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-drawer>
</template>

<style scoped>
.profile-drawer :deep(.el-drawer__body) {
  padding: 0;
}

.drawer-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* ===== 顶部用户摘要 ===== */
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #E2E8F0;
  flex-shrink: 0;
}

.user-summary {
  display: flex;
  align-items: center;
  gap: 12px;
}

.summary-avatar {
  background: linear-gradient(135deg, #818CF8, #06B6D4);
  font-size: 18px;
  font-weight: 600;
  color: white;
}

.summary-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-name {
  font-size: 15px;
  font-weight: 600;
  color: #0F172A;
}

.summary-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-dept {
  font-size: 12px;
  color: #64748B;
}

.close-btn {
  color: #94A3B8;
}

.close-btn:hover {
  color: #0F172A;
}

/* ===== Tab 区域 ===== */
.drawer-tabs {
  flex: 1;
  overflow-y: auto;
  padding: 0 24px;
}

.drawer-tabs :deep(.el-tabs__nav-wrap) {
  padding-top: 12px;
}

.drawer-tabs :deep(.el-tabs__item) {
  font-size: 13px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.drawer-form {
  padding-top: 20px;
  max-width: 340px;
}

.drawer-form :deep(.el-form-item__label) {
  white-space: nowrap;
}
</style>
