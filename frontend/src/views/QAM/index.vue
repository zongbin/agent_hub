<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Plus, Promotion, ChatLineRound, Link, Files, MagicStick } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import { qaApi } from '@/api/qa'
import type { Conversation, QaSource } from '@/api/qa'
import { knowledgeApi, type DocumentChunk, type DocumentVO } from '@/api/knowledge'
import ConversationList from '@/components/ConversationList.vue'
import { useResponsive } from '@/composables/useResponsive'

/** markdown-it 实例 — 配置为安全默认值 */
const md = new MarkdownIt({ html: false, linkify: true, typographer: true })

/** 渲染 markdown 文本为 HTML */
const renderMarkdown = (text: string): string => {
  if (!text) return ''
  return md.render(text)
}

interface ChatMessage {
  id?: number
  role: 'user' | 'assistant'
  content: string
  sources?: QaSource[]
  feedback?: 'up' | 'down'
  streaming?: boolean
}

const route = useRoute()
const messages = ref<ChatMessage[]>([])
const input = ref('')
const loading = ref(false)
const conversations = ref<Conversation[]>([])
const currentConversationId = ref<string | null>(null)
const sidebarVisible = ref(true)

const { isMobile } = useResponsive()

/** 移动端历史会话抽屉 */
const convDrawerVisible = ref(false)

/** 引用文档详情抽屉 */
const docDrawerVisible = ref(false)
const docDrawerLoading = ref(false)
const docDrawerDoc = ref<DocumentVO | null>(null)
const docDrawerChunks = ref<DocumentChunk[]>([])
const docDrawerActiveChunk = ref<number | undefined>(undefined)

