import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { api } from "./api/client";
import type {
  Activation,
  BoundaryResponse,
  ClassificationDataset,
  Config,
  FeatureKey,
  Point,
  Problem,
  Regularization,
  RegressionDataset,
  Snapshot,
} from "./api/types";
import { Controls } from "./components/Controls";
import { DatasetSelector } from "./components/DatasetSelector";
import { FeatureSelector, FEATURE_LABELS } from "./components/FeatureSelector";
import { Heatmap } from "./components/Heatmap";
import { LossChart } from "./components/LossChart";
import { NetworkGraph } from "./components/NetworkGraph";
import { buildAllThumbnails } from "./util/datasets";
import { debounce } from "./util/debounce";

const HEATMAP_DENSITY = 30;
const BOUNDARY_REFRESH_EVERY = 5; // refresh decision boundary every N steps when playing
const PLAY_STEP_MS = 0; // request the next step as soon as the previous resolves

const DEFAULT_CONFIG: Config = {
  dataset: "circle",
  regDataset: "reg-plane",
  problem: "classification",
  noise: 0,
  percTrainData: 50,
  activation: "tanh",
  regularization: "none",
  initZero: false,
  networkShape: [4, 2],
  features: ["x", "y"],
};

function emptyBoundaries(): BoundaryResponse {
  return { density: HEATMAP_DENSITY, xDomain: [-6, 6], yDomain: [-6, 6], boundaries: {} };
}

