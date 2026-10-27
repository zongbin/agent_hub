import axios from 'axios'
import type { ApiResponse } from '@/types/auth'
import { getToken, clearAuth } from '@/utils/storage'

/**
 * Axios 实例 — 统一请求配置、JWT 注入、错误处理。
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

/** 请求注入 JWT */
request.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/** 响应处理 — 解包统一格式，处理 401 */
request.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse<unknown>
    if (res.code === 0) {
      return res.data as typeof response
    }
    // 业务错误
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    if (error.response?.status === 401) {
      clearAuth()
      // 跳转登录页
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    const msg = error.response?.data?.message || error.message || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

export default request
