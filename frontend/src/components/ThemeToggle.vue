<script setup lang="ts">
import { Sunny, Moon } from '@element-plus/icons-vue'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()

/** 切换明暗主题 */
function handleToggle() {
  themeStore.toggle()
}
</script>

<template>
  <button
    class="theme-toggle"
    type="button"
    :title="themeStore.isDark ? '切换到浅色模式' : '切换到深色模式'"
    :aria-label="themeStore.isDark ? '切换到浅色模式' : '切换到深色模式'"
    :aria-pressed="themeStore.isDark"
    @click="handleToggle"
  >
    <span class="toggle-track" :class="{ 'is-dark': themeStore.isDark }">
      <el-icon class="toggle-icon toggle-icon--sun" :size="13"><Sunny /></el-icon>
      <el-icon class="toggle-icon toggle-icon--moon" :size="13"><Moon /></el-icon>
      <span class="toggle-thumb" :class="{ 'is-dark': themeStore.isDark }"></span>
    </span>
  </button>
</template>

<style scoped>
.theme-toggle {
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  line-height: 0;
}

.toggle-track {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  width: 58px;
  height: 30px;
  padding: 0 6px;
  border-radius: 999px;
  background: var(--surface-hover);
  border: 1px solid var(--border);
  backdrop-filter: blur(8px);
  transition: background 0.3s ease, border-color 0.3s ease;
}

.theme-toggle:hover .toggle-track {
  border-color: var(--primary-border);
  box-shadow: 0 0 16px -6px var(--focus-glow);
}

.toggle-icon {
  position: relative;
  z-index: 1;
  transition: color 0.3s ease, opacity 0.3s ease;
}

.toggle-icon--sun {
  color: #F59E0B;
  opacity: 0.55;
}

.toggle-icon--moon {
  color: var(--ai-violet);
  opacity: 0.4;
}

/* 暗色模式下高亮月亮图标 */
.toggle-track.is-dark .toggle-icon--sun {
  opacity: 0.35;
}

.toggle-track.is-dark .toggle-icon--moon {
  opacity: 1;
}

/* 滑块 */
.toggle-thumb {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--grad-violet);
  box-shadow: 0 0 14px -3px var(--focus-glow);
  transition: transform 0.32s cubic-bezier(0.22, 1, 0.36, 1), background 0.3s ease;
}

.toggle-thumb.is-dark {
  transform: translateX(28px);
}
</style>
