<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Reading, ChatLineRound, Cpu, Search } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const router = useRouter()

/** 统计数据 */
const stats = [
  { label: '知识库文档', value: '—', trend: '向量索引实时同步', icon: Reading, color: '#7C5CFF' },
  { label: '问答次数', value: '—', trend: 'RAG 检索增强', icon: ChatLineRound, color: '#22D3EE' },
  { label: 'Agent 任务', value: '—', trend: '计划先行的执行范式', icon: Cpu, color: '#F472B6' },
  { label: '今日活跃', value: '—', trend: '全量操作可审计', icon: Search, color: '#A3E635' }
]

/** 快速入口 */
const quickActions = [
  {
    title: '上传文档',
    desc: '将企业文档上传至知识库',
    path: '/knowledge/upload',
    icon: Reading,
    color: '#7C5CFF'
  },
  {
    title: '智能问答',
    desc: '用自然语言查询知识库',
    path: '/qa',
    icon: ChatLineRound,
    color: '#22D3EE'
  },
  {
    title: '超级智能体',
    desc: '让 AI 自主完成复杂任务',
    path: '/agent',
    icon: Cpu,
    color: '#F472B6'
  },
  {
    title: '知识搜索',
    desc: '语义检索散落文档',
    path: '/knowledge',
    icon: Search,
    color: '#A3E635'
  }
]
</script>

<template>
  <div class="dashboard">
    <!-- 欢迎区域 -->
    <section class="hero-card glass-card glow-strip">
      <div class="hero-glow"></div>
      <div class="hero-content">
        <div class="hero-eyebrow">
          <span class="ai-dot"></span>
          AGENT RUNTIME · 已就绪
        </div>
        <h2 class="hero-title">
          欢迎回来，{{ authStore.user?.realName || authStore.user?.username }}
        </h2>
        <p class="hero-desc">
          智知 AgentHub 已就绪。文档已向量化入库，随时可以开始提问或拆解复杂任务。
        </p>
        <div class="hero-actions">
          <el-button type="primary" @click="router.push('/qa')">
            <el-icon class="mr-1"><ChatLineRound /></el-icon>
            开始提问
          </el-button>
          <el-button class="ghost-btn" @click="router.push('/knowledge/upload')">
            <el-icon class="mr-1"><Reading /></el-icon>
            上传文档
          </el-button>
        </div>
      </div>
      <!-- 装饰环 -->
      <div class="hero-rings">
        <span class="ring ring-1"></span>
        <span class="ring ring-2"></span>
        <span class="ring ring-3"></span>
        <span class="ring-core"></span>
      </div>
    </section>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div v-for="stat in stats" :key="stat.label" class="stat-card glass-card glass-card-hover">
        <span class="stat-accent" :style="{ background: stat.color }"></span>
        <div class="stat-head">
          <div class="stat-icon" :style="{ color: stat.color, background: `${stat.color}1A` }">
            <el-icon :size="17"><component :is="stat.icon" /></el-icon>
          </div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
        <div class="stat-value" :style="{ color: stat.color }">{{ stat.value }}</div>
        <div class="stat-trend">{{ stat.trend }}</div>
      </div>
    </div>

    <!-- 快速入口 -->
    <section class="quick-section">
      <h3 class="section-label">快速开始</h3>
      <div class="quick-grid">
        <div
          v-for="action in quickActions"
          :key="action.title"
          class="quick-card glass-card glass-card-hover"
          @click="router.push(action.path)"
        >
          <div class="quick-icon" :style="{ color: action.color, background: `${action.color}1A` }">
            <el-icon :size="20"><component :is="action.icon" /></el-icon>
          </div>
          <div class="quick-info">
            <div class="quick-title">{{ action.title }}</div>
            <div class="quick-desc">{{ action.desc }}</div>
          </div>
          <span class="quick-arrow">→</span>
        </div>
      </div>
    </section>

    <!-- 能力说明 -->
    <section class="capability-strip">
      <div class="cap-item">
        <span class="cap-dot" style="background: #7C5CFF"></span>
        <div>
          <div class="cap-title">计划先行</div>
          <div class="cap-desc">执行前先产出可审阅的计划</div>
        </div>
      </div>
      <div class="cap-item">
        <span class="cap-dot" style="background: #22D3EE"></span>
        <div>
          <div class="cap-title">人在回路</div>
          <div class="cap-desc">写操作暂停等待人工确认</div>
        </div>
      </div>
      <div class="cap-item">
        <span class="cap-dot" style="background: #A3E635"></span>
        <div>
          <div class="cap-title">权限内检索</div>
          <div class="cap-desc">向量召回与密级过滤同一次查询</div>
        </div>
      </div>
      <div class="cap-item">
        <span class="cap-dot" style="background: #F472B6"></span>
        <div>
          <div class="cap-title">全程可审计</div>
          <div class="cap-desc">每次调用与拒绝均留痕</div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1180px;
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
}

