/**
 * 前端权限工具 — 与后端角色权限矩阵保持一致。
 *
 * 角色定义（后端 {@code UserRole}）：
 * - ADMIN 管理员：全部权限
 * - EDITOR 编辑者：读写
 * - VIEWER 查看者：只读
 * - GUEST 访客：只读（仅公开文档）
 */

/** 只读角色 —— 不能上传、删除知识库文档 */
export const READ_ONLY_ROLES: ReadonlyArray<string> = ['VIEWER', 'GUEST']

/**
 * 是否为只读角色。
 */
export function isReadOnlyRole(role?: string | null): boolean {
  return !!role && READ_ONLY_ROLES.includes(role)
}

/**
 * 是否可管理知识库文档（上传 / 删除）。
 */
export function canManageDocuments(role?: string | null): boolean {
  return !!role && !isReadOnlyRole(role)
}
