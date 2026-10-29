import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// 백엔드(8080)에는 CORS 설정이 없으므로 dev server 프록시로 /api 를 전달한다.
// SSE(/api/battles/subscribe)도 같은 프록시를 탄다.
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.js',
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
