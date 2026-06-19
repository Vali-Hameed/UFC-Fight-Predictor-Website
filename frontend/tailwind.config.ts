import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}", "./lib/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        bg: "#0a0a0f",
        panel: "#111118",
        panelSoft: "#171723",
        accent: "#d20a0a",
        gold: "#c9a84c",
        silver: "#C0C0C0",
        bronze: "#CD7F32"
      },
      boxShadow: {
        glow: "0 0 40px rgba(210, 10, 10, 0.24)"
      }
    }
  },
  plugins: []
};

export default config;