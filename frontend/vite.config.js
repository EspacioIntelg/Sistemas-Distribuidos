import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // El navegador siempre llama a rutas relativas ("/api/..."), sin importar
      // si accedes por localhost o por una URL reenviada (p. ej. Codespaces/devcontainer).
      // Vite (que corre dentro del contenedor) reenvia esas peticiones al Gateway.
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
})
