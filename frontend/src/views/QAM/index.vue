<script setup lang="ts">
import { ref } from 'vue';

interface Message {
  role: 'user' | 'assistant';
  content: string;
}

const messages = ref<Message[]>([]);
const input = ref('');
const loading = ref(false);

const sendMessage = async () => {
  if (!input.value.trim()) return;
  messages.value.push({ role: 'user', content: input.value });
  input.value = '';
  loading.value = true;
  try {
    // TODO: 对接后端问答接口（SSE）
    messages.value.push({
      role: 'assistant',
      content: '这是一个占位回复。对接后端后将展示真实答案。',
    });
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="flex flex-col h-[calc(100vh-120px)]">
    <div class="flex-1 overflow-y-auto p-4 space-y-4">
      <el-empty v-if="messages.length === 0" description="开始提问吧" />
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
          <pre class="whitespace-pre-wrap font-sans">{{ m.content }}</pre>
        </el-card>
      </div>
    </div>

    <div class="border-t p-4 bg-white">
      <el-input
        v-model="input"
        type="textarea"
        :rows="3"
        placeholder="输入你的问题... (Enter 发送)"
        @keydown.enter.exact="sendMessage"
      >
        <template #append>
          <el-button type="primary" :loading="loading" @click="sendMessage">
            发送
          </el-button>
        </template>
      </el-input>
    </div>
  </div>
</template>
