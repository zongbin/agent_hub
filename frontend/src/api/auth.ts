import request from './request'
import type { LoginRequest, LoginResponse, UserInfo } from '@/types/auth'

/** 认证 API */
export const authApi = {
  /** 登录 */
  login(data: LoginRequest): Promise<LoginResponse> {
    return request.post('/auth/login', data)
  },

  /** 登出 */
  logout(): Promise<void> {
    return request.post('/auth/logout')
  },

  /** 获取当前用户信息 */
  getCurrentUser(): Promise<UserInfo> {
    return request.get('/auth/me')
  }
}
