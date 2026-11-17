import { fileURLToPath, URL } from 'node:url'
import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// Windows 上系统临时目录可能被安全软件或当前用户权限限制，esbuild 清理临时文件会报 Access is denied。
// 将 esbuild 的临时目录固定到项目内，避免 build 和 dev 受系统 Temp 权限影响。
const localTempDir = resolve(process.cwd(), '.tmp', 'esbuild')
mkdirSync(localTempDir, { recursive: true })
process.env.TEMP = localTempDir
process.env.TMP = localTempDir
process.env.TMPDIR = localTempDir
process.env.ESBUILD_TMPDIR = localTempDir

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiProxyTarget = env.VITE_API_PROXY || process.env.VITE_API_PROXY || 'http://127.0.0.1:8080'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: 5173,
      host: '127.0.0.1',
      watch: {
        ignored: ['**/.*.tmpdir/**', '**/*.tmpdir/**', '**/.tmp/**'],
      },
      proxy: {
        '/api': {
          target: apiProxyTarget,
          changeOrigin: true,
        },
      },
    },
    build: {
      outDir: 'dist',
      chunkSizeWarningLimit: 1500,
    },
    preview: {
      port: 4173,
      host: '127.0.0.1',
      proxy: {
        '/api': {
          target: apiProxyTarget,
          changeOrigin: true,
        },
      },
    },
  }
})

