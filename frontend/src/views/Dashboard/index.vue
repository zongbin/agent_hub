<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Reading,
  ChatLineRound,
  Cpu,
  Search,
  Document as DocumentIcon,
  TrendCharts
} from '@element-plus/icons-vue'
import { dashboardApi } from '@/api/dashboard'
import type {
  DashboardStatsVO,
  RecentConversationVO,
  RecentDocumentVO
} from '@/api/dashboard'
import { useAuthStore } from '@/stores/auth'
import { canManageDocuments } from '@/utils/permission'

const authStore = useAuthStore()
const router = useRouter()

/** 是否可上传文档（查看者与访客为只读角色） */
const canManage = computed(() => canManageDocuments(authStore.user?.role))

/** 加载中 */
const loading = ref(true)

/** 工作台统计数据 */
const stats = ref<DashboardStatsVO | null>(null)

/** 加载工作台数据 */
const loadStats = async () => {
  loading.value = true
  try {
    stats.value = await dashboardApi.stats()
  } catch (error: unknown) {
    const err = error as { message?: string }
    ElMessage.error(err.message || '加载工作台数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)

// ==================== 格式化辅助 ====================

/** 文件大小格式化 */
const formatSize = (bytes: number): string => {
  if (bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let n = bytes
  while (n >= 1024 && i < units.length - 1) {
    n /= 1024
    i++
  }
  return `${n.toFixed(i === 0 ? 0 : 1)} ${units[i]}`
}

/** 数字千分位 */
const formatNum = (n: number): string => {
  return new Intl.NumberFormat('zh-CN').format(n)
}

/** 相对时间 */
const formatTime = (iso: string): string => {
  const t = new Date(iso)
  const diff = Date.now() - t.getTime()
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} 分钟前`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} 小时前`
  return t.toLocaleDateString('zh-CN')
}

/** 密级标签 */
const securityLabel = (level: string): string => {
  const map: Record<string, string> = { PUBLIC: '公开', INTERNAL: '内部', CONFIDENTIAL: '机密' }
  return map[level] ?? level
}

/** 状态标签与主题 */
const statusMeta = (status: string): { label: string; type: 'success' | 'warning' | 'danger' | 'info' } => {
  const map: Record<string, { label: string; type: 'success' | 'warning' | 'danger' | 'info' }> = {
    ACTIVE: { label: '已就绪', type: 'success' },
    PROCESSING: { label: '处理中', type: 'warning' },
    FAILED: { label: '失败', type: 'danger' },
    DELETED: { label: '已删除', type: 'info' }
  }
  return map[status] ?? { label: status, type: 'info' }
}

/** 会话类型标签 */
const convTypeLabel = (type: string): string => type === 'QA' ? '问答' : '智能体'

// ==================== 统计卡片 ====================

interface StatCard {
  label: string
  value: string
  trend: string
  icon: unknown
  color: string
}

const statCards = computed<StatCard[]>(() => {
  const s = stats.value
  if (!s) {
    return [
      { label: '知识库文档', value: '—', trend: '向量索引实时同步', icon: Reading, color: '#7C5CFF' },
      { label: '问答次数', value: '—', trend: 'RAG 检索增强', icon: ChatLineRound, color: '#22D3EE' },
      { label: 'Agent 任务', value: '—', trend: '计划先行的执行范式', icon: Cpu, color: '#F472B6' },
      { label: '今日操作', value: '—', trend: '全量操作可审计', icon: Search, color: '#A3E635' }
    ]
  }
  return [
    {
      label: '知识库文档',
      value: formatNum(s.document.total),
      trend: `${formatNum(s.document.chunkTotal)} 个向量分块 · ${formatSize(s.document.sizeBytes)}`,
      icon: Reading,
      color: '#7C5CFF'
    },
    {
      label: '问答次数',
      value: formatNum(s.qa.questionTotal),
      trend: `今日 ${formatNum(s.qa.todayQuestionCount)} 次 · ${formatNum(s.qa.conversationTotal)} 个会话`,
      icon: ChatLineRound,
      color: '#22D3EE'
    },
    {
      label: 'Agent 任务',
      value: formatNum(s.agent.taskTotal),
      trend: `今日 ${formatNum(s.agent.todayTaskCount)} 次 · ${formatNum(s.agent.conversationTotal)} 个会话`,
      icon: Cpu,
      color: '#F472B6'
    },
    {
      label: '今日操作',
      value: formatNum(s.activity.todayActions),
      trend: `近 7 日 ${formatNum(s.activity.weekActions)} 次 · 累计 ${formatNum(s.activity.totalActions)}`,
      icon: Search,
      color: '#A3E635'
    }
  ]
})

// ==================== 快速入口 ====================

const quickActions = computed(() =>
  [
    {
      title: '上传文档',
      desc: '将企业文档上传至知识库',
      path: '/knowledge/upload',
      icon: Reading,
      color: '#7C5CFF',
      writeRequired: true
    },
    {
      title: '智能问答',
      desc: '用自然语言查询知识库',
      path: '/qa',
      icon: ChatLineRound,
      color: '#22D3EE',
      writeRequired: false
    },
    {
      title: '超级智能体',
      desc: '让 AI 自主完成复杂任务',
      path: '/agent',
      icon: Cpu,
      color: '#F472B6',
      writeRequired: false
    },
    {
      title: '知识搜索',
      desc: '语义检索散落文档',
      path: '/knowledge',
      icon: Search,
      color: '#A3E635',
      writeRequired: false
    }
  ].filter((item) => !item.writeRequired || canManage.value)
)

// ==================== 最近文档 ====================

const recentDocs = computed<RecentDocumentVO[]>(() => stats.value?.recentDocuments ?? [])

/** 跳转文档详情 */
const goDoc = (row: RecentDocumentVO) => {
  router.push(`/knowledge/${row.id}`)
}

// ==================== 最近会话 ====================

const recentConvs = computed<RecentConversationVO[]>(() => stats.value?.recentConversations ?? [])

/** 跳转会话 */
const goConv = (row: RecentConversationVO) => {
  if (row.type === 'QA') {
    router.push({ path: '/qa', query: { conversationId: row.id } })
  } else {
    router.push({ path: '/agent', query: { conversationId: row.id } })
  }
}

// ==================== 密级分布 ====================

const levelDist = computed(() => stats.value?.levelDistribution ?? [])

// ==================== 趋势（简单柱状图） ====================

const trendPoints = computed(() => stats.value?.trend ?? [])

const maxTrendVal = computed(() => {
  let max = 1
  for (const p of trendPoints.value) {
    max = Math.max(max, p.qaCount + p.agentCount)
  }
  return max
})

/** 趋势条高度百分比 */
const trendHeight = (val: number): string => {
  const pct = (val / maxTrendVal.value) * 100
  return pct < 4 ? '4%' : `${pct}%`
}
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
          <el-button v-if="canManage" class="ghost-btn" @click="router.push('/knowledge/upload')">
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
      <div v-for="stat in statCards" :key="stat.label" class="stat-card glass-card glass-card-hover">
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

    <!-- 中间：最近上传 + 最近会话 -->
    <div class="mid-row">
      <!-- 最近上传 -->
      <section class="mid-card glass-card">
        <div class="mid-head">
          <h3 class="section-label">最近上传</h3>
          <span class="mid-meta">{{ stats?.document.total ?? 0 }} 篇文档</span>
        </div>
        <el-skeleton v-if="loading" :rows="5" animated />
        <div v-else-if="recentDocs.length === 0" class="empty-block">
          <el-empty description="暂无文档" :image-size="64">
            <el-button v-if="canManage" type="primary" link @click="router.push('/knowledge/upload')">
              去上传
            </el-button>
          </el-empty>
        </div>
        <div v-else class="doc-list">
          <div v-for="doc in recentDocs" :key="doc.id" class="doc-item" @click="goDoc(doc)">
            <div class="doc-icon-wrap">
              <el-icon :size="18" class="doc-icon"><DocumentIcon /></el-icon>
            </div>
            <div class="doc-info">
              <div class="doc-title" :title="doc.title">{{ doc.title }}</div>
              <div class="doc-meta">
                <el-tag size="small" :type="statusMeta(doc.status).type" effect="light">
                  {{ statusMeta(doc.status).label }}
                </el-tag>
                <span class="doc-meta-text">{{ doc.fileType.toUpperCase() }}</span>
                <span class="doc-meta-text">{{ formatSize(doc.fileSize) }}</span>
                <span class="doc-meta-text">{{ doc.chunkCount }} 分块</span>
              </div>
            </div>
            <span class="doc-time">{{ formatTime(doc.createdAt) }}</span>
          </div>
        </div>
      </section>

      <!-- 最近会话 -->
      <section class="mid-card glass-card">
        <div class="mid-head">
          <h3 class="section-label">最近会话</h3>
          <span class="mid-meta">
            {{ (stats?.qa.conversationTotal ?? 0) + (stats?.agent.conversationTotal ?? 0) }} 个会话
          </span>
        </div>
        <el-skeleton v-if="loading" :rows="5" animated />
        <div v-else-if="recentConvs.length === 0" class="empty-block">
          <el-empty description="暂无会话" :image-size="64">
            <el-button type="primary" link @click="router.push('/qa')">发起问答</el-button>
          </el-empty>
        </div>
        <div v-else class="conv-list">
          <div v-for="conv in recentConvs" :key="conv.type + conv.id" class="conv-item" @click="goConv(conv)">
            <div class="conv-icon-wrap" :class="conv.type === 'QA' ? 'conv-qa' : 'conv-agent'">
              <el-icon :size="16">
                <ChatLineRound v-if="conv.type === 'QA'" />
                <Cpu v-else />
              </el-icon>
            </div>
            <div class="conv-info">
              <div class="conv-title" :title="conv.title">{{ conv.title }}</div>
              <div class="conv-meta">
                <el-tag size="small" :type="conv.type === 'QA' ? 'primary' : 'success'" effect="plain">
                  {{ convTypeLabel(conv.type) }}
                </el-tag>
              </div>
            </div>
            <span class="conv-time">{{ formatTime(conv.updatedAt) }}</span>
          </div>
        </div>
      </section>
    </div>

    <!-- 趋势 + 密级分布 -->
    <div class="bottom-row">
      <section class="mid-card glass-card">
        <div class="mid-head">
          <h3 class="section-label">近 7 日趋势</h3>
          <span class="mid-meta">
            <TrendCharts class="trend-icon" />
          </span>
        </div>
        <el-skeleton v-if="loading" :rows="4" animated />
        <div v-else class="trend-chart">
          <div v-for="p in trendPoints" :key="p.date" class="trend-col">
            <div class="trend-bars">
              <div
                class="trend-bar trend-qa"
                :style="{ height: trendHeight(p.qaCount) }"
                :title="`问答 ${p.qaCount}`"
              ></div>
              <div
                class="trend-bar trend-agent"
                :style="{ height: trendHeight(p.agentCount) }"
                :title="`智能体 ${p.agentCount}`"
              ></div>
            </div>
            <div class="trend-date">{{ p.date.slice(5) }}</div>
          </div>
        </div>
        <div class="trend-legend">
          <span class="legend-item"><span class="legend-dot legend-qa"></span>问答</span>
          <span class="legend-item"><span class="legend-dot legend-agent"></span>智能体</span>
        </div>
      </section>

      <section class="mid-card glass-card">
        <div class="mid-head">
          <h3 class="section-label">密级分布</h3>
          <span class="mid-meta">{{ stats?.document.total ?? 0 }} 篇</span>
        </div>
        <el-skeleton v-if="loading" :rows="3" animated />
        <div v-else class="level-chart">
          <div v-for="item in levelDist" :key="item.level" class="level-row">
            <span class="level-name">{{ securityLabel(item.level) }}</span>
            <div class="level-bar-track">
              <div
                class="level-bar"
                :style="{ width: `${stats?.document.total ? (item.count / stats.document.total) * 100 : 0}%` }"
              ></div>
            </div>
            <span class="level-count">{{ item.count }}</span>
          </div>
          <div v-if="levelDist.length === 0" class="empty-block">
            <el-empty description="暂无数据" :image-size="56" />
          </div>
        </div>
      </section>
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
  width: 100%;
  animation: rise 0.5s cubic-bezier(0.22, 1, 0.36, 1) backwards;
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
  margin-bottom: 20px;
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

/* ===== 中间 / 底部双列 ===== */
.mid-row,
.bottom-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-bottom: 20px;
}

.mid-card {
  padding: 18px 20px;
}

.mid-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.mid-meta {
  font-size: 11px;
  color: var(--text-faint);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.trend-icon {
  color: var(--accent-text);
  width: 14px;
  height: 14px;
}

/* ===== 最近文档 ===== */
.doc-list,
.conv-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.doc-item,
.conv-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 8px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.2s;
}

.doc-item:hover,
.conv-item:hover {
  background: var(--surface-hover);
}

.doc-icon-wrap,
.conv-icon-wrap {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary-softer);
  color: var(--primary);
  flex-shrink: 0;
}

.conv-qa {
  background: rgba(34, 211, 238, 0.12);
  color: var(--ai-cyan);
}

.conv-agent {
  background: rgba(244, 114, 182, 0.12);
  color: var(--ai-pink);
}

.doc-info,
.conv-info {
  flex: 1;
  min-width: 0;
}

.doc-title,
.conv-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.doc-meta,
.conv-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.doc-meta-text {
  font-size: 11px;
  color: var(--text-faint);
}

.doc-time,
.conv-time {
  font-size: 11px;
  color: var(--text-faint);
  flex-shrink: 0;
}

/* ===== 空态 ===== */
.empty-block {
  padding: 12px 0;
  display: flex;
  justify-content: center;
}

/* ===== 趋势图 ===== */
.trend-chart {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 130px;
  padding: 0 4px;
}

.trend-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.trend-bars {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 4px;
}

.trend-bar {
  width: 10px;
  border-radius: 4px 4px 0 0;
  transition: height 0.4s cubic-bezier(0.22, 1, 0.36, 1);
  min-height: 3px;
}

.trend-qa {
  background: linear-gradient(180deg, #22D3EE, #0EA5C4);
}

.trend-agent {
  background: linear-gradient(180deg, #F472B6, #DB2777);
}

.trend-date {
  font-size: 10px;
  color: var(--text-faint);
}

.trend-legend {
  display: flex;
  gap: 14px;
  margin-top: 10px;
  justify-content: center;
  font-size: 11px;
  color: var(--text-dim);
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 2px;
}

.legend-qa {
  background: #22D3EE;
}

.legend-agent {
  background: #F472B6;
}

/* ===== 密级分布 ===== */
.level-chart {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.level-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.level-name {
  font-size: 12px;
  color: var(--text-soft);
  width: 48px;
  flex-shrink: 0;
}

.level-bar-track {
  flex: 1;
  height: 8px;
  background: var(--surface-hover);
  border-radius: 999px;
  overflow: hidden;
}

.level-bar {
  height: 100%;
  background: var(--grad-ai);
  border-radius: 999px;
  transition: width 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}

.level-count {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-soft);
  width: 36px;
  text-align: right;
  flex-shrink: 0;
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

@media (max-width: 900px) {
  .mid-row,
  .bottom-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .quick-grid {
    grid-template-columns: 1fr;
  }
}

/* ===== 移动端自适应 ===== */
@media (max-width: 768px) {
  .hero-card {
    padding: 20px 18px;
    margin-bottom: 14px;
  }

  .hero-title {
    font-size: 20px;
  }

  .hero-desc {
    font-size: 12.5px;
    margin-bottom: 16px;
  }

  .hero-actions {
    flex-wrap: wrap;
  }

  .stats-row {
    gap: 10px;
    margin-bottom: 14px;
  }

  .stat-card {
    padding: 14px;
  }

  .stat-value {
    font-size: 22px;
  }

  .mid-row,
  .bottom-row {
    gap: 10px;
    margin-bottom: 14px;
  }

  .mid-card {
    padding: 14px;
  }

  .quick-grid {
    gap: 10px;
    margin-bottom: 18px;
  }

  .quick-card {
    padding: 14px;
    gap: 10px;
  }

  .quick-icon {
    width: 40px;
    height: 40px;
    border-radius: 12px;
  }

  .capability-strip {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 14px 16px;
  }
}
</style>
