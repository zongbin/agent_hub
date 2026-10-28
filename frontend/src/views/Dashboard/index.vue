<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

/** 统计数据 */
const stats = [
  { label: '知识库文档', value: '—', trend: '', color: '#4F46E5' },
  { label: '问答次数', value: '—', trend: '', color: '#06B6D4' },
  { label: 'Agent 任务', value: '—', trend: '', color: '#10B981' },
  { label: '今日活跃', value: '—', trend: '', color: '#F59E0B' }
]
</script>

<template>
  <div class="dashboard">
    <!-- 欢迎区域 -->
    <div class="welcome-card">
      <div class="welcome-text">
        <h2>欢迎回来，{{ authStore.user?.realName || authStore.user?.username }} 👋</h2>
        <p>智知 AgentHub 已就绪，开始探索您的企业知识库</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div v-for="stat in stats" :key="stat.label" class="stat-card">
        <div class="stat-label">{{ stat.label }}</div>
        <div class="stat-value" :style="{ color: stat.color }">{{ stat.value }}</div>
        <div class="stat-trend" v-if="stat.trend">{{ stat.trend }}</div>
      </div>
    </div>

    <!-- 快速入口 -->
    <div class="quick-section">
      <h3 class="section-title">快速开始</h3>
      <div class="quick-grid">
        <div class="quick-card">
          <div class="quick-icon" style="background: #EEF2FF; color: #4F46E5">
            📄
          </div>
          <div class="quick-info">
            <div class="quick-title">上传文档</div>
            <div class="quick-desc">将企业文档上传至知识库</div>
          </div>
        </div>
        <div class="quick-card">
          <div class="quick-icon" style="background: #E0F2FE; color: #06B6D4">
            💬
          </div>
          <div class="quick-info">
            <div class="quick-title">智能问答</div>
            <div class="quick-desc">用自然语言查询知识库</div>
          </div>
        </div>
        <div class="quick-card">
          <div class="quick-icon" style="background: #ECFDF5; color: #10B981">
            🤖
          </div>
          <div class="quick-info">
            <div class="quick-title">超级智能体</div>
            <div class="quick-desc">让 AI 自主完成复杂任务</div>
          </div>
        </div>
        <div class="quick-card">
          <div class="quick-icon" style="background: #FFFBEB; color: #F59E0B">
            🔍
          </div>
          <div class="quick-info">
            <div class="quick-title">知识搜索</div>
            <div class="quick-desc">语义检索散落文档</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1000px;
}

/* ===== 欢迎卡片 ===== */
.welcome-card {
  background: linear-gradient(135deg, #4F46E5 0%, #06B6D4 100%);
  border-radius: 12px;
  padding: 28px 32px;
  margin-bottom: 20px;
}

.welcome-text h2 {
  font-size: 20px;
  font-weight: 700;
  color: white;
  margin: 0 0 6px;
}

.welcome-text p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

/* ===== 统计卡片 ===== */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 16px;
}

.stat-label {
  font-size: 12px;
  color: #64748B;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.stat-trend {
  font-size: 11px;
  color: #94A3B8;
  margin-top: 4px;
}

/* ===== 快速入口 ===== */
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #64748B;
  margin: 0 0 12px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  background: white;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-card:hover {
  border-color: #4F46E5;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.1);
  transform: translateY(-1px);
}

.quick-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.quick-title {
  font-size: 14px;
  font-weight: 600;
  color: #0F172A;
  margin-bottom: 2px;
}

.quick-desc {
  font-size: 12px;
  color: #94A3B8;
}
</style>
