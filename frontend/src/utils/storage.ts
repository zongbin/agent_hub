/**
 * 本地存储工具 — 管理 Token 和用户信息。
 */

const TOKEN_KEY = 'agenthub_token'
const USER_KEY = 'agenthub_user'

/** 保存 Token */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

/** 获取 Token */
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

/** 清除 Token */
export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

/** 保存用户信息 */
export function setUser(userJson: string): void {
  localStorage.setItem(USER_KEY, userJson)
}

/** 获取用户信息 */
export function getUser<T = Record<string, unknown>>(): T | null {
  const data = localStorage.getItem(USER_KEY)
  if (!data) return null
  try {
    return JSON.parse(data) as T
  } catch {
    return null
  }
}

/** 清除用户信息 */
export function removeUser(): void {
  localStorage.removeItem(USER_KEY)
}

/** 清除所有认证数据 */
export function clearAuth(): void {
  removeToken()
  removeUser()
}