/** 加载文档详情抽屉内容 */
const loadDocDrawer = async (documentId: number, chunkIndex?: number) => {
  docDrawerLoading.value = true
  docDrawerDoc.value = null
  docDrawerChunks.value = []
  docDrawerActiveChunk.value = chunkIndex
  docDrawerVisible.value = true
  try {
    docDrawerDoc.value = await knowledgeApi.detail(documentId)
    docDrawerChunks.value = await knowledgeApi.chunks(documentId)
    // 等待 DOM 渲染后滚动到引用分块
    if (chunkIndex != null) {
      await nextTick()
      await nextTick()
      const el = document.querySelector(`[data-chunk-index="${chunkIndex}"]`)
      el?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  } catch (error: unknown) {
    const err = error as { message?: string; response?: { status?: number } }
    if (err.response?.status === 403) {
      ElMessage.error('权限不足，无法访问该文档')
    } else {
      ElMessage.error(err.message || '加载文档详情失败')
    }
    docDrawerVisible.value = false
  } finally {
    docDrawerLoading.value = false
  }
}

/** 加载会话列表 */
const loadConversations = async () => {
  try {
    conversations.value = await qaApi.conversations()
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

/** 新建会话 */
const startNewConversation = () => {
  currentConversationId.value = null
  messages.value = []
  input.value = ''
}

/** 切换会话 */
const openConversation = async (conversation: Conversation) => {
  try {
    const list = await qaApi.messages(conversation.id)
    currentConversationId.value = conversation.id
    messages.value = list.map((m) => ({
      id: m.id,
      role: m.role,
      content: m.content,
      sources: m.sources ?? [],
      feedback: m.feedback === 1 ? 'up' : m.feedback === 0 ? 'down' : undefined
    }))
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    convDrawerVisible.value = false
  }
}

/** 删除会话 */
const removeConversation = async (conversation: Conversation) => {
  try {
    await ElMessageBox.confirm(`确定删除会话「${conversation.title}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await qaApi.deleteConversation(conversation.id)
    ElMessage.success('会话已删除')
    if (currentConversationId.value === conversation.id) {
      startNewConversation()
    }
    await loadConversations()
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

const sendMessage = async () => {
  const question = input.value.trim()
  if (!question || loading.value) return

  messages.value.push({ role: 'user', content: question })
  input.value = ''
  loading.value = true

  messages.value.push({ role: 'assistant', content: '', streaming: true })
  // 取回数组中的响应式代理 —— 直接修改 push 进去的原始对象不会触发渲染
  const assistant = messages.value[messages.value.length - 1]

  try {
    await qaApi.askQuestion(
      question,
      currentConversationId.value,
      {
        onDelta: (content) => {
          assistant.content += content
        },
        onDone: (answer) => {
          // 回答内容已由 delta 推送，此处只取元数据
          if (answer.conversationId) {
            currentConversationId.value = answer.conversationId
          }
          assistant.sources = answer.sources ?? []
          assistant.id = answer.messageId
          assistant.streaming = false
          loadConversations()
        },
        onError: (message) => {
          assistant.content = assistant.content || `回答生成失败：${message}`
          assistant.streaming = false
        }
      }
    )
  } catch (error) {
    assistant.streaming = false
    assistant.content = assistant.content || (error as Error).message
    ElMessage.error((error as Error).message)
  } finally {
    assistant.streaming = false
    loading.value = false
  }
}

const submitFeedback = async (message: ChatMessage, useful: boolean) => {
  if (!message.id) {
    ElMessage.warning('该回答暂不支持反馈')
    return
  }
  try {
    await qaApi.feedback(message.id, useful)
    message.feedback = useful ? 'up' : 'down'
    ElMessage.success('感谢反馈')
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

const openDocument = (source: QaSource) => {
  if (source.documentId != null) {
    loadDocDrawer(source.documentId, source.chunkIndex)
  }
}

/** 在新标签页打开文档详情 */
const openDocumentInNewTab = (documentId: number) => {
  window.open(`/knowledge/${documentId}`, '_blank')
}

/** 格式化文件大小 */
const formatFileSize = (bytes: number): string => {
  if (!bytes) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

/** 状态标签样式 */
const statusTagType = (status: string): string => {
  const map: Record<string, string> = { PROCESSING: 'warning', ACTIVE: 'success', FAILED: 'danger' }
  return map[status] || 'info'
}

const statusLabel = (status: string): string => {
  const map: Record<string, string> = { PROCESSING: '处理中', ACTIVE: '已就绪', FAILED: '处理失败' }
  return map[status] || status
}

const securityLabel = (level: string): string => {
  const map: Record<string, string> = { PUBLIC: '公开', INTERNAL: '内部', CONFIDENTIAL: '机密' }
  return map[level] || level
}

onMounted(async () => {
  await loadConversations()
  // 支持从工作台「最近会话」跳转
  const targetId = route.query.conversationId
  if (typeof targetId === 'string' && targetId) {
    const target = conversations.value.find((c) => c.id === targetId)
    if (target) {
      await openConversation(target)
    }
  }
})
</script>

<template>
  <div class="qa-layout">
    <!-- 会话列表 -->
    <aside v-show="sidebarVisible" class="conv-panel glass-card">
      <div class="conv-head">
        <el-button type="primary" class="new-btn" @click="startNewConversation">
          <el-icon><Plus /></el-icon>
          新建对话
        </el-button>
      </div>

      <ConversationList
        :conversations="conversations"
        :current-id="currentConversationId"
        @select="openConversation"
        @remove="removeConversation"
      />

      <div class="conv-foot">
        <span class="ai-badge">
          <span class="ai-dot"></span>
          语义检索已连接
        </span>
      </div>
    </aside>

    <!-- 移动端历史会话抽屉 -->
    <!-- append-to-body：避免受 .qa-layout 动画残留 transform 影响而脱离视口 -->
    <el-drawer
      v-model="convDrawerVisible"
      title="历史对话"
      direction="ltr"
      :size="isMobile ? '82%' : 320"
      class="conv-drawer"
      append-to-body
    >
      <el-button
        type="primary"
        class="new-btn drawer-new-btn"
        @click="startNewConversation(); convDrawerVisible = false"
      >
        <el-icon><Plus /></el-icon>
        新建对话
      </el-button>
      <ConversationList
        :conversations="conversations"
        :current-id="currentConversationId"
        @select="openConversation"
        @remove="removeConversation"
      />
    </el-drawer>

    <!-- 聊天区 -->
    <section class="chat-panel glass-card">
      <header class="chat-head">
        <div class="chat-head-left">
          <button
            class="conv-toggle"
            type="button"
            aria-label="打开历史对话"
            @click="convDrawerVisible = true"
          >
            <el-icon :size="18"><Clock /></el-icon>
          </button>
          <el-icon :size="18" class="chat-icon"><ChatLineRound /></el-icon>
          <div>
            <div class="chat-title">智能问答</div>
            <div class="chat-sub">RAG · 向量召回 + 重排序 + 引用溯源</div>
          </div>
        </div>
        <span class="ai-badge">
          <span class="ai-dot"></span>
          模型就绪
        </span>
      </header>

      <div class="chat-body">
        <!-- 空状态 -->
        <div v-if="messages.length === 0" class="empty-state">
          <div class="empty-orb">
            <el-icon :size="28"><ChatLineRound /></el-icon>
          </div>
          <h3 class="empty-title">开始提问吧</h3>
          <p class="empty-desc">
            提问后系统会先在知识库中做语义召回，再结合原文生成带引用的回答。
          </p>
          <div class="empty-chips">
            <span class="empty-chip">公司报销流程是怎样的？</span>
            <span class="empty-chip">总结最新的产品规划文档</span>
            <span class="empty-chip">哪些客户处于合同续约期？</span>
          </div>
        </div>

        <!-- 消息列表 -->
        <div
          v-for="(m, i) in messages"
          :key="i"
          class="msg-row"
          :class="m.role === 'user' ? 'msg-row--user' : 'msg-row--ai'"
        >
          <div v-if="m.role === 'assistant'" class="msg-avatar ai-avatar">
            <el-icon :size="14"><ChatLineRound /></el-icon>
          </div>

          <div class="msg-bubble" :class="m.role === 'user' ? 'bubble-user' : 'bubble-ai'">
            <!-- 用户消息：纯文本 -->
            <pre v-if="m.role === 'user'" class="msg-text">{{ m.content }}</pre>
            <!-- 助手消息：Markdown 渲染 -->
            <div
              v-else
              class="msg-markdown"
              v-html="renderMarkdown(m.content)"
            ></div>
            <span v-if="m.streaming" class="caret">▍</span>

            <!-- 流式等待指示 -->
            <div v-if="m.streaming && !m.content" class="thinking">
              <span></span><span></span><span></span>
            </div>

            <!-- 引用来源 -->
            <div v-if="m.role === 'assistant' && m.sources && m.sources.length" class="sources">
              <div class="sources-head">
                <span class="sources-dot"></span>
                引用来源
              </div>
              <div class="sources-list">
                <a
                  v-for="(s, si) in m.sources"
                  :key="si"
                  class="source-chip"
                  href="javascript:void(0)"
                  @click.prevent="openDocument(s)"
                >
                  <span class="source-index">{{ si + 1 }}</span>
                  <span class="source-chip-text">
                    {{ s.title }}<template v-if="s.chunkIndex != null"> · 第 {{ s.chunkIndex + 1 }} 段</template>
                  </span>
                  <el-icon :size="12" class="source-link-icon"><Link /></el-icon>
                </a>
              </div>
            </div>

            <!-- 反馈 -->
            <div
              v-if="m.role === 'assistant' && !m.streaming && m.content"
              class="feedback"
            >
              <span class="feedback-label">这个回答有帮助吗？</span>
              <button
                class="fb-btn"
                :class="{ on: m.feedback === 'up' }"
                :disabled="m.feedback === 'up'"
                @click="submitFeedback(m, true)"
              >
                👍 有帮助
              </button>
              <button
                class="fb-btn"
                :class="{ on: m.feedback === 'down' }"
                :disabled="m.feedback === 'down'"
                @click="submitFeedback(m, false)"
              >
                👎 无帮助
              </button>
            </div>
          </div>

          <div v-if="m.role === 'user'" class="msg-avatar user-avatar">
            {{ '我' }}
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <footer class="composer">
        <div class="composer-inner">
          <el-input
            v-model="input"
            type="textarea"
            :rows="3"
            resize="none"
            placeholder="输入你的问题... (Enter 发送)"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <div class="composer-actions">
            <span class="composer-hint">Enter 发送 · Shift+Enter 换行</span>
            <el-button type="primary" :loading="loading" @click="sendMessage">
              <el-icon class="mr-1"><Promotion /></el-icon>
              发送
            </el-button>
          </div>
        </div>
      </footer>
    </section>

    <!-- 引用文档详情抽屉 -->
    <el-drawer
      v-model="docDrawerVisible"
      direction="rtl"
      :size="isMobile ? '90%' : '50%'"
      :with-header="false"
      class="doc-drawer"
      append-to-body
    >
      <div v-loading="docDrawerLoading" class="doc-drawer-content">
        <div v-if="docDrawerDoc" class="doc-drawer-inner">
          <!-- 抽屉头部 -->
          <div class="doc-drawer-head">
            <div class="doc-drawer-title-row">
              <span
                class="doc-drawer-security"
                :class="`security-${docDrawerDoc.securityLevel?.toLowerCase()}`"
              >
                {{ securityLabel(docDrawerDoc.securityLevel) }}
              </span>
              <h2 class="doc-drawer-title">{{ docDrawerDoc.title }}</h2>
            </div>
            <el-button
              link
              size="small"
              :icon="Link"
              @click="openDocumentInNewTab(docDrawerDoc.id)"
            >
              新标签打开
            </el-button>
          </div>

          <!-- 元信息 -->
          <div class="doc-drawer-meta">
            <span>{{ docDrawerDoc.fileType?.toUpperCase() }} · {{ formatFileSize(docDrawerDoc.fileSize) }}</span>
            <el-tag :type="statusTagType(docDrawerDoc.status)" size="small">
              {{ statusLabel(docDrawerDoc.status) }}
            </el-tag>
          </div>

          <!-- AI 摘要 -->
          <div v-if="docDrawerDoc.summary" class="doc-drawer-section">
            <div class="doc-drawer-section-title">
              <el-icon><MagicStick /></el-icon>
              AI 摘要
            </div>
            <div class="doc-drawer-summary md-render" v-html="renderMarkdown(docDrawerDoc.summary)"></div>
          </div>

          <!-- 分块内容 -->
          <div class="doc-drawer-section">
            <div class="doc-drawer-section-title">
              <el-icon><Files /></el-icon>
              分块内容
              <span v-if="docDrawerChunks.length" class="doc-drawer-count">
                共 {{ docDrawerChunks.length }} 块
              </span>
            </div>
            <el-collapse v-model="docDrawerActiveChunk" accordion>
              <el-collapse-item
                v-for="chunk in docDrawerChunks"
                :key="chunk.chunkIndex"
                :name="chunk.chunkIndex"
              >
                <template #title>
                  <div
                    class="chunk-title"
                    :data-chunk-index="chunk.chunkIndex"
                    :class="{ 'chunk-highlighted': chunk.chunkIndex === docDrawerActiveChunk }"
                  >
                    <span class="chunk-tag">#{{ chunk.chunkIndex + 1 }}</span>
                    <span class="chunk-chars">{{ chunk.charCount }} 字</span>
                  </div>
                </template>
                <div class="chunk-content md-render" v-html="renderMarkdown(chunk.content)"></div>
              </el-collapse-item>
            </el-collapse>
          </div>

          <!-- 处理中 / 失败提示 -->
          <el-alert
            v-if="docDrawerDoc.status === 'PROCESSING'"
            type="warning"
            :closable="false"
            show-icon
            class="doc-drawer-alert"
          >
            <template #title>文档正在处理中，请稍后刷新查看结果。</template>
          </el-alert>
          <el-alert
            v-if="docDrawerDoc.status === 'FAILED'"
            type="error"
            :closable="false"
            show-icon
            class="doc-drawer-alert"
          >
            <template #title>文档处理失败，请检查文件格式或重新上传。</template>
          </el-alert>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.qa-layout {
  display: flex;
  gap: 16px;
  height: calc(100vh - var(--header-height) - 48px);
  height: calc(100dvh - var(--header-height) - 48px);
  animation: rise 0.45s cubic-bezier(0.22, 1, 0.36, 1) backwards;
}

/* ===== 会话列表 ===== */
.conv-panel {
  width: 250px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.conv-head {
  padding: 14px;
  border-bottom: 1px solid var(--border);
}

.new-btn {
  width: 100%;
  font-weight: 500;
}

.conv-foot {
  padding: 12px 14px;
  border-top: 1px solid var(--border);
}

/* ===== 聊天区 ===== */
.chat-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 13px 20px;
  border-bottom: 1px solid var(--border);
  background: var(--surface-soft);
}

.chat-head-left {
  display: flex;
  align-items: center;
  gap: 11px;
}

.chat-icon {
  color: var(--accent-text);
  filter: drop-shadow(0 0 8px rgba(34, 211, 238, 0.8));
}

.chat-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.chat-sub {
  font-size: 11px;
  color: var(--text-faint);
  margin-top: 1px;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 22px 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ===== 空状态 ===== */
.empty-state {
  margin: auto;
  text-align: center;
  max-width: 460px;
  animation: rise 0.5s ease backwards;
}

.empty-orb {
  width: 62px;
  height: 62px;
  margin: 0 auto 18px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #7C5CFF, #22D3EE);
  box-shadow: 0 0 40px -6px rgba(124, 92, 255, 0.95);
  animation: float 6s ease-in-out infinite;
}

.empty-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text);
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-faint);
  margin: 0 0 20px;
}

.empty-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.empty-chip {
  font-size: 12px;
  color: var(--text-dim);
  padding: 6px 13px;
  border-radius: 999px;
  background: var(--glass);
  border: 1px solid var(--border);
}

/* ===== 消息 ===== */
.msg-row {
  display: flex;
  gap: 11px;
  align-items: flex-start;
  animation: rise 0.32s ease backwards;
}

.msg-row--user {
  justify-content: flex-end;
}

.msg-avatar {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.ai-avatar {
  color: #fff;
  background: linear-gradient(135deg, #7C5CFF, #22D3EE);
  box-shadow: 0 0 16px -4px rgba(124, 92, 255, 0.9);
}

.user-avatar {
  color: var(--text-soft);
  background: var(--surface-active);
  border: 1px solid var(--border);
}

.msg-bubble {
  max-width: 72%;
  padding: 13px 16px;
  border-radius: 14px;
}

.bubble-user {
  background: linear-gradient(135deg, rgba(124, 92, 255, 0.9) 0%, rgba(56, 189, 248, 0.82) 100%);
  color: #fff;
  border-bottom-right-radius: 4px;
  box-shadow: 0 10px 30px -14px rgba(124, 92, 255, 0.95);
}

.bubble-ai {
  background: var(--glass);
  border: 1px solid var(--border);
  color: var(--text-soft);
  border-bottom-left-radius: 4px;
}

.msg-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 13.5px;
  line-height: 1.85;
}

/* ===== Markdown 渲染 ===== */
.msg-markdown {
  font-size: 13.5px;
  line-height: 1.85;
  color: var(--text-soft);
  word-break: break-word;
}

.msg-markdown :deep(h1),
.msg-markdown :deep(h2),
.msg-markdown :deep(h3),
.msg-markdown :deep(h4) {
  margin: 1em 0 0.5em;
  font-weight: 700;
  color: var(--text);
  line-height: 1.4;
}

.msg-markdown :deep(h1) { font-size: 1.4em; }
.msg-markdown :deep(h2) { font-size: 1.25em; }
.msg-markdown :deep(h3) { font-size: 1.12em; }
.msg-markdown :deep(h4) { font-size: 1em; }

.msg-markdown :deep(p) {
  margin: 0 0 0.6em;
}

.msg-markdown :deep(ul),
.msg-markdown :deep(ol) {
  margin: 0.4em 0;
  padding-left: 1.6em;
}

.msg-markdown :deep(li) {
  margin: 0.2em 0;
}

.msg-markdown :deep(blockquote) {
  margin: 0.6em 0;
  padding: 6px 14px;
  border-left: 3px solid var(--ai-cyan);
  background: var(--tint-cyan-bg);
  color: var(--text-dim);
  border-radius: 0 8px 8px 0;
}

.msg-markdown :deep(code) {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 0.88em;
  padding: 2px 6px;
  border-radius: 5px;
  background: var(--tint-cyan-bg);
  color: var(--accent-text);
  border: 1px solid var(--tint-cyan-border);
}

.msg-markdown :deep(pre) {
  margin: 0.6em 0;
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(8, 14, 28, 0.85);
  border: 1px solid var(--border);
  overflow-x: auto;
}

.msg-markdown :deep(pre code) {
  padding: 0;
  background: none;
  border: none;
  color: #c9d1d9;
  font-size: 12.5px;
  line-height: 1.7;
}

.msg-markdown :deep(a) {
  color: var(--accent-text);
  text-decoration: underline;
  text-underline-offset: 2px;
}

.msg-markdown :deep(a:hover) {
  color: #7C5CFF;
}

.msg-markdown :deep(table) {
  margin: 0.6em 0;
  border-collapse: collapse;
  font-size: 13px;
  width: 100%;
}

.msg-markdown :deep(th),
.msg-markdown :deep(td) {
  padding: 6px 12px;
  border: 1px solid var(--border);
  text-align: left;
}

.msg-markdown :deep(th) {
  background: var(--surface-hover);
  font-weight: 600;
  color: var(--text);
}

.msg-markdown :deep(hr) {
  margin: 0.8em 0;
  border: none;
  border-top: 1px solid var(--border);
}

.msg-markdown :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.msg-markdown :deep(strong) {
  color: var(--text);
  font-weight: 700;
}

.caret {
  color: var(--accent-text);
  animation: caret 1s step-end infinite;
}

/* 思考指示 */
.thinking {
  display: flex;
  gap: 5px;
  padding: 3px 0;
}

.thinking span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ai-cyan);
  box-shadow: 0 0 8px rgba(34, 211, 238, 0.9);
  animation: pulseGlow 1.2s ease-in-out infinite;
}

.thinking span:nth-child(2) { animation-delay: 0.18s; }
.thinking span:nth-child(3) { animation-delay: 0.36s; }

/* ===== 引用来源 ===== */
.sources {
  margin-top: 13px;
  padding-top: 11px;
  border-top: 1px dashed var(--border);
}

.sources-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.1em;
  color: var(--text-faint);
  margin-bottom: 8px;
}

.sources-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--ai-lime);
  box-shadow: 0 0 8px var(--ai-lime);
}

.sources-list {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.source-chip {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: var(--accent-text);
  padding: 6px 10px;
  border-radius: 8px;
  background: var(--tint-cyan-bg);
  border: 1px solid var(--tint-cyan-border);
  cursor: pointer;
  text-decoration: none;
  transition: background 0.2s, border-color 0.2s, transform 0.2s, box-shadow 0.2s;
}

.source-chip:hover {
  background: rgba(34, 211, 238, 0.16);
  border-color: var(--ai-cyan);
  transform: translateX(2px);
  box-shadow: 0 0 12px -4px rgba(34, 211, 238, 0.5);
}

.source-chip-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-link-icon {
  flex-shrink: 0;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.source-chip:hover .source-link-icon {
  opacity: 1;
}

.source-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 700;
  color: #062024;
  background: var(--ai-cyan);
  flex-shrink: 0;
}

/* ===== 反馈 ===== */
.feedback {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 13px;
  padding-top: 11px;
  border-top: 1px dashed var(--border);
}

.feedback-label {
  font-size: 11px;
  color: var(--text-faint);
}

.fb-btn {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 999px;
  color: var(--text-dim);
  background: var(--glass);
  border: 1px solid var(--border);
  cursor: pointer;
  transition: all 0.2s;
}

.fb-btn:hover:not(:disabled) {
  color: var(--text);
  border-color: var(--ai-cyan);
}

.fb-btn.on {
  color: #062024;
  background: var(--ai-cyan);
  border-color: var(--ai-cyan);
  font-weight: 600;
}

.fb-btn:disabled {
  cursor: default;
}

/* ===== 输入区 ===== */
.composer {
  border-top: 1px solid var(--border);
  padding: 14px 20px 16px;
  background: var(--surface-soft);
}

.composer-inner {
  position: relative;
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.composer-hint {
  font-size: 11px;
  color: var(--text-faint);
}

/* ===== 移动端历史会话开关 ===== */
.conv-toggle {
  display: none;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border: none;
  border-radius: var(--radius-sm);
  color: var(--text-soft);
  background: var(--surface-hover);
  cursor: pointer;
}

.conv-toggle:hover {
  color: var(--text);
  background: var(--surface-active);
}

@media (max-width: 900px) {
  .conv-panel {
    display: none;
  }

  /* 会话列表收起后，用抽屉按钮唤起 */
  .conv-toggle {
    display: inline-flex;
  }
}

.drawer-new-btn {
  margin-bottom: 10px;
}

.conv-drawer :deep(.conv-list) {
  max-height: 60vh;
  padding: 0;
}

/* ===== 引用文档抽屉 ===== */
.doc-drawer-content {
  height: 100%;
  overflow-y: auto;
  padding: 20px 24px;
}

.doc-drawer-inner {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.doc-drawer-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.doc-drawer-title-row {
  flex: 1;
  min-width: 0;
}

.doc-drawer-security {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 6px;
}

.security-public {
  background: rgba(103, 194, 58, 0.15);
  color: #67c23a;
}

.security-internal {
  background: rgba(230, 162, 60, 0.15);
  color: #e6a23c;
}

.security-confidential {
  background: rgba(245, 108, 108, 0.15);
  color: #f56c6c;
}

.doc-drawer-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--text);
  margin: 0;
  word-break: break-all;
  line-height: 1.4;
}

.doc-drawer-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--text-faint);
}

.doc-drawer-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.doc-drawer-section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.doc-drawer-count {
  font-size: 11px;
  font-weight: 400;
  color: var(--text-faint);
}

.doc-drawer-summary {
  padding: 12px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  line-height: 1.85;
  color: var(--text-soft);
  background: var(--primary-softer);
  border: 1px solid var(--primary-border);
  word-break: break-word;
}

.chunk-title {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  transition: background 0.2s;
}

.chunk-highlighted {
  background: rgba(34, 211, 238, 0.08);
  border-radius: 6px;
  padding: 2px 4px;
  margin: -2px -4px;
}

.chunk-tag {
  font-size: 11px;
  font-weight: 600;
  color: var(--accent-text);
  background: var(--tint-cyan-bg);
  border: 1px solid var(--tint-cyan-border);
  border-radius: 4px;
  padding: 1px 6px;
}

.chunk-chars {
  font-size: 11px;
  color: var(--text-faint);
}

.chunk-content {
  margin: 0;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.85;
  color: var(--text-soft);
}

/* ===== 分块 Markdown 渲染 ===== */
.md-render {
  word-break: break-word;
}

.md-render :deep(h1),
.md-render :deep(h2),
.md-render :deep(h3),
.md-render :deep(h4) {
  margin: 1em 0 0.5em;
  font-weight: 700;
  color: var(--text);
  line-height: 1.4;
}

.md-render :deep(h1) { font-size: 1.4em; }
.md-render :deep(h2) { font-size: 1.25em; }
.md-render :deep(h3) { font-size: 1.12em; }
.md-render :deep(h4) { font-size: 1em; }

.md-render :deep(p) { margin: 0 0 0.6em; }

.md-render :deep(ul),
.md-render :deep(ol) {
  margin: 0.4em 0;
  padding-left: 1.6em;
}

.md-render :deep(li) { margin: 0.2em 0; }

.md-render :deep(blockquote) {
  margin: 0.6em 0;
  padding: 6px 14px;
  border-left: 3px solid var(--ai-cyan);
  background: var(--tint-cyan-bg);
  color: var(--text-dim);
  border-radius: 0 8px 8px 0;
}

.md-render :deep(code) {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 0.88em;
  padding: 2px 6px;
  border-radius: 5px;
  background: var(--tint-cyan-bg);
  color: var(--accent-text);
  border: 1px solid var(--tint-cyan-border);
}

.md-render :deep(pre) {
  margin: 0.6em 0;
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(8, 14, 28, 0.85);
  border: 1px solid var(--border);
  overflow-x: auto;
}

.md-render :deep(pre code) {
  padding: 0;
  background: none;
  border: none;
  color: #c9d1d9;
  font-size: 12.5px;
  line-height: 1.7;
}

.md-render :deep(a) {
  color: var(--accent-text);
  text-decoration: underline;
  text-underline-offset: 2px;
}

.md-render :deep(a:hover) { color: #7C5CFF; }

.md-render :deep(table) {
  margin: 0.6em 0;
  border-collapse: collapse;
  font-size: 13px;
  width: 100%;
}

.md-render :deep(th),
.md-render :deep(td) {
  padding: 6px 12px;
  border: 1px solid var(--border);
  text-align: left;
}

.md-render :deep(th) {
  background: var(--surface-hover);
  font-weight: 600;
  color: var(--text);
}

.md-render :deep(hr) {
  margin: 0.8em 0;
  border: none;
  border-top: 1px solid var(--border);
}

.md-render :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.md-render :deep(strong) {
  color: var(--text);
  font-weight: 700;
}

.doc-drawer-alert {
  margin-top: 4px;
}

/* ============================================================
   移动端自适应
   ============================================================ */
@media (max-width: 768px) {
  .qa-layout {
    height: calc(100dvh - var(--header-height) - 26px);
    gap: 0;
  }

  .chat-head {
    padding: 10px 12px;
  }

  .chat-sub {
    display: none;
  }

  .chat-head .ai-badge {
    display: none;
  }

  .chat-body {
    padding: 14px 12px;
    gap: 16px;
  }

  .msg-row {
    gap: 8px;
  }

  .msg-bubble {
    max-width: 84%;
    padding: 10px 12px;
  }

  .msg-avatar {
    width: 26px;
    height: 26px;
    border-radius: 8px;
    font-size: 11px;
  }

  .empty-chips {
    display: none;
  }

  .composer {
    padding: 10px 12px 12px;
  }

  .composer-hint {
    display: none;
  }

  .composer-actions {
    justify-content: flex-end;
  }
}
</style>
