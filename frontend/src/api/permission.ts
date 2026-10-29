import request from './request'
import type {
  RoleInfo,
  UserItem,
  DocPermissionItem,
  AuditLogItem,
  UpdateRoleRequest,
  UpdateStatusRequest,
  CreateUserRequest,
  GrantPermissionRequest,
  RevokePermissionRequest,
  AuditLogQueryParams
} from '@/types/permission'

/** 权限管理 API */
export const permissionApi = {
  /** 获取角色列表 */
  listRoles(): Promise<RoleInfo[]> {
    return request.get('/permissions/roles')
  },

  /** 获取用户列表 */
  listUsers(): Promise<UserItem[]> {
    return request.get('/permissions/users')
  },

  /** 创建用户 */
  createUser(data: CreateUserRequest): Promise<UserItem> {
    return request.post('/permissions/users', data)
  },

  /** 修改用户角色 */
  updateUserRole(userId: number, data: UpdateRoleRequest): Promise<void> {
    return request.put(`/permissions/users/${userId}/role`, data)
  },

  /** 启用/禁用用户 */
  updateUserStatus(userId: number, data: UpdateStatusRequest): Promise<void> {
    return request.put(`/permissions/users/${userId}/status`, data)
  },

  /** 授予文档权限 */
  grantDocumentPermission(documentId: number, data: GrantPermissionRequest): Promise<void> {
    return request.post(`/permissions/documents/${documentId}/grant`, data)
  },

  /** 撤销文档权限 */
  revokeDocumentPermission(documentId: number, data: RevokePermissionRequest): Promise<void> {
    return request.delete(`/permissions/documents/${documentId}/revoke`, { data })
  },

  /** 获取文档授权列表 */
  listDocumentPermissions(documentId: number): Promise<DocPermissionItem[]> {
    return request.get(`/permissions/documents/${documentId}/permissions`)
  },

  /** 查询审计日志 */
  queryAuditLogs(params: AuditLogQueryParams): Promise<{
    list: AuditLogItem[]
    total: number
    page: number
    size: number
  }> {
    return request.get('/permissions/audit-logs', { params })
  }
}
