import { useEffect, useRef } from "react";
import type { Point } from "../api/types";
import { valueToHex, valueToRGB } from "../util/colors";

export interface HeatmapProps {
  /** Square decision-boundary matrix indexed [x][y]. */
  data?: number[][];
  /** Domain of the data axes (always square in this app). */
  xDomain: [number, number];
  yDomain: [number, number];
  /** Pixel size of the rendered square. */
  size: number;
  /** Optional points overlaid on top of the heatmap. */
  trainData?: Point[];
  testData?: Point[];
  /** When true, force discrete colors. */
  discretize?: boolean;
  /** Hide axes for the small per-node previews. */
  showAxes?: boolean;
  /** When false, the heatmap is rendered without overlaid points. */
  showPoints?: boolean;
}

/**
 * Canvas-based heatmap closely matching the original playground visualization.
 * Rendering happens off-screen at boundary resolution and is then scaled by
 * CSS to keep the React render path cheap.
 */
export function Heatmap({
  data,
  xDomain,
  yDomain,
  size,
  trainData,
  testData,
  discretize = false,
  showAxes = false,
  showPoints = true,
}: HeatmapProps) {
  const heatmapRef = useRef<HTMLCanvasElement | null>(null);
  const overlayRef = useRef<HTMLCanvasElement | null>(null);

  const padding = showAxes ? 20 : 0;
  const innerSize = size - 2 * padding;

  // Render the boundary into the heatmap canvas.
  useEffect(() => {
    const canvas = heatmapRef.current;
    if (!canvas || !data || data.length === 0) return;
    const dx = data.length;
    const dy = data[0].length;
    canvas.width = dx;
    canvas.height = dy;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    const image = ctx.createImageData(dx, dy);
    let p = 0;
    for (let y = 0; y < dy; y++) {
      for (let x = 0; x < dx; x++) {
        const raw = data[x][y];
        const v = discretize ? (raw >= 0 ? 1 : -1) : raw;
        const [r, g, b] = valueToRGB(v);
        image.data[p++] = r;
        image.data[p++] = g;
        image.data[p++] = b;
        image.data[p++] = 160;
      }
    }
    ctx.putImageData(image, 0, 0);
  }, [data, discretize]);

  // Render the data points into the overlay canvas at full size.
  useEffect(() => {
    const canvas = overlayRef.current;
    if (!canvas) return;
    const dpr = window.devicePixelRatio ?? 1;
    canvas.width = innerSize * dpr;
    canvas.height = innerSize * dpr;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.scale(dpr, dpr);
    ctx.clearRect(0, 0, innerSize, innerSize);
    if (!showPoints) return;
    const xRange = xDomain[1] - xDomain[0];
    const yRange = yDomain[1] - yDomain[0];

    function drawPoints(points: Point[] | undefined, stroke: string | null) {
      if (!points) return;
      for (const [x, y, label] of points) {
        if (x < xDomain[0] || x > xDomain[1] || y < yDomain[0] || y > yDomain[1])
          continue;
        const cx = ((x - xDomain[0]) / xRange) * innerSize;
        // Y is inverted: top of screen = +y.
        const cy = innerSize - ((y - yDomain[0]) / yRange) * innerSize;
        ctx!.beginPath();
        ctx!.arc(cx, cy, 3, 0, Math.PI * 2);
        ctx!.fillStyle = valueToHex(label);
        ctx!.fill();
        if (stroke) {
          ctx!.lineWidth = 1;
          ctx!.strokeStyle = stroke;
          ctx!.stroke();
        }
      }
    }

    drawPoints(trainData, null);
    drawPoints(testData, "white");
  }, [trainData, testData, innerSize, xDomain, yDomain, showPoints]);

  return (
    <div
      className="relative"
      style={{ width: size, height: size }}
      role="img"
      aria-label="decision boundary"
    >
      <canvas
        ref={heatmapRef}
        className="absolute"
        style={{
          left: padding,
          top: padding,
          width: innerSize,
          height: innerSize,
          imageRendering: "pixelated",
        }}
      />
      <canvas
        ref={overlayRef}
        className="absolute"
        style={{
          left: padding,
          top: padding,
          width: innerSize,
          height: innerSize,
        }}
      />
      {showAxes && <Axes size={innerSize} padding={padding} domain={xDomain} />}
    </div>
  );
}

function Axes({
  size,
  padding,
  domain,
}: {
  size: number;
  padding: number;
  domain: [number, number];
}) {
  const ticks = [-6, -3, 0, 3, 6];
  const xToPx = (v: number) =>
    ((v - domain[0]) / (domain[1] - domain[0])) * size + padding;
  const yToPx = (v: number) =>
    size - ((v - domain[0]) / (domain[1] - domain[0])) * size + padding;
  return (
    <svg
      className="absolute pointer-events-none text-pg-dim"
      style={{ left: 0, top: 0, width: size + 2 * padding, height: size + 2 * padding }}
    >
      {/* x axis (bottom) */}
      <line
        x1={padding}
        y1={size + padding}
        x2={size + padding}
        y2={size + padding}
        stroke="currentColor"
      />
      {ticks.map((t) => (
        <g key={`x${t}`}>
          <line
            x1={xToPx(t)}
            y1={size + padding}
            x2={xToPx(t)}
            y2={size + padding + 4}
            stroke="currentColor"
          />
          <text
            x={xToPx(t)}
            y={size + padding + 14}
            fontSize={10}
            textAnchor="middle"
            fill="currentColor"
          >
            {t}
          </text>
        </g>
      ))}
      {/* y axis (right) */}
      <line
        x1={size + padding}
        y1={padding}
        x2={size + padding}
        y2={size + padding}
        stroke="currentColor"
      />
      {ticks.map((t) => (
        <g key={`y${t}`}>
          <line
            x1={size + padding}
            y1={yToPx(t)}
            x2={size + padding + 4}
            y2={yToPx(t)}
            stroke="currentColor"
          />
          <text
            x={size + padding + 6}
            y={yToPx(t) + 3}
            fontSize={10}
            textAnchor="start"
            fill="currentColor"
          >
            {t}
          </text>
        </g>
      ))}
    </svg>
  );
}
