<script setup lang="ts">
import { onMounted } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import { useProfile } from '@/composables/useProfile'

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
    <div class="page-header">
      <h2>个人设置</h2>
      <p class="page-desc">修改个人资料和密码</p>
    </div>

    <div class="profile-cards">
      <!-- 个人资料卡片 -->
      <div class="profile-card">
        <div class="card-header">
          <el-icon :size="20"><User /></el-icon>
          <h3>个人资料</h3>
        </div>
        <el-form
          ref="profileFormRef"
          :model="profileForm"
          label-width="80px"
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
      </div>

      <!-- 修改密码卡片 -->
      <div class="profile-card">
        <div class="card-header">
          <el-icon :size="20"><Lock /></el-icon>
          <h3>修改密码</h3>
        </div>
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="80px"
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
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 720px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #0F172A;
  margin: 0 0 4px;
}

.page-desc {
  font-size: 13px;
  color: #64748B;
  margin: 0;
}

.profile-cards {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-card {
  background: white;
  border-radius: 10px;
  border: 1px solid #E2E8F0;
  padding: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  color: #0F172A;
}

.card-header h3 {
  font-size: 15px;
  font-weight: 600;
  margin: 0;
}

.profile-form {
  max-width: 400px;
}
</style>
