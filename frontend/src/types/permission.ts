/** 角色信息 */
export interface RoleInfo {
  code: string
  name: string
  description: string
}

/** 用户信息（权限管理用） */
export interface UserItem {
  id: number
  username: string
  realName: string
  email: string
  role: string
  department: string
  status: number
  createdAt: string
}

/** 文档授权信息 */
export interface DocPermissionItem {
  id: number
  documentId: number
  documentName: string
  granteeId: number
  granteeName: string
  permission: string
  createdAt: string
}

/** 审计日志 */
export interface AuditLogItem {
  id: number
  userId: number
  username: string
  action: string
  resource: string
  detail: Record<string, unknown>
  ipAddress: string
  success: number
  createdAt: string
}

/** 修改角色请求 */
export interface UpdateRoleRequest {
  role: string
}

/** 更新用户状态请求 */
export interface UpdateStatusRequest {
  status: number
}

/** 创建用户请求 */
export interface CreateUserRequest {
  username: string
  password: string
  realName: string
  email: string
  role: string
  department: string
}

/** 授权请求 */
export interface GrantPermissionRequest {
  userId: number
  permission: string
}

/** 撤销授权请求 */
export interface RevokePermissionRequest {
  userId: number
}

/** 审计日志查询参数（分页字段与后端 PageQuery 保持一致：page 从 1 开始） */
export interface AuditLogQueryParams {
  userId?: number
  actionType?: string
  startTime?: string
  endTime?: string
  page: number
  size: number
}
