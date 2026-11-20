
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
    plugins: [react()],
    server: {
        proxy: {
            // API independiente de reservas
            '/reservas-api': {
                target: 'http://localhost:8082',
                changeOrigin: true,
                rewrite: (path) =>
                    path.replace(/^\/reservas-api/, ''),
            },

            // NUEVO: API independiente de pedidos
            '/pedidos-api': {
                target: 'http://localhost:8083',
                changeOrigin: true,
                rewrite: (path) =>
                    path.replace(/^\/pedidos-api/, ''),
            },

            // Backend principal: usuarios, JWT y catálogo
            '/api': {
                target: 'http://localhost:8081',
                changeOrigin: true,
            },
        },
    },
})
