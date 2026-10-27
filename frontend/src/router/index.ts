import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { getToken } from '@/utils/storage'

/**
 * 路由配置 — 登录页公开，其余需认证。
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login/index.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard/index.vue'),
        meta: { title: '工作台' }
      }
    ]
  },
  // 兜底：未匹配路由跳转首页
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/** 全局守卫 — 未登录跳转登录页 */
router.beforeEach((to) => {
  const token = getToken()

  if (to.meta.public) {
    // 已登录用户访问登录页，直接跳转首页
    if (token && to.name === 'Login') {
      return { path: '/dashboard' }
    }
    return true
  }

  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  return true
})

export default router
