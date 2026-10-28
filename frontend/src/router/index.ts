import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { getToken, getUser } from '@/utils/storage'
import type { UserInfo } from '@/types/auth'

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
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/Knowledge/List.vue'),
        meta: { title: '知识库' }
      },
      {
        path: 'knowledge/upload',
        name: 'KnowledgeUpload',
        component: () => import('@/views/Knowledge/Upload.vue'),
        meta: { title: '上传文档' }
      },
      {
        path: 'knowledge/:id',
        name: 'KnowledgeDetail',
        component: () => import('@/views/Knowledge/Detail.vue'),
        meta: { title: '文档详情' }
      },
      {
        path: 'qa',
        name: 'QA',
        component: () => import('@/views/QAM/index.vue'),
        meta: { title: '智能问答' }
      },
      {
        path: 'agent',
        name: 'Agent',
        component: () => import('@/views/QAM/index.vue'),
        meta: { title: '超级智能体' }
      },
      {
        path: 'permission',
        name: 'Permission',
        component: () => import('@/views/Permission/index.vue'),
        meta: { title: '权限管理', adminOnly: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile/index.vue'),
        meta: { title: '个人设置' }
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

/** 全局守卫 — 未登录跳转登录页，非管理员禁止访问权限页 */
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

  // 校验 adminOnly 路由
  if (to.meta.adminOnly) {
    const user = getUser<UserInfo>()
    if (user?.role !== 'ADMIN') {
      return { path: '/dashboard' }
    }
  }

  return true
})

export default router
