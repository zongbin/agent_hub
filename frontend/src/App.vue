<script setup lang="ts">
import { RouterView, RouterLink, useRoute } from 'vue-router';

const route = useRoute();
</script>

<template>
  <div class="app-container">
    <!-- 登录页使用独立布局 -->
    <RouterView v-if="route.path === '/login'" />

    <!-- 主框架布局 -->
    <el-container v-else class="h-screen">
      <el-aside width="220px" class="bg-slate-800">
        <div class="text-white text-center py-4 text-lg font-bold">
          智知 AgentHub
        </div>
        <el-menu
          :default-active="route.path"
          background-color="#1e293b"
          text-color="#cbd5e1"
          active-text-color="#3b82f6"
          router
        >
          <el-menu-item index="/dashboard">
            <el-icon><span>📊</span></el-icon>
            <span>工作台</span>
          </el-menu-item>
          <el-menu-item index="/qa">
            <el-icon><span>💬</span></el-icon>
            <span>智能问答</span>
          </el-menu-item>
          <el-sub-menu index="knowledge">
            <template #title>
              <el-icon><span>📚</span></el-icon>
              <span>知识库</span>
            </template>
            <el-menu-item index="/knowledge/list">文档列表</el-menu-item>
            <el-menu-item index="/knowledge/upload">上传文档</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="permission">
            <template #title>
              <el-icon><span>🔐</span></el-icon>
              <span>权限管理</span>
            </template>
            <el-menu-item index="/permission/users">用户管理</el-menu-item>
            <el-menu-item index="/permission/audit">审计日志</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="bg-white border-b flex items-center justify-between px-6">
          <span class="text-slate-600">{{ route.meta.title || '' }}</span>
          <el-dropdown>
            <span class="cursor-pointer text-slate-600">
              管理员 <el-icon><span>▼</span></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人设置</el-dropdown-item>
                <el-dropdown-item divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-header>
        <el-main>
          <RouterView />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style scoped>
.app-container {
  height: 100vh;
}
</style>
