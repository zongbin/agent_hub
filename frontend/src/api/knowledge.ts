import request from './request'

/** 文档处理状态（与后端 DocumentStatus 枚举一致） */
export type DocumentStatus = 'PROCESSING' | 'ACTIVE' | 'FAILED' | 'DELETED'

/** 文档信息 */
export interface DocumentVO {
  id: number
  title: string
  fileName: string
  fileType: string
  fileSize: number
  securityLevel: string
  summary: string
  tags: string[]
  status: DocumentStatus
  chunkCount: number
  createdAt: string
}

/** 文档分块 */
export interface DocumentChunk {
  /** 分块序号（从 0 开始） */
  chunkIndex: number
  /** 分块文本内容 */
  content: string
  /** 分块字符数 */
  charCount: number
}

/** 文档上传请求元信息 */
export interface DocumentUploadRequest {
  title?: string
  securityLevel: string
  tags?: string
}

/** 文档查询参数 */
export interface DocumentQuery {
  page?: number
  size?: number
  securityLevel?: string
  tag?: string
  status?: DocumentStatus
}

/** 分页文档列表响应 */
export interface DocumentListResponse {
  list: DocumentVO[]
  total: number
  page: number
  size: number
}

/** 知识库 API */
export const knowledgeApi = {
  /**
   * 上传文档（Multipart 表单）。
   *
   * @param file    文件对象
   * @param request 上传元信息（JSON 字符串）
   */
  upload(file: File, requestMeta: string): Promise<DocumentVO> {
    const formData = new FormData()
    formData.append('file', file)
    // 以 application/json Blob 发送，Spring 才能用 Jackson 反序列化为 DocumentUploadRequest
    formData.append('request', new Blob([requestMeta], { type: 'application/json' }))
    // 不手动设置 Content-Type，让 axios 自动带上正确的 boundary
    return request.post('/documents/upload', formData)
  },

  /**
   * 分页查询文档列表。
   */
  list(params: DocumentQuery): Promise<DocumentListResponse> {
    return request.get('/documents', { params })
  },

  /**
   * 查询文档详情。
   */
  detail(id: number): Promise<DocumentVO> {
    return request.get(`/documents/${id}`)
  },

  /**
   * 查询文档的分块内容。
   */
  chunks(id: number): Promise<DocumentChunk[]> {
    return request.get(`/documents/${id}/chunks`)
  },

  /**
   * 删除文档（逻辑删除）。
   */
  delete(id: number): Promise<void> {
    return request.delete(`/documents/${id}`)
  }
}
