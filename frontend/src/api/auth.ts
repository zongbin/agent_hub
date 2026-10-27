import request from './request'
import type { LoginRequest, LoginResponse, UserInfo } from '@/types/auth'

/** 更新个人资料请求 */
export interface UpdateProfileRequest {
  realName?: string
  email?: string
  department?: string
}

/** 修改密码请求 */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

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
  },

  /** 更新个人资料 */
  updateProfile(data: UpdateProfileRequest): Promise<UserInfo> {
    return request.put('/auth/me', data)
  },

  /** 修改密码 */
  changePassword(data: ChangePasswordRequest): Promise<void> {
    return request.put('/auth/me/password', data)
  }
}
