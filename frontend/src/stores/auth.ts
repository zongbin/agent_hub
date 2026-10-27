import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/types/auth'
import { authApi } from '@/api/auth'
import { setToken, getToken, setUser, getUser, clearAuth } from '@/utils/storage'

/** 序列化用户信息为可存储格式 */
function serializeUser(user: UserInfo): string {
  return JSON.stringify(user)
}

/**
 * 认证状态管理。
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(getToken() || '')
  const user = ref<UserInfo | null>(getUser<UserInfo>())

  /** 是否已登录 */
  const isLoggedIn = computed(() => !!token.value)

  /** 登录 */
  async function login(username: string, password: string) {
    const res = await authApi.login({ username, password })
    token.value = res.token
    user.value = res.userInfo
    setToken(res.token)
    setUser(serializeUser(res.userInfo))
  }

  /** 登出 */
  async function logout() {
    try {
      await authApi.logout()
    } finally {
      token.value = ''
      user.value = null
      clearAuth()
    }
  }

  /** 加载当前用户信息 */
  async function fetchCurrentUser() {
    if (!token.value) return
    try {
      const info = await authApi.getCurrentUser()
      user.value = info
      setUser(serializeUser(info))
    } catch {
      token.value = ''
      user.value = null
      clearAuth()
    }
  }

  return { token, user, isLoggedIn, login, logout, fetchCurrentUser }
})
