import { getToken } from '@/utils/storage'

/** 智能体步骤事件 — 与服务端 AgentStepEventDTO 字段一一对应 */
export interface AgentStep {
  /** 事件类型：STEP_START / STEP_PROGRESS / STEP_COMPLETE / COMPLETE / ERROR */
  eventType: 'STEP_START' | 'STEP_PROGRESS' | 'STEP_COMPLETE'
  /** 步骤序号（从 1 开始） */
  stepIndex: number
  /** 步骤名称 — 本步选用的工具名，模型直接作答时为「回答」 */
  stepName?: string
  /** 进展描述 — 模型的思考内容或工具调用说明 */
  message?: string
  /** 工具名 */
  toolName?: string
  /** 工具执行结果 */
  toolResult?: string
}

/** 智能体回答 — 与服务端 AgentChatResponse 字段一一对应 */
export interface AgentAnswer {
  conversationId: string
  messageId: number
  answer: string
}

/** 智能体错误事件载荷 */
export interface AgentErrorEvent {
  message: string
}

/** 会话 */
export interface AgentConversation {
  id: string
  title: string
  createdAt: string
  updatedAt: string
}

/** 消息 */
export interface AgentMessage {
  id: number
  role: 'USER' | 'ASSISTANT'
  content: string
  steps: AgentStep[]
  createdAt: string
}

/** 前端持有的助手消息步骤（实时累积中） */
export interface PendingAssistantMessage {
  /** 步骤序号 → 步骤事件映射，便于按序号归组 */
  stepsByIndex: Map<number, AgentStep[]>
  answer: string
  messageId?: number
  streaming: boolean
}

/** SSE 回调集合 */
export interface AgentStreamHandlers {
  /** 步骤事件 */
  onStep: (step: AgentStep) => void
  /** 执行完成 */
  onComplete: (answer: AgentAnswer) => void
  /** 执行出错 */
  onError: (message: string) => void
}

/** 智能体 API */
export const agentApi = {
  /**
   * 发起流式智能体对话。
   *
   * <p>使用 fetch + ReadableStream 解析 SSE —— POST 请求无法使用 EventSource，
   * 且需要携带 Authorization 头。
   *
   * @param message       任务描述
   * @param conversationId 会话 ID（可选，为空则新建会话）
   * @param handlers       SSE 事件回调
   * @param signal         可用于取消请求
   */
  async chat(
    message: string,
    conversationId: string | null,
    handlers: AgentStreamHandlers,
    signal?: AbortSignal
  ): Promise<void> {
    const token = getToken()
    const response = await fetch('/api/agent/manus/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify({ message, conversationId }),
      signal
    })

    if (!response.ok) {
      if (response.status === 401) {
        window.location.href = '/login'
      }
      throw new Error(`智能体请求失败（HTTP ${response.status}）`)
    }
    if (!response.body) {
      throw new Error('当前浏览器不支持流式响应')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let currentEvent = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })

      // SSE 以空行分隔事件块
      let boundary = buffer.indexOf('\n\n')
      while (boundary !== -1) {
        const rawEvent = buffer.slice(0, boundary)
        buffer = buffer.slice(boundary + 2)

        for (const line of rawEvent.split('\n')) {
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            const rawData = line.slice(5).trim()
            if (rawData) {
              dispatchEvent(currentEvent, rawData, handlers)
            }
          }
        }
        currentEvent = ''
        boundary = buffer.indexOf('\n\n')
      }
    }
  },

  /**
   * 查询会话列表。
   */
  async conversations(): Promise<AgentConversation[]> {
    const res = await fetch('/api/agent/conversations', { headers: authHeaders() })
    const body = await res.json()
    if (body.code !== 0) {
      throw new Error(body.message || '查询会话列表失败')
    }
    return body.data ?? []
  },

  /**
   * 查询会话消息。
   */
  async messages(conversationId: string): Promise<AgentMessage[]> {
    const res = await fetch(`/api/agent/conversations/${conversationId}/messages`, {
      headers: authHeaders()
    })
    const body = await res.json()
    if (body.code !== 0) {
      throw new Error(body.message || '查询会话消息失败')
    }
    return (body.data ?? []).map((m: AgentMessage) => ({
      ...m,
      role: m.role.toLowerCase() as 'user' | 'assistant',
      steps: m.steps ?? []
    }))
  },

  /**
   * 删除会话。
   */
  async deleteConversation(conversationId: string): Promise<void> {
    const res = await fetch(`/api/agent/conversations/${conversationId}`, {
      method: 'DELETE',
      headers: authHeaders()
    })
    const body = await res.json()
    if (body.code !== 0) {
      throw new Error(body.message || '删除会话失败')
    }
  }
}

/** 构造带鉴权的请求头 */
function authHeaders(): Record<string, string> {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

/** 解析并分发单个 SSE 事件 */
function dispatchEvent(event: string, rawData: string, handlers: AgentStreamHandlers): void {
  let payload: Record<string, unknown>
  try {
    payload = JSON.parse(rawData)
  } catch {
    // 兼容非 JSON 的纯文本 data
    return
  }

  switch (event) {
    case 'agent.step.start':
    case 'agent.step.progress':
    case 'agent.step.complete':
      handlers.onStep(payload as unknown as AgentStep)
      break
    case 'agent.complete':
      handlers.onComplete(payload as unknown as AgentAnswer)
      break
    case 'agent.error':
      handlers.onError(String((payload as unknown as AgentErrorEvent).message ?? '智能体执行失败'))
      break
    default:
      break
  }
}
