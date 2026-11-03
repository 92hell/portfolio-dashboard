import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// In dev, the Spring Boot API runs on :8080 and Vite proxies /api to it.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
