<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import { qaApi } from '@/api/qa'
import type { Conversation, QaSource } from '@/api/qa'

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
const currentConversationId = ref<number | null>(null)
const sidebarVisible = ref(true)

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
        onSource: (conversationId, sources) => {
          currentConversationId.value = conversationId
          assistant.sources = sources
        },
        onDelta: (content) => {
          assistant.content += content
        },
        onDone: (_conversationId, messageId) => {
          assistant.id = messageId
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

const openDocument = (documentId: number) => {
  if (documentId) {
    router.push(`/knowledge/${documentId}`)
  }
}

/** 格式化时间 */
const formatTime = (value: string) => {
  const date = new Date(value)
  return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(
    date.getMinutes()
  ).padStart(2, '0')}`
}

onMounted(() => {
  loadConversations()
})
</script>

<template>
  <div class="flex h-[calc(100vh-120px)]">
    <!-- 会话列表 -->
    <div
      v-show="sidebarVisible"
      class="w-64 flex-shrink-0 border-r bg-white flex flex-col"
    >
      <div class="p-3 border-b">
        <el-button type="primary" class="w-full" @click="startNewConversation">
          <el-icon><Plus /></el-icon>
          新建对话
        </el-button>
      </div>
      <div class="flex-1 overflow-y-auto p-2 space-y-1">
        <el-empty v-if="conversations.length === 0" description="暂无历史对话" :image-size="60" />
        <div
          v-for="c in conversations"
          :key="c.id"
          :class="[
            'group px-3 py-2 rounded cursor-pointer text-sm flex items-center justify-between gap-2',
            currentConversationId === c.id ? 'bg-blue-50 text-blue-600' : 'hover:bg-gray-50'
          ]"
          @click="openConversation(c)"
        >
          <div class="flex-1 min-w-0">
            <div class="truncate">{{ c.title }}</div>
            <div class="text-xs text-gray-400">{{ formatTime(c.updatedAt) }}</div>
          </div>
          <el-button
            size="small"
            text
            class="opacity-0 group-hover:opacity-100"
            @click.stop="removeConversation(c)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 聊天区 -->
    <div class="flex-1 flex flex-col min-w-0">
      <div class="flex-1 overflow-y-auto p-4 space-y-4">
        <el-empty
          v-if="messages.length === 0"
          description="开始提问吧"
        />
        <div
          v-for="(m, i) in messages"
          :key="i"
          :class="['flex', m.role === 'user' ? 'justify-end' : 'justify-start']"
        >
          <el-card
            :class="[
              'max-w-[70%]',
              m.role === 'user' ? 'bg-blue-50' : 'bg-white',
            ]"
          >
            <pre class="whitespace-pre-wrap font-sans">{{ m.content }}<span v-if="m.streaming">▍</span></pre>

            <!-- 引用来源 -->
            <div v-if="m.role === 'assistant' && m.sources && m.sources.length" class="mt-3 border-t pt-2">
              <div class="text-xs text-gray-500 mb-1">引用来源</div>
              <div class="space-y-1">
                <div
                  v-for="(s, si) in m.sources"
                  :key="si"
                  class="text-xs text-blue-600 cursor-pointer hover:underline"
                  @click="openDocument(s.documentId)"
                >
                  [{{ si + 1 }}] {{ s.title }} · 第 {{ s.chunkIndex + 1 }} 段
                </div>
              </div>
            </div>

            <!-- 反馈 -->
            <div
              v-if="m.role === 'assistant' && !m.streaming && m.content"
              class="mt-3 flex gap-2 items-center"
            >
              <span class="text-xs text-gray-400">这个回答有帮助吗？</span>
              <el-button
                size="small"
                :type="m.feedback === 'up' ? 'primary' : 'default'"
                :disabled="m.feedback === 'up'"
                @click="submitFeedback(m, true)"
              >
                👍 有帮助
              </el-button>
              <el-button
                size="small"
                :type="m.feedback === 'down' ? 'danger' : 'default'"
                :disabled="m.feedback === 'down'"
                @click="submitFeedback(m, false)"
              >
                👎 无帮助
              </el-button>
            </div>
          </el-card>
        </div>
      </div>

      <div class="border-t p-4 bg-white">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          placeholder="输入你的问题... (Enter 发送)"
          @keydown.enter.exact.prevent="sendMessage"
        >
          <template #append>
            <el-button type="primary" :loading="loading" @click="sendMessage">
              发送
            </el-button>
          </template>
        </el-input>
      </div>
    </div>
  </div>
</template>
