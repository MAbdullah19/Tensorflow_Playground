import { useEffect, useRef } from "react";
import type {
  ClassificationDataset,
  Point,
  Problem,
  RegressionDataset,
} from "../api/types";
import { valueToHex } from "../util/colors";

const CLASSIFY: ClassificationDataset[] = ["circle", "xor", "gauss", "spiral"];
const REGRESS: RegressionDataset[] = ["reg-plane", "reg-gauss"];

const LABELS: Record<string, string> = {
  circle: "Circle",
  xor: "XOR",
  gauss: "Gaussian",
  spiral: "Spiral",
  "reg-plane": "Plane",
  "reg-gauss": "Multi gaussian",
};

export interface DatasetSelectorProps {
  problem: Problem;
  classificationDataset: ClassificationDataset;
  regressionDataset: RegressionDataset;
  onSelectClassification: (key: ClassificationDataset) => void;
  onSelectRegression: (key: RegressionDataset) => void;
  thumbnails: Record<string, Point[]>;
}

export function DatasetSelector({
  problem,
  classificationDataset,
  regressionDataset,
  onSelectClassification,
  onSelectRegression,
  thumbnails,
}: DatasetSelectorProps) {
  const items =
    problem === "classification"
      ? CLASSIFY.map((k) => ({
          key: k as string,
          selected: k === classificationDataset,
          onClick: () => onSelectClassification(k),
        }))
      : REGRESS.map((k) => ({
          key: k as string,
          selected: k === regressionDataset,
          onClick: () => onSelectRegression(k),
        }));

  return (
    <div className="grid grid-cols-2 gap-2">
      {items.map((item) => (
        <button
          key={item.key}
          type="button"
          onClick={item.onClick}
          aria-pressed={item.selected}
          className={
            "border rounded p-1 transition-colors flex flex-col items-center bg-pg-raised " +
            (item.selected
              ? "border-amber-500 ring-2 ring-amber-500/35 shadow-md shadow-black/30"
              : "border-pg-border hover:border-fuchsia-800/70")
          }
          title={LABELS[item.key] ?? item.key}
        >
          <Thumbnail data={thumbnails[item.key]} />
          <span className="text-[10px] text-pg-muted mt-0.5">
            {LABELS[item.key] ?? item.key}
          </span>
        </button>
      ))}
    </div>
  );
}

function Thumbnail({ data }: { data?: Point[] }) {
  const ref = useRef<HTMLCanvasElement | null>(null);
  useEffect(() => {
    const canvas = ref.current;
    if (!canvas) return;
    const dpr = window.devicePixelRatio ?? 1;
    canvas.width = 60 * dpr;
    canvas.height = 60 * dpr;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, 60, 60);
    if (!data) return;
    const range = 12;
    for (const [x, y, label] of data) {
      const cx = ((x + range / 2) / range) * 60;
      const cy = 60 - ((y + range / 2) / range) * 60;
      ctx.fillStyle = valueToHex(label);
      ctx.fillRect(cx - 1.5, cy - 1.5, 3, 3);
    }
  }, [data]);
  return <canvas ref={ref} style={{ width: 60, height: 60 }} />;
}
