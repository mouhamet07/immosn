import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig(({ mode }) => ({
  plugins: [
    vue(),
    // DevTools uniquement en développement local
    ...(mode === 'development' ? [vueDevTools()] : []),
  ],

  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },

  build: {
    chunkSizeWarningLimit: 600,

    rollupOptions: {
      output: {
        manualChunks(id) {
          // Ne traite que les dépendances externes
          if (!id.includes('node_modules')) return

          // Chunk HTTP
          if (id.includes('axios')) {
            return 'http'
          }

          // Chunk framework Vue
          if (
            id.includes('vue') ||
            id.includes('vue-router') ||
            id.includes('pinia')
          ) {
            return 'vendor'
          }

          // fallback
          return 'vendor'
        },
      },
    },
  },
}))
