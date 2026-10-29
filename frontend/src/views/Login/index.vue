<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Right } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import ThemeToggle from '@/components/ThemeToggle.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

/** 登录表单 */
const form = reactive({
  username: '',
  password: ''
})

/** 校验规则 */
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

/** 加载状态 */
const loading = ref(false)
const formRef = ref()

/** 提交登录 */
async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    ElMessage.success('登录成功')

    // 跳转目标页（如有 redirect 参数）或首页
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.replace(redirect)
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '登录失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

/** 平台能力标签 */
const capabilities = ['RAG 语义检索', '多智能体编排', '细粒度权限', '全量审计']
</script>

<template>
  <div class="login-container ai-canvas">
    <!-- 极光光斑 -->
    <div class="orb orb--violet"></div>
    <div class="orb orb--cyan"></div>
    <div class="orb orb--lime"></div>

    <!-- 网格遮罩 -->
    <div class="grid-mask"></div>

    <!-- 主题切换 -->
    <div class="login-theme-toggle">
      <ThemeToggle />
    </div>

    <!-- 左侧品牌区 -->
    <section class="hero">
      <div class="hero-badge ai-badge">
        <span class="ai-dot"></span>
        Enterprise AI Platform
      </div>
      <h1 class="hero-title">
        让企业的每一份知识<br />
        都能被 <span class="gradient-text">智能调用</span>
      </h1>
      <p class="hero-desc">
        智知 AgentHub 融合文档知识库、RAG 检索增强与自主智能体，
        让散落在组织内部的文档真正成为可被对话、可被推理的决策资产。
      </p>

      <div class="capability-list">
        <span v-for="cap in capabilities" :key="cap" class="capability-chip">
          {{ cap }}
        </span>
      </div>

      <!-- 指标 -->
      <div class="hero-metrics">
        <div class="metric">
          <div class="metric-value gradient-text">1024</div>
          <div class="metric-label">向量维度</div>
        </div>
        <div class="metric">
          <div class="metric-value gradient-text">&lt;50ms</div>
          <div class="metric-label">检索延迟</div>
        </div>
        <div class="metric">
          <div class="metric-value gradient-text">4×3</div>
          <div class="metric-label">角色 × 密级</div>
        </div>
      </div>
    </section>

    <!-- 登录卡片 -->
    <section class="login-panel">
      <div class="login-card glass-card neon-ring">
        <!-- 顶部光带 -->
        <span class="card-strip"></span>

        <div class="login-header">
          <div class="logo">
            <span class="logo-ring"></span>
            <span class="logo-text">智</span>
          </div>
          <h1 class="title">智知 AgentHub</h1>
          <p class="subtitle">企业知识管理平台 · 登录</p>
        </div>

        <!-- 登录表单 -->
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="用户名"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              进入智能中枢
              <el-icon class="ml-1"><Right /></el-icon>
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 提示信息 -->
        <div class="login-tip">
          <p>默认管理员账号：admin / admin123</p>
        </div>
      </div>
    </section>

    <!-- 底部 -->
    <div class="login-footer">
      <p>© 2026 智知 AgentHub · Enterprise Knowledge Intelligence</p>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 80px;
  height: 100vh;
  overflow: hidden;
}

/* ===== 极光光斑 ===== */
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  pointer-events: none;
}

.orb--violet {
  width: 560px;
  height: 560px;
  top: -160px;
  left: 6%;
  background: radial-gradient(circle, rgba(124, 92, 255, 0.45), transparent 68%);
  animation: float 11s ease-in-out infinite;
}

.orb--cyan {
  width: 480px;
  height: 480px;
  bottom: -170px;
  right: 4%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.32), transparent 68%);
  animation: float 14s ease-in-out infinite reverse;
}

.orb--lime {
  width: 320px;
  height: 320px;
  top: 42%;
  left: 44%;
  background: radial-gradient(circle, rgba(163, 230, 53, 0.16), transparent 70%);
  animation: float 9s ease-in-out infinite;
}

/* 网格 */
.grid-mask {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.07) 1px, transparent 1px);
  background-size: 52px 52px;
  mask-image: radial-gradient(ellipse 90% 90% at 50% 50%, #000 25%, transparent 80%);
  -webkit-mask-image: radial-gradient(ellipse 90% 90% at 50% 50%, #000 25%, transparent 80%);
}

/* ===== 左侧品牌 ===== */
.hero {
  position: relative;
  z-index: 1;
  max-width: 520px;
  animation: rise 0.7s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.hero-badge {
  margin-bottom: 22px;
}

.hero-title {
  font-size: 40px;
  font-weight: 800;
  line-height: 1.28;
  letter-spacing: -0.01em;
  color: var(--text);
  margin: 0 0 18px;
}

.hero-desc {
  font-size: 14px;
  line-height: 1.9;
  color: var(--text-dim);
  margin: 0 0 26px;
  max-width: 460px;
}

.capability-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 38px;
}

.capability-chip {
  padding: 5px 13px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--text-soft);
  background: var(--glass);
  border: 1px solid var(--border);
  backdrop-filter: blur(8px);
}

.hero-metrics {
  display: flex;
  gap: 42px;
}

.metric-value {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.metric-label {
  font-size: 11px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-faint);
  margin-top: 3px;
}

/* ===== 登录卡片 ===== */
.login-panel {
  position: relative;
  z-index: 1;
  animation: rise 0.7s 0.1s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.login-card {
  position: relative;
  width: 400px;
  padding: 42px 38px 34px;
  background: var(--veil-strong);
  backdrop-filter: blur(22px);
  overflow: hidden;
}

.card-strip {
  position: absolute;
  top: 0;
  left: 15%;
  right: 15%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(34, 211, 238, 0.95), transparent);
}

/* ===== Logo 区域 ===== */
.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  position: relative;
  width: 54px;
  height: 54px;
  margin: 0 auto 16px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #7C5CFF 0%, #22D3EE 100%);
  box-shadow: 0 0 34px -6px rgba(124, 92, 255, 0.95);
}

.logo-ring {
  position: absolute;
  inset: -5px;
  border-radius: 20px;
  border: 1px solid rgba(34, 211, 238, 0.5);
  border-top-color: transparent;
  border-right-color: transparent;
  animation: spin-slow 7s linear infinite;
}

.logo-text {
  font-size: 23px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
}

.title {
  font-size: 21px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--text);
  margin: 0 0 6px;
}

.subtitle {
  font-size: 12px;
  color: var(--text-faint);
  margin: 0;
}

/* ===== 表单 ===== */
.login-btn {
  width: 100%;
  height: 46px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.08em;
  border-radius: var(--radius-md);
}

.login-tip {
  margin-top: 18px;
  text-align: center;
}

.login-tip p {
  font-size: 11px;
  color: var(--text-faint);
  margin: 0;
}

/* ===== 底部 ===== */
.login-theme-toggle {
  position: absolute;
  top: 24px;
  right: 28px;
  z-index: 2;
}

.login-footer {
  position: absolute;
  bottom: 22px;
  left: 0;
  right: 0;
  text-align: center;
  z-index: 1;
}

.login-footer p {
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--text-faint);
  margin: 0;
}

/* ===== Element Plus 覆盖 ===== */
:deep(.el-input__wrapper) {
  border-radius: var(--radius-md);
  height: 46px;
  background-color: var(--surface-soft);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--ai-violet) inset, 0 0 20px -6px rgba(124, 92, 255, 0.9);
}

@media (max-width: 1100px) {
  .hero {
    display: none;
  }
}
</style>
