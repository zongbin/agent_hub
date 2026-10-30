<script setup lang="ts">
import { onMounted } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import { useProfile } from '@/composables/useProfile'
import { useResponsive } from '@/composables/useResponsive'

const { isMobile } = useResponsive()

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
  handleChangePassword
} = useProfile()

/** 加载当前用户信息 */
onMounted(() => {
  syncProfileFromStore()
})
</script>

<template>
  <div class="profile-page">
    <header class="page-header">
      <h2 class="page-title gradient-text">个人设置</h2>
      <p class="page-desc">管理你的资料信息与登录凭证</p>
    </header>

    <div class="profile-cards">
      <!-- 个人资料卡片 -->
      <section class="profile-card glass-card">
        <div class="card-header">
          <span class="card-icon">
            <el-icon :size="18"><User /></el-icon>
          </span>
          <h3>个人资料</h3>
          <span class="card-line"></span>
        </div>
        <el-form
          ref="profileFormRef"
          :model="profileForm"
          :label-width="isMobile ? '0' : '80px'"
          :label-position="isMobile ? 'top' : 'right'"
          class="profile-form"
        >
          <el-form-item label="用户名">
            <el-input :model-value="profileForm.realName" disabled />
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
      </section>

      <!-- 修改密码卡片 -->
      <section class="profile-card glass-card">
        <div class="card-header">
          <span class="card-icon card-icon--cyan">
            <el-icon :size="18"><Lock /></el-icon>
          </span>
          <h3>修改密码</h3>
          <span class="card-line"></span>
        </div>
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          :label-width="isMobile ? '0' : '80px'"
          :label-position="isMobile ? 'top' : 'right'"
          class="profile-form"
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
      </section>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 760px;
  animation: rise 0.45s cubic-bezier(0.22, 1, 0.36, 1) backwards;
}

.page-header {
  margin-bottom: 22px;
}

.page-title {
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.01em;
  margin: 0 0 6px;
}

.page-desc {
  font-size: 13px;
  color: var(--text-faint);
  margin: 0;
}

.profile-cards {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.profile-card {
  padding: 24px 26px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 11px;
  margin-bottom: 22px;
}

.card-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
  white-space: nowrap;
}

.card-icon {
  width: 34px;
  height: 34px;
  border-radius: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
  background: var(--primary-soft);
  border: 1px solid var(--primary-border);
}

.card-icon--cyan {
  color: var(--accent-text);
  background: var(--tint-cyan-bg);
  border-color: var(--tint-cyan-border);
}

.card-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, var(--border), transparent);
}

.profile-form {
  max-width: 400px;
}

/* ============================================================
   移动端自适应
   ============================================================ */
@media (max-width: 768px) {
  .profile-card {
    padding: 18px 16px;
  }

  .profile-form {
    max-width: 100%;
  }

  .page-title {
    font-size: 18px;
  }
}
</style>
