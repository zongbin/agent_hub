<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Plus, Promotion, Cpu } from '@element-plus/icons-vue'
import { agentApi } from '@/api/agent'
import type { AgentConversation, AgentStep, PendingAssistantMessage } from '@/api/agent'
import ConversationList from '@/components/ConversationList.vue'
import { useResponsive } from '@/composables/useResponsive'

/** 按步骤序号归组的步骤（前端视图模型，模板直接渲染） */
interface StepGroup {
  stepIndex: number
  stepName: string
  progresses: string[]
  toolName?: string
  toolResult?: string
  expanded: boolean
}

/** 界面上的消息条目 —— 服务端消息 + 实时步骤流的并集 */
interface DisplayMessage {
  id?: number
  role: 'user' | 'assistant'
  content: string
  steps: StepGroup[]
  streaming?: boolean
}

const messages = ref<DisplayMessage[]>([])
const input = ref('')
const loading = ref(false)
const conversations = ref<AgentConversation[]>([])
const currentConversationId = ref<string | null>(null)
const sidebarVisible = ref(true)
const { isMobile } = useResponsive()
const convDrawerVisible = ref(false)

/** 当前流式助手消息的实时状态 —— 步骤按序号归组 */
const pending = ref<PendingAssistantMessage | null>(null)

/** 加载会话列表 */
const loadConversations = async () => {
  try {
    conversations.value = await agentApi.conversations()
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
const openConversation = async (conversation: AgentConversation) => {
  try {
    const list = await agentApi.messages(conversation.id)
    currentConversationId.value = conversation.id
    messages.value = list.map((m) => ({
      id: m.id,
      role: m.role as 'user' | 'assistant',
      content: m.content,
      steps: groupStepsFromHistory(m.steps ?? [])
    }))
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    convDrawerVisible.value = false
  }
}

/** 删除会话 */
const removeConversation = async (conversation: AgentConversation) => {
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
    await agentApi.deleteConversation(conversation.id)
    ElMessage.success('会话已删除')
    if (currentConversationId.value === conversation.id) {
      startNewConversation()
    }
    await loadConversations()
  } catch (error) {
    ElMessage.error((error as Error).message)
  }
}

/** 把 pending 中的步骤按序号归组，便于模板渲染 */
const groupedSteps = (): StepGroup[] => {
  if (!pending.value) return []
  const groups = new Map<number, StepGroup>()
  const allSteps = Array.from(pending.value.stepsByIndex.values()).reduce<AgentStep[]>(
      (acc, list) => acc.concat(list), [])
  for (const step of allSteps) {
    let group = groups.get(step.stepIndex)
    if (!group) {
      group = { stepIndex: step.stepIndex, stepName: step.stepName ?? '', progresses: [], expanded: false }
      groups.set(step.stepIndex, group)
    }
    if (step.message) group.progresses.push(step.message)
    if (step.toolName) group.toolName = step.toolName
    if (step.toolResult) group.toolResult = step.toolResult
  }
  return Array.from(groups.values()).sort((a, b) => a.stepIndex - b.stepIndex)
}

/** 历史消息中的步骤（AgentStep[]）→ 视图模型（StepGroup[]） */
const groupStepsFromHistory = (steps: AgentStep[]): StepGroup[] => {
  const groups = new Map<number, StepGroup>()
  for (const step of steps) {
    let group = groups.get(step.stepIndex)
    if (!group) {
      group = { stepIndex: step.stepIndex, stepName: step.stepName ?? '', progresses: [], expanded: false }
      groups.set(step.stepIndex, group)
    }
    if (step.message) group.progresses.push(step.message)
    if (step.toolName) group.toolName = step.toolName
    if (step.toolResult) group.toolResult = step.toolResult
  }
  return Array.from(groups.values()).sort((a, b) => a.stepIndex - b.stepIndex)
}

const sendMessage = async () => {
  const task = input.value.trim()
  if (!task || loading.value) return

  messages.value.push({ role: 'user', content: task, steps: [] })
  input.value = ''
  loading.value = true

  pending.value = { stepsByIndex: new Map(), answer: '', streaming: true }
  messages.value.push({ role: 'assistant', content: '', steps: [], streaming: true })
  const assistant = messages.value[messages.value.length - 1]
  await nextTick()

  try {
    await agentApi.chat(task, currentConversationId.value, {
      onStep: (step) => {
        // 按序号归组
        const list = pending.value!.stepsByIndex.get(step.stepIndex) ?? []
        list.push(step)
        pending.value!.stepsByIndex.set(step.stepIndex, list)
        // 同步到显示消息
        assistant.steps = groupedSteps()
      },
      onComplete: (answer) => {
        if (answer.conversationId) currentConversationId.value = answer.conversationId
        assistant.content = answer.answer || pending.value!.answer
        assistant.id = answer.messageId
        assistant.streaming = false
        pending.value = null
        loadConversations()
      },
      onError: (message) => {
        assistant.content = assistant.content || `执行失败：${message}`
        assistant.streaming = false
        pending.value = null
      }
    })
  } catch (error) {
    assistant.streaming = false
    assistant.content = assistant.content || (error as Error).message
    pending.value = null
    ElMessage.error((error as Error).message)
  } finally {
    assistant.streaming = false
    loading.value = false
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
          新建任务
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
          智能体已就绪
        </span>
      </div>
    </aside>

    <!-- 移动端历史会话抽屉 -->
    <el-drawer
      v-model="convDrawerVisible"
      title="历史任务"
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
        新建任务
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
            aria-label="打开历史任务"
            @click="convDrawerVisible = true"
          >
            <el-icon :size="18"><Clock /></el-icon>
          </button>
          <el-icon :size="18" class="chat-icon"><Cpu /></el-icon>
          <div>
            <div class="chat-title">超级智能体</div>
            <div class="chat-sub">ReAct · 自主拆解 · 工具调用 · 步骤可视化</div>
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
            <el-icon :size="28"><Cpu /></el-icon>
          </div>
          <h3 class="empty-title">交给智能体一项任务</h3>
          <p class="empty-desc">
            描述复杂任务，智能体会自主拆解、调用工具、逐步推进，并把每一步的过程展示给你。
          </p>
          <div class="empty-chips">
            <span class="empty-chip">搜索今年 AI 行业的主要进展并总结</span>
            <span class="empty-chip">对比竞品 X 与我方产品的功能差异</span>
            <span class="empty-chip">下载这份报告并转成 PDF</span>
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
            <el-icon :size="14"><Cpu /></el-icon>
          </div>

          <div class="msg-bubble" :class="m.role === 'user' ? 'bubble-user' : 'bubble-ai'">
            <pre class="msg-text">{{ m.content }}<span v-if="m.streaming" class="caret">▍</span></pre>

            <!-- 流式等待指示 -->
            <div v-if="m.streaming && !m.content" class="thinking">
              <span></span><span></span><span></span>
            </div>

            <!-- 步骤时间线 -->
            <div v-if="m.role === 'assistant' && m.steps.length" class="steps">
              <div class="steps-head">
                <span class="steps-dot"></span>
                执行步骤
              </div>
              <div class="steps-list">
                <div
                  v-for="g in m.steps"
                  :key="g.stepIndex"
                  class="step-item"
                >
                  <div class="step-summary" @click="g.expanded = !g.expanded">
                    <span class="step-index">{{ g.stepIndex }}</span>
                    <span class="step-name">{{ g.stepName }}</span>
                    <el-icon class="step-toggle" :size="12">
                      <component :is="g.expanded ? 'ArrowUp' : 'ArrowDown'" />
                    </el-icon>
                  </div>
                  <div v-if="g.expanded" class="step-detail">
                    <div v-for="(p, pi) in g.progresses" :key="pi" class="step-progress">{{ p }}</div>
                    <div v-if="g.toolName" class="step-tool">
                      <span class="step-tool-label">工具：</span>{{ g.toolName }}
                    </div>
                    <div v-if="g.toolResult" class="step-result">{{ g.toolResult }}</div>
                  </div>
                </div>
              </div>
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
            placeholder="描述一项任务... (Enter 发送)"
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
/* ===== 复用 QAM 页的全部布局类名与 CSS 变量 ===== */
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

/* ===== 步骤时间线 ===== */
.steps {
  margin-top: 13px;
  padding-top: 11px;
  border-top: 1px dashed var(--border);
}

.steps-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.1em;
  color: var(--text-faint);
  margin-bottom: 8px;
}

.steps-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--ai-violet);
  box-shadow: 0 0 8px var(--ai-violet);
}

