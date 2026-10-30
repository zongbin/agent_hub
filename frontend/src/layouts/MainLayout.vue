<script setup lang="ts">
import { computed, ref, watch } from 'vue'
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
  ArrowDown,
  Fold
} from '@element-plus/icons-vue'
import ProfileDrawer from '@/components/ProfileDrawer.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
import { useResponsive } from '@/composables/useResponsive'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const { isMobile } = useResponsive()

/** 当前用户是否为管理员 */
const isAdmin = computed(() => authStore.user?.role === 'ADMIN')

/** 移动端侧边栏抽屉是否展开 */
const mobileMenuVisible = ref(false)

/** 路由变化时收起移动端菜单 */
watch(
  () => route.path,
  () => {
    mobileMenuVisible.value = false
  }
)

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

/** 个人设置抽屉 */
const profileDrawerVisible = ref(false)

/** 打开个人设置抽屉 */
function openProfileDrawer() {
  profileDrawerVisible.value = true
}

/** 导航菜单 */
const menuItems = [
  { path: '/dashboard', title: '工作台', icon: HomeFilled, desc: '知识运营总览' },
  { path: '/knowledge', title: '知识库', icon: Reading, desc: '文档与向量索引' },
  { path: '/qa', title: '智能问答', icon: ChatLineRound, desc: 'RAG 检索增强' },
  { path: '/agent', title: '超级智能体', icon: Cpu, desc: '自主拆解复杂任务' },
  { path: '/permission', title: '权限管理', icon: UserFilled, desc: '角色与审计', adminOnly: true }
]

/** 当前页标题 */
const pageTitle = computed(() => (route.meta.title as string) || '智知 AgentHub')

/** 当前页副标题 */
const pageDesc = computed(() => {
  const item = menuItems.find((i) => route.path.startsWith(i.path))
  return item?.desc || '企业级智能知识中枢'
})

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
  <div class="app-layout ai-canvas">
    <!-- 移动端菜单遮罩 -->
    <transition name="mask-fade">
      <div
        v-if="isMobile && mobileMenuVisible"
        class="mobile-mask"
        @click="mobileMenuVisible = false"
      ></div>
    </transition>

    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ 'sidebar--open': mobileMenuVisible }">
      <!-- 极光光晕 -->
      <div class="sidebar-aurora"></div>

      <!-- 品牌 -->
      <div class="sidebar-brand">
        <div class="brand-logo">
          <span class="brand-ring"></span>
          <span class="brand-glyph">智</span>
        </div>
        <div class="brand-text">
          <h1>智知 AgentHub</h1>
          <span class="brand-sub">Enterprise AI Hub</span>
        </div>
      </div>

      <!-- 导航 -->
      <nav class="sidebar-nav">
        <div class="nav-section">功能模块</div>
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: route.path.startsWith(item.path) }"
          v-show="!item.adminOnly || isAdmin"
          @click="mobileMenuVisible = false"
        >
          <span class="nav-indicator"></span>
          <el-icon :size="17">
            <component :is="item.icon" />
          </el-icon>
          <span class="nav-body">
            <span class="nav-title">{{ item.title }}</span>
            <span class="nav-desc">{{ item.desc }}</span>
          </span>
        </router-link>

        <div class="nav-section nav-section--second">系统</div>
        <div class="nav-item nav-item--muted" @click="openProfileDrawer(); mobileMenuVisible = false">
          <span class="nav-indicator"></span>
          <el-icon :size="17"><UserFilled /></el-icon>
          <span class="nav-body">
            <span class="nav-title">个人设置</span>
            <span class="nav-desc">资料与密码</span>
          </span>
        </div>
      </nav>

      <!-- 用户信息 -->
      <div class="sidebar-footer">
        <div class="user-card" @click="openProfileDrawer">
          <el-avatar :size="34" class="user-avatar">
            {{ authStore.user?.realName?.[0] || authStore.user?.username?.[0] || '管' }}
          </el-avatar>
          <div class="user-info">
            <div class="user-name">{{ authStore.user?.realName || authStore.user?.username }}</div>
            <div class="user-role">{{ getRoleLabel(authStore.user?.role) }}</div>
          </div>
          <el-icon class="logout-icon" @click.stop="handleLogout" title="退出登录">
            <SwitchButton />
          </el-icon>
        </div>
      </div>
    </aside>

    <!-- 主内容 -->
    <main class="main-content">
      <!-- 头部 -->
      <header class="header">
        <div class="header-left">
          <button
            class="menu-toggle"
            type="button"
            aria-label="打开导航菜单"
            @click="mobileMenuVisible = !mobileMenuVisible"
          >
            <el-icon :size="19"><Fold /></el-icon>
          </button>
          <h2 class="page-title gradient-text">{{ pageTitle }}</h2>
          <span class="page-desc">{{ pageDesc }}</span>
        </div>
        <div class="header-right">
          <ThemeToggle />
          <span class="ai-badge">
            <span class="ai-dot"></span>
            AI 内核在线
          </span>
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
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
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
  height: 100dvh;
}

/* ===== 侧边栏 ===== */
.sidebar {
  position: relative;
  width: var(--sidebar-width);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--sidebar-veil);
  border-right: 1px solid var(--border);
  backdrop-filter: blur(18px);
  overflow: hidden;
}

/* 极光光晕 */
.sidebar-aurora {
  position: absolute;
  top: -120px;
  left: -60px;
  width: 340px;
  height: 340px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(124, 92, 255, 0.4) 0%, rgba(34, 211, 238, 0.12) 45%, transparent 70%);
  filter: blur(20px);
  animation: aurora 14s ease-in-out infinite;
  pointer-events: none;
}

