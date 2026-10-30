<script setup lang="ts">
import { Delete } from '@element-plus/icons-vue'
import type { Conversation } from '@/api/qa'

defineProps<{
  /** 历史会话列表 */
  conversations: Conversation[]
  /** 当前选中的会话 ID */
  currentId: string | null
}>()

const emit = defineEmits<{
  (e: 'select', conversation: Conversation): void
  (e: 'remove', conversation: Conversation): void
}>()

/** 格式化时间 */
const formatTime = (value: string) => {
  const date = new Date(value)
  return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(
    date.getMinutes()
  ).padStart(2, '0')}`
}
</script>

<template>
  <div class="conv-list">
    <el-empty v-if="conversations.length === 0" description="暂无历史对话" :image-size="60" />
    <div
      v-for="c in conversations"
      :key="c.id"
      class="conv-item"
      :class="{ active: currentId === c.id }"
      @click="emit('select', c)"
    >
      <div class="conv-meta">
        <div class="conv-title">{{ c.title }}</div>
        <div class="conv-time">{{ formatTime(c.updatedAt) }}</div>
      </div>
      <el-button
        size="small"
        text
        class="conv-del"
        @click.stop="emit('remove', c)"
      >
        <el-icon><Delete /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 0;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 11px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.2s, box-shadow 0.2s;
}

.conv-item:hover {
  background: var(--surface-hover);
}

.conv-item.active {
  background: linear-gradient(90deg, rgba(124, 92, 255, 0.22), rgba(34, 211, 238, 0.08));
  box-shadow: inset 0 0 0 1px rgba(124, 92, 255, 0.3);
}

.conv-meta {
  flex: 1;
  min-width: 0;
}

.conv-title {
  font-size: 13px;
  color: var(--text-soft);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-item.active .conv-title {
  color: #fff;
}

.conv-time {
  font-size: 10px;
  color: var(--text-faint);
  margin-top: 2px;
}

.conv-del {
  color: var(--text-faint);
  opacity: 0;
  transition: opacity 0.2s, color 0.2s;
}

.conv-item:hover .conv-del {
  opacity: 1;
}

.conv-del:hover {
  color: var(--ai-pink);
}
</style>
