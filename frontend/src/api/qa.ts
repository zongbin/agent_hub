import { getToken } from '@/utils/storage'

/** 引用来源 */
export interface QaSource {
  documentId: number
  title: string
  chunkIndex: number
}

/** 问答结果 — 元数据与引用来源（回答内容已由 delta 推送） */
export interface QaAnswer {
  conversationId: string
  messageId: number
  sources: QaSource[]
}

/** 会话 */
export interface Conversation {
  id: string
  title: string
  createdAt: string
  updatedAt: string
}

/** 消息 */
export interface Message {
  id: number
  role: 'user' | 'assistant'
  content: string
  sources?: QaSource[]
  feedback?: number | null
  createdAt: string
}

/** SSE 回调集合 */
export interface QaStreamHandlers {
  /** 答案增量 */
  onDelta: (content: string) => void
  /** 生成完成（元数据与引用来源，回答内容已由 delta 推送） */
  onDone: (answer: QaAnswer) => void
  /** 发生错误 */
  onError: (message: string) => void
}

/** 问答 API */
export const qaApi = {
  /**
   * 发起流式问答。
   *
   * <p>使用 fetch + ReadableStream 解析 SSE —— POST 请求无法使用 EventSource。
   *
   * @param question       问题
   * @param conversationId 会话 ID（可选，为空则新建会话）
   * @param handlers       SSE 事件回调
   * @param signal         可用于取消请求
   */
  async askQuestion(
    question: string,
    conversationId: string | null,
    handlers: QaStreamHandlers,
    signal?: AbortSignal
  ): Promise<void> {
    const token = getToken()
    const response = await fetch('/api/qa/ask', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify({ question, conversationId }),
      signal
    })

    if (!response.ok) {
      if (response.status === 401) {
        window.location.href = '/login'
      }
      throw new Error(`问答请求失败（HTTP ${response.status}）`)
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
   * 提交答案反馈。
   */
  async feedback(messageId: number, useful: boolean): Promise<void> {
    const token = getToken()
    const response = await fetch('/api/qa/feedback', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify({ messageId, useful })
    })
    if (!response.ok) {
      throw new Error(`反馈提交失败（HTTP ${response.status}）`)
    }
  },

  /**
   * 查询会话列表。
   */
  async conversations(): Promise<Conversation[]> {
    const res = await fetch('/api/qa/conversations', { headers: authHeaders() })
    const body = await res.json()
    if (body.code !== 0) {
      throw new Error(body.message || '查询会话列表失败')
    }
    return body.data ?? []
  },

  /**
   * 查询会话消息。
   */
  async messages(conversationId: string): Promise<Message[]> {
    const res = await fetch(`/api/qa/conversations/${conversationId}/messages`, {
      headers: authHeaders()
    })
    const body = await res.json()
    if (body.code !== 0) {
      throw new Error(body.message || '查询会话消息失败')
    }
    return body.data ?? []
  },

  /**
   * 删除会话。
   */
  async deleteConversation(conversationId: string): Promise<void> {
    const res = await fetch(`/api/qa/conversations/${conversationId}`, {
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
function dispatchEvent(event: string, rawData: string, handlers: QaStreamHandlers): void {
  let payload: Record<string, unknown>
  try {
    payload = JSON.parse(rawData)
  } catch {
    // 兼容非 JSON 的纯文本 data
    if (event === 'delta') {
      handlers.onDelta(rawData)
    }
    return
  }

  switch (event) {
    case 'delta':
      handlers.onDelta(String(payload.content ?? ''))
      break
    case 'done':
      handlers.onDone({
        conversationId: String(payload.conversationId ?? ''),
        messageId: Number(payload.messageId ?? 0),
        sources: (payload.sources as QaSource[]) ?? []
      })
      break
    case 'error':
      handlers.onError(String(payload.message ?? '问答处理失败'))
      break
    default:
      break
  }
}
