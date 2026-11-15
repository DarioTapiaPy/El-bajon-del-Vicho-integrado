import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
    plugins: [react()],
    server: {
        proxy: {
            // API independiente de reservas, sin modificar rutas del backend principal.
            '/reservas-api': {
                target: 'http://localhost:8082',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/reservas-api/, ''),
            },
            '/api': {
                target: 'http://localhost:8081',
                changeOrigin: true,
            },
        },
    },
})
