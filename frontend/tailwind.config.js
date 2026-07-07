/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        /** Decision-boundary gradient (replaces cool blue / warm orange). */
        positive: "#34d399",
        negative: "#f43f5e",
        neutral: "#78716c",
        /** App chrome — deep violet night theme */
        pg: {
          bg: "#0c0a10",
          panel: "#15101f",
          raised: "#1f1830",
          border: "#4c3d6b",
          text: "#f5f0ff",
          muted: "#a898c9",
          dim: "#7d6ca3",
        },
      },
      fontFamily: {
        sans: [
          "Roboto",
          "ui-sans-serif",
          "system-ui",
          "Helvetica Neue",
          "Arial",
          "sans-serif",
        ],
      },
    },
  },
  plugins: [],
};
