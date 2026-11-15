import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Same origin for the browser keeps the session and CSRF cookies working without CORS.
  server: {
    proxy: { '/api': 'http://localhost:8080' },
  },
})