/* ===== 品牌 ===== */
.sidebar-brand {
  position: relative;
  padding: 18px 18px 16px;
  display: flex;
  align-items: center;
  gap: 11px;
  border-bottom: 1px solid var(--border);
}

.brand-logo {
  position: relative;
  width: 36px;
  height: 36px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #7C5CFF 0%, #22D3EE 100%);
  box-shadow: 0 0 20px -4px rgba(124, 92, 255, 0.9);
}

.brand-ring {
  position: absolute;
  inset: -4px;
  border-radius: 14px;
  border: 1px solid rgba(34, 211, 238, 0.45);
  animation: spin-slow 8s linear infinite;
  border-top-color: transparent;
  border-left-color: transparent;
}

.brand-glyph {
  font-size: 17px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
}

.brand-text h1 {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.02em;
  margin: 0;
  background: var(--grad-text);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.brand-sub {
  display: block;
  font-size: 10px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--text-faint);
}

/* ===== 导航 ===== */
.sidebar-nav {
  flex: 1;
  padding: 14px 12px;
  overflow-y: auto;
}

.nav-section {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  color: var(--text-faint);
  padding: 0 10px;
  margin: 6px 0 10px;
}

.nav-section--second {
  margin-top: 22px;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 9px 12px;
  margin-bottom: 4px;
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--text-dim);
  cursor: pointer;
  text-decoration: none;
  overflow: hidden;
  transition: color 0.2s, background 0.2s;
}

.nav-item:hover {
  background: var(--surface-hover);
  color: var(--text);
}

.nav-item.active {
  color: var(--nav-active-text);
  background: var(--nav-active-bg);
  box-shadow: var(--nav-active-shadow);
}

.nav-item.active .nav-indicator {
  opacity: 1;
  transform: scaleY(1);
  box-shadow: 0 0 12px rgba(34, 211, 238, 0.9);
}

/* 激活指示条 */
.nav-indicator {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%) scaleY(0);
  width: 2px;
  height: 60%;
  border-radius: 0 2px 2px 0;
  background: linear-gradient(180deg, #7C5CFF, #22D3EE);
  opacity: 0;
  transition: all 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}

.nav-body {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.nav-title {
  font-size: 13px;
  font-weight: 500;
  line-height: 1.3;
}

.nav-desc {
  font-size: 10px;
  color: var(--text-faint);
  line-height: 1.3;
}

.nav-item.active .nav-desc {
  color: var(--nav-active-desc);
}

/* ===== 底部用户 ===== */
.sidebar-footer {
  position: relative;
  padding: 12px 14px 16px;
  border-top: 1px solid var(--border);
}

.user-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.2s;
}

.user-card:hover {
  background: var(--surface-hover);
}

.user-avatar,
.trigger-avatar {
  background: linear-gradient(135deg, #7C5CFF, #22D3EE);
  font-size: 13px;
  font-weight: 600;
  color: #fff;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 10px;
  color: var(--text-faint);
}

.logout-icon {
  color: var(--text-faint);
  transition: color 0.2s;
  cursor: pointer;
}

.logout-icon:hover {
  color: var(--ai-pink);
}

/* ===== 主内容 ===== */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  padding: 0 24px;
  flex-shrink: 0;
  background: var(--header-veil);
  border-bottom: 1px solid var(--border);
  backdrop-filter: blur(16px);
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
  min-width: 0;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.01em;
  margin: 0;
}

.page-desc {
  font-size: 12px;
  color: var(--text-faint);
  white-space: nowrap;
}

.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s, box-shadow 0.2s;
}

.user-trigger:hover {
  background: var(--surface-hover);
  box-shadow: 0 0 0 1px var(--border);
}

.trigger-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-soft);
  max-width: 120px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.trigger-arrow {
  color: var(--text-faint);
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

/* ===== 路由切换动效 ===== */
.page-enter-active,
.page-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 900px) {
  .page-desc {
    display: none;
  }
}

/* ===== 移动端菜单开关 ===== */
.menu-toggle {
  display: none;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  margin-right: 4px;
  border: none;
  border-radius: var(--radius-sm);
  color: var(--text-soft);
  background: var(--surface-hover);
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.menu-toggle:hover {
  color: var(--text);
  background: var(--surface-active);
}

/* ===== 移动端遮罩 ===== */
.mobile-mask {
  position: fixed;
  inset: 0;
  z-index: 1900;
  background: var(--mask-color);
  backdrop-filter: blur(2px);
}

.mask-fade-enter-active,
.mask-fade-leave-active {
  transition: opacity 0.25s ease;
}

.mask-fade-enter-from,
.mask-fade-leave-to {
  opacity: 0;
}

/* ============================================================
   移动端自适应
   ============================================================ */
@media (max-width: 768px) {
  .app-layout {
    flex-direction: column;
    --header-height: 54px;
  }

  /* 侧边栏改为抽屉 */
  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 2000;
    width: 258px;
    transform: translateX(-100%);
    transition: transform 0.28s cubic-bezier(0.22, 1, 0.36, 1);
    box-shadow: var(--shadow-pop);
  }

  .sidebar--open {
    transform: translateX(0);
  }

  .menu-toggle {
    display: inline-flex;
  }

  /* 头部压缩 */
  .header {
    height: 54px;
    padding: 0 12px;
    flex-shrink: 0;
  }

  .page-title {
    font-size: 16px;
    white-space: nowrap;
  }

  .header-right {
    gap: 10px;
  }

  .header-right .ai-badge,
  .trigger-name,
  .trigger-arrow {
    display: none;
  }

  .user-trigger {
    padding: 3px;
  }

  /* 内容区 */
  .content-area {
    padding: 14px 12px 20px;
  }
}
</style>
