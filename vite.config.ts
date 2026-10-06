import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    host: "0.0.0.0",
    port: 5173,
    strictPort: true,
    allowedHosts: ["localhost", ".e2b.app"],
    proxy: {
      "/api": {
        target: "http://127.0.0.1:4173",
        changeOrigin: false,
        configure: (proxy) => {
          proxy.on("proxyReq", (proxyRequest, request) => {
            const originalHost = request.headers.host;
            if (originalHost) proxyRequest.setHeader("x-forwarded-host", originalHost);
          });
        },
      },
    },
  },
});
