import { computed, onMounted, onUnmounted, ref } from 'vue'

/** 移动端断点（与各页面 @media 断点保持一致） */
export const MOBILE_BREAKPOINT = 768

/** 平板断点 */
export const TABLET_BREAKPOINT = 1024

/**
 * 响应式断点状态。
 * 通过监听窗口尺寸变化，让组件在移动端切换布局（如侧边栏抽屉、表格列裁剪）。
 */
export function useResponsive() {
  const width = ref(typeof window === 'undefined' ? 1440 : window.innerWidth)

  const update = () => {
    width.value = window.innerWidth
  }

  onMounted(() => {
    update()
    window.addEventListener('resize', update, { passive: true })
    window.addEventListener('orientationchange', update)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', update)
    window.removeEventListener('orientationchange', update)
  })

  /** 是否为移动端 */
  const isMobile = computed(() => width.value <= MOBILE_BREAKPOINT)

  /** 是否为平板（移动端以上、桌面以下） */
  const isTablet = computed(
    () => width.value > MOBILE_BREAKPOINT && width.value <= TABLET_BREAKPOINT
  )

  return { width, isMobile, isTablet }
}