.steps-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.step-item {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface-soft);
  overflow: hidden;
}

.step-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 11px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;
}

.step-summary:hover {
  background: var(--surface-hover);
}

.step-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 5px;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: var(--ai-violet);
  flex-shrink: 0;
}

.step-name {
  flex: 1;
  font-size: 12px;
  color: var(--text-soft);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.step-toggle {
  color: var(--text-faint);
  flex-shrink: 0;
}

.step-detail {
  padding: 9px 11px 11px;
  border-top: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.step-progress {
  font-size: 12px;
  color: var(--text-dim);
  line-height: 1.7;
}

.step-tool {
  font-size: 11px;
  color: var(--text-faint);
  padding: 4px 8px;
  border-radius: 6px;
  background: var(--tint-cyan-bg);
  border: 1px solid var(--tint-cyan-border);
}

.step-tool-label {
  font-weight: 600;
}

.step-result {
  font-size: 12px;
  color: var(--text-soft);
  line-height: 1.7;
  padding: 6px 9px;
  border-radius: 6px;
  background: var(--glass-weak);
  border: 1px solid var(--border);
  max-height: 200px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-word;
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
}

.mr-1 {
  margin-right: 4px;
}

/* ===== 移动端 ===== */
.conv-toggle {
  display: none;
  background: none;
  border: none;
  color: var(--text-soft);
  cursor: pointer;
}

@media (max-width: 768px) {
  .conv-panel {
    display: none;
  }

  .conv-toggle {
    display: block;
  }
}

.drawer-new-btn {
  width: 100%;
  margin: 12px;
  margin-top: 0;
}

/* 动画关键帧（与全局 styles/index.css 同名复用） */
@keyframes rise {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

@keyframes caret {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

@keyframes pulseGlow {
  0%, 100% { opacity: 0.4; transform: scale(0.85); }
  50% { opacity: 1; transform: scale(1.15); }
}
</style>