/** Top level state container; manages session, snapshot, and playback. */
export default function App() {
  const [snapshot, setSnapshot] = useState<Snapshot | null>(null);
  const [boundaries, setBoundaries] = useState<BoundaryResponse>(emptyBoundaries);
  const [trainData, setTrainData] = useState<Point[]>([]);
  const [testData, setTestData] = useState<Point[]>([]);
  const [showTestData, setShowTestData] = useState(false);
  const [discretize, setDiscretize] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [lossHistory, setLossHistory] = useState<Array<[number, number]>>([]);
  const [batchSize, setBatchSize] = useState(10);
  const [learningRate, setLearningRate] = useState(0.03);
  const [regularizationRate, setRegularizationRate] = useState(0);
  const [noise, setNoise] = useState(0);
  const [percTrainData, setPercTrainData] = useState(50);
  const [config, setConfig] = useState<Config>(DEFAULT_CONFIG);

  const sessionId = snapshot?.sessionId;
  const playStateRef = useRef({ playing: false, sinceBoundary: 0 });
  const stepInFlightRef = useRef(false);
  const thumbnails = useMemo(() => buildAllThumbnails(), []);

  // Mirror hyper-parameters into a ref so the play loop always reads
  // the latest values even if the user changes them mid-training.
  const hyperRef = useRef({ learningRate, regularizationRate, batchSize, discretize });
  useEffect(() => {
    hyperRef.current = { learningRate, regularizationRate, batchSize, discretize };
  }, [learningRate, regularizationRate, batchSize, discretize]);

  // ----- session bootstrap -------------------------------------------
  useEffect(() => {
    let cancelled = false;
    api
      .createSession(DEFAULT_CONFIG)
      .then(async (snap) => {
        if (cancelled) return;
        applySnapshot(snap, true);
        const b = await api.boundary(snap.sessionId, { density: HEATMAP_DENSITY });
        if (!cancelled) setBoundaries(b);
      })
      .catch((e) => !cancelled && setError(e.message));
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /** Apply a snapshot returned by the backend, syncing local UI mirrors too. */
  const applySnapshot = useCallback((snap: Snapshot, isFresh: boolean) => {
    setSnapshot(snap);
    setConfig(snap.config);
    if (snap.trainData) setTrainData(snap.trainData);
    if (snap.testData) setTestData(snap.testData);
    if (isFresh) setLossHistory([[snap.lossTrain, snap.lossTest]]);
    else setLossHistory((prev) => [...prev, [snap.lossTrain, snap.lossTest]]);
  }, []);

  // ----- helpers -----------------------------------------------------
  const refetchBoundary = useCallback(
    async (id: string, opts?: { discretize?: boolean }) => {
      try {
        const b = await api.boundary(id, {
          density: HEATMAP_DENSITY,
          discretize: opts?.discretize ?? discretize,
        });
        setBoundaries(b);
      } catch (e) {
        setError((e as Error).message);
      }
    },
    [discretize],
  );

  /** Debounced version used for sliders that fire many events per second. */
  const debouncedRefetchBoundary = useMemo(
    () => debounce((id: string) => refetchBoundary(id), 80),
    [refetchBoundary],
  );

  // ----- config mutations -------------------------------------------
  const reconfigure = useCallback(
    async (patch: Partial<Config>) => {
      if (!sessionId) return;
      pause();
      try {
        const snap = await api.configure(sessionId, patch);
        applySnapshot(snap, true);
        await refetchBoundary(snap.sessionId);
      } catch (e) {
        setError((e as Error).message);
      }
    },
    [sessionId, applySnapshot, refetchBoundary],
  );

  const regenerateData = useCallback(async () => {
    if (!sessionId) return;
    pause();
    try {
      const snap = await api.regenerateData(sessionId);
      applySnapshot(snap, true);
      await refetchBoundary(snap.sessionId);
    } catch (e) {
      setError((e as Error).message);
    }
  }, [sessionId, applySnapshot, refetchBoundary]);

  const reset = useCallback(async () => {
    if (!sessionId) return;
    pause();
    try {
      const snap = await api.buildNetwork(sessionId);
      applySnapshot(snap, true);
      await refetchBoundary(snap.sessionId);
    } catch (e) {
      setError((e as Error).message);
    }
  }, [sessionId, applySnapshot, refetchBoundary]);

  // ----- play / step ------------------------------------------------
  const stepOnce = useCallback(async () => {
    if (!sessionId || stepInFlightRef.current) return;
    stepInFlightRef.current = true;
    try {
      const { learningRate: lr, regularizationRate: rr, batchSize: bs } = hyperRef.current;
      const snap = await api.train(sessionId, {
        epochs: 1,
        learningRate: lr,
        regularizationRate: rr,
        batchSize: bs,
      });
      applySnapshot(snap, false);
      playStateRef.current.sinceBoundary++;
      if (playStateRef.current.sinceBoundary >= BOUNDARY_REFRESH_EVERY) {
        playStateRef.current.sinceBoundary = 0;
        await refetchBoundary(sessionId);
      }
    } catch (e) {
      setError((e as Error).message);
    } finally {
      stepInFlightRef.current = false;
    }
  }, [sessionId, applySnapshot, refetchBoundary]);

  const playLoop = useCallback(async () => {
    while (playStateRef.current.playing) {
      await stepOnce();
      if (PLAY_STEP_MS > 0) await new Promise((r) => setTimeout(r, PLAY_STEP_MS));
    }
  }, [stepOnce]);

  const playPause = useCallback(() => {
    if (playStateRef.current.playing) {
      playStateRef.current.playing = false;
      setIsPlaying(false);
    } else {
      playStateRef.current.playing = true;
      setIsPlaying(true);
      void playLoop();
    }
  }, [playLoop]);

  function pause() {
    playStateRef.current.playing = false;
    setIsPlaying(false);
  }

  // make sure we stop on unmount
  useEffect(() => {
    return () => {
      playStateRef.current.playing = false;
      if (sessionId) {
        api.deleteSession(sessionId).catch(() => {
          /* ignore */
        });
      }
    };
  }, [sessionId]);

  const onStep = useCallback(async () => {
    pause();
    await stepOnce();
    if (sessionId) await refetchBoundary(sessionId);
  }, [stepOnce, sessionId, refetchBoundary]);

  // ----- specific UI handlers ---------------------------------------
  const onSelectClassification = (k: ClassificationDataset) => {
    void reconfigure({ dataset: k });
  };
  const onSelectRegression = (k: RegressionDataset) => {
    void reconfigure({ regDataset: k });
  };
  const onProblem = (p: Problem) => {
    void reconfigure({ problem: p });
  };
  const onActivation = (a: Activation) => {
    void reconfigure({ activation: a });
  };
  const onRegularization = (r: Regularization) => {
    void reconfigure({ regularization: r });
  };
  const onNoise = (n: number) => {
    setNoise(n);
    void reconfigure({ noise: n });
  };
  const onPercTrain = (p: number) => {
    setPercTrainData(p);
    void reconfigure({ percTrainData: p });
  };
  const onAddLayer = () => {
    if (config.networkShape.length >= 6) return;
    void reconfigure({ networkShape: [...config.networkShape, 2] });
  };
  const onRemoveLayer = () => {
    if (config.networkShape.length <= 0) return;
    void reconfigure({ networkShape: config.networkShape.slice(0, -1) });
  };
  const onAddNeuron = (idx: number) => {
    const shape = [...config.networkShape];
    if (shape[idx] >= 8) return;
    shape[idx]++;
    void reconfigure({ networkShape: shape });
  };
  const onRemoveNeuron = (idx: number) => {
    const shape = [...config.networkShape];
    if (shape[idx] <= 1) return;
    shape[idx]--;
    void reconfigure({ networkShape: shape });
  };
  const onToggleFeature = (key: FeatureKey) => {
    const isOn = config.features.includes(key);
    const next = isOn
      ? config.features.filter((f) => f !== key)
      : [...config.features, key];
    if (next.length === 0) {
      setError("At least one feature must remain enabled.");
      return;
    }
    void reconfigure({ features: next });
  };
  const onDiscretizeChange = (v: boolean) => {
    setDiscretize(v);
    if (sessionId) debouncedRefetchBoundary(sessionId);
  };

  // ----- derived ----------------------------------------------------
  const xDomain: [number, number] = boundaries.xDomain ?? [-6, 6];
  const yDomain: [number, number] = boundaries.yDomain ?? [-6, 6];

  const outputId =
    snapshot && snapshot.layers.length
      ? snapshot.layers[snapshot.layers.length - 1][0].id
      : null;
  const mainBoundaryId = selectedNodeId ?? outputId;
  const mainBoundary =
    mainBoundaryId && boundaries.boundaries[mainBoundaryId]
      ? boundaries.boundaries[mainBoundaryId]
      : undefined;

  // ----- render -----------------------------------------------------
  return (
    <div className="min-h-screen flex flex-col bg-pg-bg">
      <Header />
      <Controls
        isPlaying={isPlaying}
        iter={snapshot?.iter ?? 0}
        onPlayPause={playPause}
        onStep={() => void onStep()}
        onReset={() => void reset()}
        learningRate={learningRate}
        onLearningRate={setLearningRate}
        activation={config.activation}
        onActivation={onActivation}
        regularization={config.regularization}
        onRegularization={onRegularization}
        regularizationRate={regularizationRate}
        onRegularizationRate={setRegularizationRate}
        problem={config.problem}
        onProblem={onProblem}
        batchSize={batchSize}
        onBatchSize={setBatchSize}
      />
      <main className="flex flex-1 gap-4 p-4 bg-pg-bg">
        {/* Data column */}
        <section className="w-56 space-y-4">
          <h2 className="text-xs uppercase tracking-wide text-pg-dim">Data</h2>
          <div>
            <p className="text-xs text-pg-muted mb-1">Which dataset?</p>
            <DatasetSelector
              problem={config.problem}
              classificationDataset={config.dataset}
              regressionDataset={config.regDataset}
              onSelectClassification={onSelectClassification}
              onSelectRegression={onSelectRegression}
              thumbnails={thumbnails}
            />
          </div>
          <Slider
            label={`Ratio of train data: ${percTrainData}%`}
            value={percTrainData}
            min={10}
            max={90}
            step={1}
            onChange={onPercTrain}
          />
          <Slider
            label={`Noise: ${noise}`}
            value={noise}
            min={0}
            max={50}
            step={1}
            onChange={onNoise}
          />
          <button
            type="button"
            onClick={() => void regenerateData()}
            className="px-3 py-1.5 text-xs rounded border border-pg-border bg-pg-raised hover:bg-pg-panel text-pg-text w-full"
          >
            Regenerate data
          </button>
          <Slider
            label={`Batch size: ${batchSize}`}
            value={batchSize}
            min={1}
            max={30}
            step={1}
            onChange={setBatchSize}
          />
        </section>

        {/* Features column */}
        <section className="w-44 space-y-2">
          <h2 className="text-xs uppercase tracking-wide text-pg-dim">Features</h2>
          <p className="text-xs text-pg-muted mb-1">Which inputs?</p>
          <FeatureSelector
            active={config.features}
            boundaries={boundaries.boundaries}
            onToggle={onToggleFeature}
            xDomain={xDomain}
            yDomain={yDomain}
          />
        </section>

        {/* Network column */}
        <section className="flex-1 min-w-0 overflow-x-auto">
          <h2 className="text-xs uppercase tracking-wide text-pg-dim mb-2">
            Network
          </h2>
          {snapshot && (
            <NetworkGraph
              layers={snapshot.layers}
              links={snapshot.links}
              boundaries={boundaries.boundaries}
              xDomain={xDomain}
              yDomain={yDomain}
              features={config.features}
              onAddLayer={onAddLayer}
              onRemoveLayer={onRemoveLayer}
              onAddNeuron={onAddNeuron}
              onRemoveNeuron={onRemoveNeuron}
              selectedNodeId={selectedNodeId}
              onSelectNode={setSelectedNodeId}
              featureLabels={FEATURE_LABELS}
            />
          )}
        </section>

        {/* Output column */}
        <section className="w-80">
          <h2 className="text-xs uppercase tracking-wide text-pg-dim mb-2">
            Output
          </h2>
          <div className="space-y-2 text-xs text-pg-muted">
            <div className="flex items-center gap-3">
              <span>Test loss</span>
              <span className="font-mono text-pg-text text-sm">
                {(snapshot?.lossTest ?? 0).toFixed(3)}
              </span>
            </div>
            <div className="flex items-center gap-3">
              <span>Train loss</span>
              <span className="font-mono text-pg-text text-sm">
                {(snapshot?.lossTrain ?? 0).toFixed(3)}
              </span>
            </div>
            <LossChart history={lossHistory} width={260} />
            <div className="flex items-center gap-3 mt-2">
              <label className="flex items-center gap-1 cursor-pointer">
                <input
                  type="checkbox"
                  checked={showTestData}
                  onChange={(e) => setShowTestData(e.target.checked)}
                />
                <span>Show test data</span>
              </label>
              <label className="flex items-center gap-1 cursor-pointer">
                <input
                  type="checkbox"
                  checked={discretize}
                  onChange={(e) => onDiscretizeChange(e.target.checked)}
                />
                <span>Discretize output</span>
              </label>
            </div>
            <div className="mt-2">
              <Heatmap
                data={mainBoundary}
                xDomain={xDomain}
                yDomain={yDomain}
                size={300}
                trainData={trainData}
                testData={showTestData ? testData : undefined}
                discretize={discretize}
                showAxes
              />
              <p className="text-[11px] text-pg-dim mt-1">
                {selectedNodeId
                  ? `Decision boundary of node ${selectedNodeId}`
                  : "Decision boundary of the network"}
              </p>
            </div>
          </div>
        </section>
      </main>
      {error && (
        <div className="fixed bottom-4 right-4 max-w-sm bg-rose-950 border border-rose-700/80 text-rose-100 text-sm px-3 py-2 rounded-lg shadow-lg shadow-black/40 flex items-start gap-2">
          <span className="flex-1">{error}</span>
          <button
            type="button"
            onClick={() => setError(null)}
            className="text-rose-300/80 hover:text-rose-100"
            aria-label="dismiss"
          >
            ×
          </button>
        </div>
      )}
    </div>
  );
}

function Header() {
  return (
    <header className="flex items-center gap-3 px-4 py-2.5 bg-gradient-to-r from-violet-950 via-fuchsia-950 to-indigo-950 text-violet-50 border-b border-violet-800/60 shadow-lg shadow-black/30">
      <div className="w-8 h-8 rounded-md bg-amber-500/25 border border-amber-400/40 flex items-center justify-center text-xs font-bold text-amber-200">
        NN
      </div>
      <div>
        <h1 className="text-base font-semibold leading-tight tracking-tight">
          Neural Network Playground
        </h1>
        <p className="text-xs text-violet-300/90">
          Java backend · React + Tailwind frontend
        </p>
      </div>
    </header>
  );
}

function Slider({
  label,
  value,
  min,
  max,
  step,
  onChange,
}: {
  label: string;
  value: number;
  min: number;
  max: number;
  step?: number;
  onChange: (v: number) => void;
}) {
  return (
    <label className="block text-xs text-pg-muted space-y-1">
      <span>{label}</span>
      <input
        className="slider w-full"
        type="range"
        min={min}
        max={max}
        step={step ?? 1}
        value={value}
        onChange={(e) => onChange(Number(e.target.value))}
      />
    </label>
  );
}
