import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  // 读取 .env / .env.<mode> / .env.local 中的变量
  // 第三个参数传空字符串 = 不限制变量前缀，VITE_ 开头的变量也会读入
  const env = loadEnv(mode, process.cwd(), '')

  /**
   * 开发环境代理转发的后端地址。
   * 由 VITE_PROXY_TARGET 配置（见 .env.development / .env.local）。
   * 注意：这里只能填「主机名（可含端口）」，不要带路径 ——
   * 后端 context-path 是 /api，vite 的 '/api' 代理规则会原样保留该前缀，
   * 写成 http://host/api 会被拼成 /api/api/xxx。
   */
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://localhost:8080'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true
          // 上游为自签证书时取消下面一行的注释（仅限开发环境使用）
          // secure: false
        }
      }
    }
  }
})
