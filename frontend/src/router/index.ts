import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';

/**
 * 路由配置 — 与后端 API 路径前缀 /api 无关，仅前端视图路由。
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login/index.vue'),
    meta: { title: '登录', layout: false },
  },
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard/index.vue'),
    meta: { title: '工作台' },
  },
  {
    path: '/qa',
    name: 'QAM',
    component: () => import('@/views/QAM/index.vue'),
    meta: { title: '智能问答' },
  },
  {
    path: '/knowledge',
    children: [
      { path: '', redirect: '/knowledge/list' },
      {
        path: 'list',
        name: 'KnowledgeList',
        component: () => import('@/views/Knowledge/List.vue'),
        meta: { title: '文档列表' },
      },
      {
        path: 'upload',
        name: 'KnowledgeUpload',
        component: () => import('@/views/Knowledge/Upload.vue'),
        meta: { title: '上传文档' },
      },
      {
        path: ':id',
        name: 'KnowledgeDetail',
        component: () => import('@/views/Knowledge/Detail.vue'),
        meta: { title: '文档详情' },
      },
    ],
  },
  {
    path: '/permission',
    children: [
      { path: '', redirect: '/permission/users' },
      {
        path: 'users',
        name: 'PermissionUsers',
        component: () => import('@/views/Permission/Users.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'audit',
        name: 'PermissionAudit',
        component: () => import('@/views/Permission/Audit.vue'),
        meta: { title: '审计日志' },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
