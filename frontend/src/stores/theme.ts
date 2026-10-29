import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/** 主题模式 */
export type ThemeMode = 'light' | 'dark'

/** localStorage 存储键 */
const STORAGE_KEY = 'agenthub_theme'

/**
 * 读取初始主题：本地偏好优先，其次跟随系统偏好。
 */
function readInitialTheme(): ThemeMode {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved === 'light' || saved === 'dark') return saved
  } catch {
    // localStorage 不可用时忽略
  }
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

/**
 * 将主题应用到 <html> 元素上。
 */
function applyTheme(mode: ThemeMode) {
  const root = document.documentElement
  root.classList.toggle('dark', mode === 'dark')
  root.style.colorScheme = mode
}

/**
 * 在应用挂载前初始化主题，避免首屏主题闪烁。
 */
export function initTheme() {
  applyTheme(readInitialTheme())
}

/**
 * 主题状态管理（明暗切换）。
 */
export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>(readInitialTheme())

  /** 当前是否为暗色 */
  const isDark = computed(() => mode.value === 'dark')

  /** 设置主题并持久化 */
  function setMode(next: ThemeMode) {
    mode.value = next
    applyTheme(next)
    try {
      localStorage.setItem(STORAGE_KEY, next)
    } catch {
      // 忽略存储失败
    }
  }

  /** 切换明暗主题 */
  function toggle() {
    setMode(isDark.value ? 'light' : 'dark')
  }

  /** 跟随系统主题变化（仅在用户未手动设置过时生效） */
  function bindSystemTheme() {
    const mq = window.matchMedia?.('(prefers-color-scheme: dark)')
    if (!mq) return
    const handler = () => {
      try {
        if (localStorage.getItem(STORAGE_KEY)) return
      } catch {
        // 忽略存储异常
      }
      mode.value = mq.matches ? 'dark' : 'light'
      applyTheme(mode.value)
    }
    mq.addEventListener('change', handler)
  }

  bindSystemTheme()

  return { mode, isDark, setMode, toggle }
})
