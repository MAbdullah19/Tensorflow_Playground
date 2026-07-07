import type { FeatureKey } from "../api/types";
import { Heatmap } from "./Heatmap";

const ALL: FeatureKey[] = ["x", "y"];

export const FEATURE_LABELS: Record<FeatureKey, string> = {
  x: "X₁",
  y: "X₂",
};

export interface FeatureSelectorProps {
  active: FeatureKey[];
  boundaries: Record<string, number[][]>;
  onToggle: (key: FeatureKey) => void;
  xDomain: [number, number];
  yDomain: [number, number];
}

/**
 * Lets the user pick which input coordinates feed the input layer.
 * Every feature is shown as a small heatmap preview to visualise its shape.
 */
export function FeatureSelector({
  active,
  boundaries,
  onToggle,
  xDomain,
  yDomain,
}: FeatureSelectorProps) {
  return (
    <div className="space-y-2">
      {ALL.map((key) => {
        const isOn = active.includes(key);
        return (
          <button
            key={key}
            type="button"
            onClick={() => onToggle(key)}
            aria-pressed={isOn}
            className={
              "w-full flex items-center gap-2 p-1 rounded border " +
              (isOn
                ? "border-amber-500/70 bg-amber-500/10 shadow-sm shadow-amber-900/20"
                : "border-pg-border opacity-70 hover:opacity-100 hover:border-fuchsia-800/60")
            }
          >
            <Heatmap
              data={boundaries[key]}
              size={36}
              xDomain={xDomain}
              yDomain={yDomain}
              showPoints={false}
            />
            <span
              className={"text-xs " + (isOn ? "text-pg-text" : "text-pg-dim")}
            >
              {FEATURE_LABELS[key]}
            </span>
          </button>
        );
      })}
    </div>
  );
}