/* ===== 欢迎卡片 ===== */
.hero-card {
  position: relative;
  overflow: hidden;
  padding: 30px 34px;
  margin-bottom: 20px;
  background:
    linear-gradient(120deg, rgba(124, 92, 255, 0.18) 0%, rgba(34, 211, 238, 0.1) 55%, rgba(163, 230, 53, 0.06) 100%),
    var(--veil);
}

.hero-glow {
  position: absolute;
  top: -120px;
  right: -80px;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(124, 92, 255, 0.4), transparent 65%);
  filter: blur(30px);
  animation: aurora 12s ease-in-out infinite;
  pointer-events: none;
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 560px;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.18em;
  color: var(--text-faint);
  margin-bottom: 14px;
}

.hero-title {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text);
  margin: 0 0 10px;
}

.hero-desc {
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-dim);
  margin: 0 0 22px;
}

.hero-actions {
  display: flex;
  gap: 10px;
}

.ghost-btn {
  background: var(--surface-hover);
  border-color: var(--border-strong);
  color: var(--text-soft);
}

.ghost-btn:hover {
  background: var(--surface-active);
  border-color: var(--ai-cyan);
  color: var(--text);
}

/* 装饰环 */
.hero-rings {
  position: absolute;
  right: 70px;
  top: 50%;
  transform: translateY(-50%);
  width: 180px;
  height: 180px;
  pointer-events: none;
}

.ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1px solid rgba(34, 211, 238, 0.22);
}

.ring-1 { animation: spin-slow 18s linear infinite; border-top-color: rgba(34, 211, 238, 0.85); }
.ring-2 { inset: 26px; animation: spin-slow 13s linear infinite reverse; border-left-color: rgba(124, 92, 255, 0.85); }
.ring-3 { inset: 52px; animation: spin-slow 9s linear infinite; border-bottom-color: rgba(163, 230, 53, 0.8); }

.ring-core {
  position: absolute;
  inset: 76px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(124, 92, 255, 0.9), rgba(34, 211, 238, 0.5));
  box-shadow: 0 0 34px rgba(124, 92, 255, 0.85);
  animation: pulseGlow 3.4s ease-in-out infinite;
}

/* ===== 统计卡片 ===== */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 26px;
}

.stat-card {
  position: relative;
  padding: 18px 18px 16px;
  overflow: hidden;
}

.stat-accent {
  position: absolute;
  left: 0;
  top: 16px;
  bottom: 16px;
  width: 2px;
  border-radius: 0 2px 2px 0;
  box-shadow: 0 0 12px currentColor;
}

.stat-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.stat-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-label {
  font-size: 12px;
  color: var(--text-dim);
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.1;
}

.stat-trend {
  font-size: 11px;
  color: var(--text-faint);
  margin-top: 6px;
}

/* ===== 快速入口 ===== */
.section-label {
  margin: 0 0 14px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  margin-bottom: 26px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  cursor: pointer;
}

.quick-icon {
  width: 46px;
  height: 46px;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.quick-info {
  flex: 1;
  min-width: 0;
}

.quick-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 3px;
}

.quick-desc {
  font-size: 12px;
  color: var(--text-faint);
}

.quick-arrow {
  font-size: 15px;
  color: var(--text-faint);
  transition: transform 0.25s, color 0.25s;
}

.quick-card:hover .quick-arrow {
  transform: translateX(4px);
  color: var(--accent-text);
}

/* ===== 能力说明 ===== */
.capability-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  padding: 18px 20px;
  border-radius: var(--radius-lg);
  background: var(--glass-weak);
  border: 1px solid var(--border);
}

.cap-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.cap-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
  box-shadow: 0 0 10px currentColor;
}

.cap-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-soft);
  margin-bottom: 3px;
}

.cap-desc {
  font-size: 11px;
  line-height: 1.6;
  color: var(--text-faint);
}

@media (max-width: 1000px) {
  .stats-row,
  .capability-strip {
    grid-template-columns: repeat(2, 1fr);
  }
  .hero-rings {
    display: none;
  }
}

@media (max-width: 700px) {
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
