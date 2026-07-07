import { useEffect, useRef } from "react";

export interface LossChartProps {
  /** Pairs of [trainLoss, testLoss], one per training step. */
  history: Array<[number, number]>;
  width?: number;
  height?: number;
}

/**
 * Lightweight canvas line chart for the train / test loss curves.
 *
 * Auto-scales the y axis to the highest loss observed so the curves stay
 * useful across the full ~0.7 to ~0 range typical of a playground run.
 */
export function LossChart({
  history,
  width = 280,
  height = 60,
}: LossChartProps) {
  const ref = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = ref.current;
    if (!canvas) return;
    const dpr = window.devicePixelRatio ?? 1;
    canvas.width = width * dpr;
    canvas.height = height * dpr;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, width, height);

    if (history.length === 0) return;

    let maxY = 0;
    for (const pair of history) {
      maxY = Math.max(maxY, pair[0], pair[1]);
    }
    if (maxY === 0) maxY = 1; // avoid division by zero on a fresh run
    maxY = Math.max(maxY, 0.001); // tiny floor for extremely converged runs

    const xStep = history.length > 1 ? width / (history.length - 1) : width;

    function drawSeries(idx: 0 | 1, color: string) {
      ctx!.beginPath();
      ctx!.lineWidth = 1.5;
      ctx!.strokeStyle = color;
      for (let i = 0; i < history.length; i++) {
        const x = i * xStep;
        const y = height - (history[i][idx] / maxY) * height;
        if (i === 0) ctx!.moveTo(x, y);
        else ctx!.lineTo(x, y);
      }
      ctx!.stroke();
    }

    // gridlines (4 horizontal)
    ctx.strokeStyle = "rgba(244, 240, 255, 0.08)";
    ctx.lineWidth = 1;
    for (let i = 1; i < 4; i++) {
      ctx.beginPath();
      ctx.moveTo(0, (height * i) / 4);
      ctx.lineTo(width, (height * i) / 4);
      ctx.stroke();
    }

    drawSeries(0, "#fbbf24");
    drawSeries(1, "#c4b5fd");
  }, [history, width, height]);

  return (
    <canvas
      ref={ref}
      className="block bg-pg-raised rounded border border-pg-border/60"
      style={{ width, height }}
    />
  );
}
