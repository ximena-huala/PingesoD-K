import { defineConfig } from "vitest/config";
import path from "path";

// Config de pruebas del frontend (Vitest). Entorno jsdom para tener
// localStorage y APIs de navegador que usa el cliente de API.
export default defineConfig({
  resolve: {
    alias: { "@": path.resolve(__dirname, "./src") },
  },
  test: {
    environment: "jsdom",
    include: ["src/**/*.test.ts", "src/**/*.test.tsx"],
  },
});
