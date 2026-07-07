# Playground Frontend (React + Tailwind)

Vite-powered React + TypeScript + Tailwind CSS UI that talks to the Java
backend over REST.

## Prerequisites

- Node.js 18+ (tested with Node 22)
- The Java backend running on http://localhost:8080 (see ../backend/README.md)

## Quick start

```bash
npm install
npm run dev          # starts the dev server on http://localhost:5173
```

The dev server proxies `/api` to the backend; no CORS dance needed in
development. Set `VITE_BACKEND_URL` in the environment if your backend lives
somewhere other than `http://localhost:8080`.

## Production build

```bash
npm run build
npm run preview      # serves the built bundle locally
```

`vite build` writes the static bundle into `dist/`.

## Layout

```
frontend/
  src/
    App.tsx                  -- top level container, owns session + play loop
    api/
      client.ts              -- thin fetch wrapper
      types.ts
    components/
      Controls.tsx           -- top toolbar (play / step / reset / hyper-params)
      DatasetSelector.tsx
      FeatureSelector.tsx
      Heatmap.tsx            -- canvas-based decision boundary
      LossChart.tsx          -- canvas-based loss curve
      NetworkGraph.tsx       -- SVG graph with bias / weight visualisation
    util/
      colors.ts              -- shared 3-stop color gradient
      datasets.ts            -- client-side thumbnails
      debounce.ts
```

## Notes

* The actual training, dataset generation and decision boundary computation
  all run server-side in Java. The frontend only renders the snapshots that
  the backend sends.
* The play loop polls `POST /train` repeatedly with `epochs=1` so the UI
  stays responsive between updates and slider changes take effect mid-run.
* Decision boundaries are refreshed every few iterations to keep network
  traffic reasonable; `BOUNDARY_REFRESH_EVERY` in `App.tsx` controls this.
