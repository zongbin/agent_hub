import request from './request'

/** 知识库文档统计 */
export interface DocumentStatVO {
  total: number
  active: number
  processing: number
  failed: number
  chunkTotal: number
  sizeBytes: number
}

/** 智能问答统计 */
export interface QaStatVO {
  conversationTotal: number
  questionTotal: number
  todayQuestionCount: number
}

/** 超级智能体统计 */
export interface AgentStatVO {
  conversationTotal: number
  taskTotal: number
  todayTaskCount: number
}

/** 操作活跃度统计 */
export interface ActivityStatVO {
  todayActions: number
  weekActions: number
  totalActions: number
}

/** 密级分布项 */
export interface LevelCountVO {
  level: string
  count: number
}

/** 最近文档 */
export interface RecentDocumentVO {
  id: number
  title: string
  fileType: string
  fileSize: number
  securityLevel: string
  status: 'PROCESSING' | 'ACTIVE' | 'FAILED' | 'DELETED'
  chunkCount: number
  createdAt: string
}

/** 最近会话 */
export interface RecentConversationVO {
  type: 'QA' | 'AGENT'
  id: string
  title: string
  updatedAt: string
}

/** 趋势数据点 */
export interface TrendPointVO {
  date: string
  qaCount: number
  agentCount: number
}

/** 工作台统计数据 */
export interface DashboardStatsVO {
  document: DocumentStatVO
  qa: QaStatVO
  agent: AgentStatVO
  activity: ActivityStatVO
  levelDistribution: LevelCountVO[]
  recentDocuments: RecentDocumentVO[]
  recentConversations: RecentConversationVO[]
  trend: TrendPointVO[]
}

/** 工作台 API */
export const dashboardApi = {
  /** 查询工作台统计数据 */
  stats(): Promise<DashboardStatsVO> {
    return request.get('/dashboard/stats')
  }
}
