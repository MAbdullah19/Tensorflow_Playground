import type {
  Activation,
  Problem,
  Regularization,
} from "../api/types";

export interface ControlsProps {
  isPlaying: boolean;
  iter: number;
  onPlayPause: () => void;
  onStep: () => void;
  onReset: () => void;
  // hyper-parameters
  learningRate: number;
  onLearningRate: (v: number) => void;
  activation: Activation;
  onActivation: (v: Activation) => void;
  regularization: Regularization;
  onRegularization: (v: Regularization) => void;
  regularizationRate: number;
  onRegularizationRate: (v: number) => void;
  problem: Problem;
  onProblem: (v: Problem) => void;
  batchSize: number;
  onBatchSize: (v: number) => void;
}

const LEARNING_RATES = [
  0.00001, 0.0001, 0.001, 0.003, 0.01, 0.03, 0.1, 0.3, 1, 3, 10,
];
const REG_RATES = [0, 0.001, 0.003, 0.01, 0.03, 0.1, 0.3, 1, 3, 10];

const ICON_PLAY =
  "M8 5v14l11-7z";
const ICON_PAUSE =
  "M6 4h4v16H6zM14 4h4v16h-4z";

export function Controls({
  isPlaying,
  iter,
  onPlayPause,
  onStep,
  onReset,
  learningRate,
  onLearningRate,
  activation,
  onActivation,
  regularization,
  onRegularization,
  regularizationRate,
  onRegularizationRate,
  problem,
  onProblem,
  batchSize,
  onBatchSize,
}: ControlsProps) {
  return (
    <div className="flex flex-wrap items-center gap-x-6 gap-y-3 px-4 py-3 bg-pg-panel border-b border-pg-border">
      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={onReset}
          className="w-10 h-10 rounded-full border border-pg-border bg-pg-raised hover:bg-violet-950/50 text-pg-text flex items-center justify-center"
          aria-label="reset"
          title="Reset"
        >
          <svg viewBox="0 0 24 24" width={18} height={18} fill="currentColor">
            <path d="M12 5V1L7 6l5 5V7c3.31 0 6 2.69 6 6s-2.69 6-6 6-6-2.69-6-6H4c0 4.42 3.58 8 8 8s8-3.58 8-8-3.58-8-8-8z" />
          </svg>
        </button>
        <button
          type="button"
          onClick={onPlayPause}
          className="w-12 h-12 rounded-full bg-amber-500 text-violet-950 hover:bg-amber-400 shadow-md shadow-amber-900/30 flex items-center justify-center"
          aria-label={isPlaying ? "pause" : "play"}
          title={isPlaying ? "Pause" : "Play"}
        >
          <svg viewBox="0 0 24 24" width={22} height={22} fill="currentColor">
            <path d={isPlaying ? ICON_PAUSE : ICON_PLAY} />
          </svg>
        </button>
        <button
          type="button"
          onClick={onStep}
          className="w-10 h-10 rounded-full border border-pg-border bg-pg-raised hover:bg-violet-950/50 text-pg-text flex items-center justify-center"
          aria-label="step"
          title="One step"
        >
          <svg viewBox="0 0 24 24" width={18} height={18} fill="currentColor">
            <path d="M6 18l8.5-6L6 6v12zM16 6h2v12h-2z" />
          </svg>
        </button>
      </div>

      <Field label="Epoch">
        <span className="font-mono text-sm text-amber-300/95">{iter.toString().padStart(6, "0")}</span>
      </Field>

      <Field label="Learning rate">
        <Select
          value={learningRate.toString()}
          onChange={(v) => onLearningRate(Number(v))}
          options={LEARNING_RATES.map((r) => ({ value: r.toString(), label: r.toString() }))}
        />
      </Field>

      <Field label="Activation">
        <Select
          value={activation}
          onChange={(v) => onActivation(v as Activation)}
          options={[
            { value: "tanh", label: "Tanh" },
            { value: "relu", label: "ReLU" },
            { value: "sigmoid", label: "Sigmoid" },
            { value: "linear", label: "Linear" },
          ]}
        />
      </Field>

      <Field label="Regularization">
        <Select
          value={regularization}
          onChange={(v) => onRegularization(v as Regularization)}
          options={[
            { value: "none", label: "None" },
            { value: "L1", label: "L1" },
            { value: "L2", label: "L2" },
          ]}
        />
      </Field>

      <Field label="Reg. rate">
        <Select
          value={regularizationRate.toString()}
          onChange={(v) => onRegularizationRate(Number(v))}
          options={REG_RATES.map((r) => ({ value: r.toString(), label: r.toString() }))}
        />
      </Field>

      <Field label="Problem type">
        <Select
          value={problem}
          onChange={(v) => onProblem(v as Problem)}
          options={[
            { value: "classification", label: "Classification" },
            { value: "regression", label: "Regression" },
          ]}
        />
      </Field>

      <Field label={`Batch size: ${batchSize}`}>
        <input
          type="range"
          min={1}
          max={30}
          value={batchSize}
          onChange={(e) => onBatchSize(Number(e.target.value))}
          className="slider w-32"
        />
      </Field>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="flex flex-col text-xs text-pg-dim gap-1">
      <span>{label}</span>
      {children}
    </label>
  );
}

function Select<T extends string>({
  value,
  onChange,
  options,
}: {
  value: T;
  onChange: (v: T) => void;
  options: Array<{ value: string; label: string }>;
}) {
  return (
    <select
      className="border border-pg-border rounded px-2 py-1 text-sm bg-pg-raised text-pg-text"
      value={value}
      onChange={(e) => onChange(e.target.value as T)}
    >
      {options.map((o) => (
        <option key={o.value} value={o.value}>
          {o.label}
        </option>
      ))}
    </select>
  );
}
