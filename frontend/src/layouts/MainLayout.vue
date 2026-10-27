<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  HomeFilled,
  Reading,
  ChatLineRound,
  Cpu,
  SwitchButton,
  UserFilled,
  ArrowDown
} from '@element-plus/icons-vue'
import ProfileDrawer from '@/components/ProfileDrawer.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

/** 当前用户是否为管理员 */
const isAdmin = computed(() => authStore.user?.role === 'ADMIN')

/** 个人设置抽屉 */
const profileDrawerVisible = ref(false)

/** 打开个人设置抽屉 */
function openProfileDrawer() {
  profileDrawerVisible.value = true
}

/** 导航菜单 */
const menuItems = [
  { path: '/dashboard', title: '工作台', icon: HomeFilled },
  { path: '/knowledge', title: '知识库', icon: Reading },
  { path: '/qa', title: '智能问答', icon: ChatLineRound },
  { path: '/agent', title: 'SuperAgent', icon: Cpu },
  { path: '/permission', title: '权限管理', icon: UserFilled, adminOnly: true }
]

/** 当前页标题 */
const pageTitle = computed(() => (route.meta.title as string) || '智知 AgentHub')

/** 登出 */
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确认退出登录？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await authStore.logout()
    ElMessage.success('已退出登录')
    router.replace('/login')
  } catch {
    // 用户取消
  }
}
</script>

<template>
  <div class="app-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <!-- 品牌 -->
      <div class="sidebar-brand">
        <div class="brand-logo">
          <span>智</span>
        </div>
        <div class="brand-text">
          <h1>智知 AgentHub</h1>
          <span>企业知识管理</span>
        </div>
      </div>

      <!-- 导航 -->
      <nav class="sidebar-nav">
        <div class="nav-section">功能</div>
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: route.path === item.path }"
          v-show="!item.adminOnly || isAdmin"
        >
          <el-icon :size="18">
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.title }}</span>
        </router-link>
      </nav>

      <!-- 用户信息 -->
      <div class="sidebar-footer">
        <div class="user-card">
          <el-avatar :size="32" class="user-avatar">
            {{ authStore.user?.realName?.[0] || authStore.user?.username?.[0] || '管' }}
          </el-avatar>
          <div class="user-info">
            <div class="user-name">{{ authStore.user?.realName || authStore.user?.username }}</div>
            <div class="user-role">{{ authStore.user?.role }}</div>
          </div>
          <el-icon class="logout-icon" @click="handleLogout" title="退出登录">
            <SwitchButton />
          </el-icon>
        </div>
      </div>
    </aside>

    <!-- 主内容 -->
    <main class="main-content">
      <!-- 头部 -->
      <header class="header">
        <h2 class="page-title">{{ pageTitle }}</h2>
        <div class="header-right">
          <div class="user-trigger" @click="openProfileDrawer">
            <el-avatar :size="32" class="trigger-avatar">
              {{ authStore.user?.realName?.[0] || authStore.user?.username?.[0] || 'U' }}
            </el-avatar>
            <span class="trigger-name">
              {{ authStore.user?.realName || authStore.user?.username }}
            </span>
            <el-icon :size="12" class="trigger-arrow"><ArrowDown /></el-icon>
          </div>
        </div>
      </header>

      <!-- 内容区 -->
      <div class="content-area">
        <router-view />
      </div>
    </main>

    <!-- 个人设置抽屉 -->
    <ProfileDrawer v-model="profileDrawerVisible" />
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
}

/* ===== 侧边栏 ===== */
.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #1E1B4B 0%, #312E81 100%);
  color: white;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-brand {
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.brand-logo {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #818CF8, #06B6D4);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
}

.brand-text h1 {
  font-size: 15px;
  font-weight: 700;
  margin: 0;
}

.brand-text span {
  font-size: 11px;
  opacity: 0.5;
}

/* ===== 导航 ===== */
.sidebar-nav {
  flex: 1;
  padding: 12px 10px;
  overflow-y: auto;
}

.nav-section {
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
  opacity: 0.35;
  padding: 0 10px;
  margin: 12px 0 6px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  font-size: 13px;
  opacity: 0.65;
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
  color: white;
  margin-bottom: 2px;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.08);
  opacity: 1;
}

.nav-item.active {
  background: rgba(255, 255, 255, 0.15);
  opacity: 1;
  font-weight: 500;
}

/* ===== 底部用户 ===== */
.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.user-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.user-card:hover {
  background: rgba(255, 255, 255, 0.08);
}

.user-avatar {
  background: linear-gradient(135deg, #818CF8, #06B6D4);
  font-size: 12px;
  font-weight: 600;
  color: white;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 10px;
  opacity: 0.45;
}

.logout-icon {
  opacity: 0.4;
  transition: opacity 0.2s;
  cursor: pointer;
}

.logout-icon:hover {
  opacity: 1;
}

/* ===== 主内容 ===== */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  height: 56px;
  background: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
  display: flex;
  align-items: center;
  padding: 0 24px;
  flex-shrink: 0;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: #0F172A;
}

.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.user-trigger:hover {
  background: #F1F5F9;
}

.trigger-avatar {
  background: linear-gradient(135deg, #818CF8, #06B6D4);
  font-size: 13px;
  font-weight: 600;
  color: white;
}

.trigger-name {
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  max-width: 120px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.trigger-arrow {
  color: #94A3B8;
  transition: transform 0.2s;
}

.user-trigger:hover .trigger-arrow {
  transform: rotate(180deg);
}

.content-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
</style>
