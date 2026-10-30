<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Plus, Promotion, ChatLineRound } from '@element-plus/icons-vue'
import { qaApi } from '@/api/qa'
import type { Conversation, QaSource } from '@/api/qa'
import ConversationList from '@/components/ConversationList.vue'
import { useResponsive } from '@/composables/useResponsive'

interface ChatMessage {
  id?: number
  role: 'user' | 'assistant'
  content: string
  sources?: QaSource[]
  feedback?: 'up' | 'down'
  streaming?: boolean
}

const router = useRouter()
const messages = ref<ChatMessage[]>([])
const input = ref('')
const loading = ref(false)
const conversations = ref<Conversation[]>([])
const currentConversationId = ref<string | null>(null)
const sidebarVisible = ref(true)

const { isMobile } = useResponsive()

/** 移动端历史会话抽屉 */
const convDrawerVisible = ref(false)

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
          // 回答内容与引用来源一起到达，一次性渲染
          if (answer.conversationId) {
            currentConversationId.value = answer.conversationId
          }
          assistant.content = answer.answer || assistant.content
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
  const path = source.documentUrl || (source.documentId ? `/knowledge/${source.documentId}` : '')
  if (path) {
    router.push(path)
  }
}

onMounted(() => {
  loadConversations()
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
            <pre class="msg-text">{{ m.content }}<span v-if="m.streaming" class="caret">▍</span></pre>

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
                <div
                  v-for="(s, si) in m.sources"
                  :key="si"
                  class="source-chip"
                  @click="openDocument(s)"
                >
                  <span class="source-index">{{ si + 1 }}</span>
                  {{ s.title }}<template v-if="s.chunkIndex != null"> · 第 {{ s.chunkIndex + 1 }} 段</template>
                </div>
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
  padding: 5px 9px;
  border-radius: 8px;
  background: var(--tint-cyan-bg);
  border: 1px solid var(--tint-cyan-border);
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, transform 0.2s;
}

.source-chip:hover {
  background: rgba(34, 211, 238, 0.14);
  border-color: var(--ai-cyan);
  transform: translateX(2px);
}

.source-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 15px;
  height: 15px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 700;
  color: #062024;
  background: var(--ai-cyan);
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
