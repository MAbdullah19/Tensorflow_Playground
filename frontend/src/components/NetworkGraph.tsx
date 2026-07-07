import { useMemo, useState } from "react";
import type { LinkInfo, NodeInfo } from "../api/types";
import { valueToHex, clamp } from "../util/colors";
import { Heatmap } from "./Heatmap";

export interface NetworkGraphProps {
  layers: NodeInfo[][];
  links: LinkInfo[];
  /** node id -> 2D boundary matrix used for the small heatmap inside each node. */
  boundaries: Record<string, number[][]>;
  xDomain: [number, number];
  yDomain: [number, number];
  features: string[];
  onAddLayer: () => void;
  onRemoveLayer: () => void;
  onAddNeuron: (layerIdx: number) => void;
  onRemoveNeuron: (layerIdx: number) => void;
  selectedNodeId: string | null;
  onSelectNode: (id: string | null) => void;
  /** Pretty label per feature key. */
  featureLabels: Record<string, string>;
}

const NODE_SIZE = 40;
const COLUMN_GAP = 110;
const NODE_GAP = 12;

interface Geometry {
  width: number;
  height: number;
  columns: Array<{ x: number; nodes: Array<{ id: string; y: number }> }>;
  linkLines: Array<{
    sx: number;
    sy: number;
    tx: number;
    ty: number;
    weight: number;
    dead: boolean;
    source: string;
    dest: string;
  }>;
}

function computeGeometry(layers: NodeInfo[][]): Geometry {
  const numLayers = layers.length;
  const columns: Geometry["columns"] = [];
  let maxNodes = 0;
  for (const layer of layers) maxNodes = Math.max(maxNodes, layer.length);
  const height = maxNodes * NODE_SIZE + (maxNodes - 1) * NODE_GAP + 10;
  const width = numLayers * NODE_SIZE + (numLayers - 1) * COLUMN_GAP + 10;

  for (let li = 0; li < numLayers; li++) {
    const layer = layers[li];
    const x = li * (NODE_SIZE + COLUMN_GAP) + 5;
    const layerHeight = layer.length * NODE_SIZE + (layer.length - 1) * NODE_GAP;
    const yStart = (height - layerHeight) / 2;
    const nodes = layer.map((n, i) => ({
      id: n.id,
      y: yStart + i * (NODE_SIZE + NODE_GAP),
    }));
    columns.push({ x, nodes });
  }

  return { width, height, columns, linkLines: [] };
}

export function NetworkGraph({
  layers,
  links,
  boundaries,
  xDomain,
  yDomain,
  features,
  onAddLayer,
  onRemoveLayer,
  onAddNeuron,
  onRemoveNeuron,
  selectedNodeId,
  onSelectNode,
  featureLabels,
}: NetworkGraphProps) {
  const [hovered, setHovered] = useState<{
    type: "weight" | "bias";
    value: number;
    x: number;
    y: number;
  } | null>(null);

  const geometry = useMemo(() => computeGeometry(layers), [layers]);

  const nodePositions = useMemo(() => {
    const pos: Record<string, { x: number; y: number }> = {};
    geometry.columns.forEach((col) => {
      for (const n of col.nodes) {
        pos[n.id] = { x: col.x + NODE_SIZE / 2, y: n.y + NODE_SIZE / 2 };
      }
    });
    return pos;
  }, [geometry]);

  // Build link path data using cubic curves for smoothness.
  const linkPaths = useMemo(() => {
    return links.map((link) => {
      const s = nodePositions[link.source];
      const t = nodePositions[link.dest];
      if (!s || !t) return null;
      const sx = s.x + NODE_SIZE / 2 - 1;
      const sy = s.y;
      const tx = t.x - NODE_SIZE / 2 + 1;
      const ty = t.y;
      const mid = (sx + tx) / 2;
      const path = `M${sx},${sy} C${mid},${sy} ${mid},${ty} ${tx},${ty}`;
      const width = clamp(Math.abs(link.weight) * 2 + 0.5, 0.5, 8);
      return { ...link, path, width, color: valueToHex(link.weight) };
    });
  }, [links, nodePositions]);

  return (
    <div className="relative">
      <svg
        width={geometry.width}
        height={Math.max(geometry.height, 220)}
        className="block"
      >
        {/* Links */}
        <g>
          {linkPaths.map(
            (lp) =>
              lp && (
                <g key={`${lp.source}-${lp.dest}`}>
                  <path
                    d={lp.path}
                    fill="none"
                    stroke={lp.dead ? "#57534e" : lp.color}
                    strokeWidth={lp.width}
                    strokeOpacity={lp.dead ? 0.4 : 0.85}
                    style={{ pointerEvents: "stroke" }}
                  />
                  <path
                    d={lp.path}
                    fill="none"
                    stroke="transparent"
                    strokeWidth={Math.max(lp.width + 4, 8)}
                    onMouseEnter={(e) => {
                      const rect =
                        (e.currentTarget.ownerSVGElement as SVGSVGElement).getBoundingClientRect();
                      setHovered({
                        type: "weight",
                        value: lp.weight,
                        x: e.clientX - rect.left,
                        y: e.clientY - rect.top,
                      });
                    }}
                    onMouseLeave={() => setHovered(null)}
                    style={{ cursor: "pointer" }}
                  />
                </g>
              ),
          )}
        </g>

        {/* Nodes */}
        {layers.map((layer, li) => (
          <g key={li}>
            {layer.map((node) => {
              const pos = nodePositions[node.id];
              if (!pos) return null;
              const isInput = li === 0;
              const isOutput = li === layers.length - 1;
              const x = pos.x - NODE_SIZE / 2;
              const y = pos.y - NODE_SIZE / 2;
              const selected = selectedNodeId === node.id;

              const hasBoundary = boundaries[node.id];
              return (
                <g
                  key={node.id}
                  transform={`translate(${x},${y})`}
                  onMouseEnter={() => onSelectNode(node.id)}
                  onMouseLeave={() => onSelectNode(null)}
                  style={{ cursor: "pointer" }}
                >
                  <rect
                    width={NODE_SIZE}
                    height={NODE_SIZE}
                    rx={4}
                    fill="#1f1830"
                    stroke={selected ? "#f59e0b" : "#6b5b8c"}
                    strokeWidth={selected ? 2 : 1}
                  />
                  {!isInput && (
                    <rect
                      x={-7}
                      y={NODE_SIZE - 5}
                      width={5}
                      height={5}
                      fill={valueToHex(node.bias)}
                      stroke="#6b5b8c"
                      onMouseEnter={(e) => {
                        e.stopPropagation();
                        const rect =
                          (e.currentTarget.ownerSVGElement as SVGSVGElement).getBoundingClientRect();
                        setHovered({
                          type: "bias",
                          value: node.bias,
                          x: e.clientX - rect.left,
                          y: e.clientY - rect.top,
                        });
                      }}
                      onMouseLeave={() => setHovered(null)}
                    />
                  )}
                  {isInput && (
                    <text
                      x={-8}
                      y={NODE_SIZE / 2 + 4}
                      textAnchor="end"
                      fontSize={11}
                      fill="#ddd6fe"
                    >
                      {featureLabels[node.id] ?? node.id}
                    </text>
                  )}
                  {isOutput && (
                    <text
                      x={NODE_SIZE + 8}
                      y={NODE_SIZE / 2 + 4}
                      textAnchor="start"
                      fontSize={11}
                      fill="#ddd6fe"
                    >
                      output
                    </text>
                  )}
                  {hasBoundary && (
                    <foreignObject
                      x={2}
                      y={2}
                      width={NODE_SIZE - 4}
                      height={NODE_SIZE - 4}
                    >
                      <Heatmap
                        data={hasBoundary}
                        size={NODE_SIZE - 4}
                        xDomain={xDomain}
                        yDomain={yDomain}
                        showPoints={false}
                      />
                    </foreignObject>
                  )}
                </g>
              );
            })}
          </g>
        ))}
      </svg>

      {/* Plus / minus controls per hidden layer */}
      <div
        className="absolute top-0 left-0 pointer-events-none"
        style={{ width: geometry.width, height: geometry.height }}
      >
        {layers.slice(1, -1).map((layer, idx) => {
          const li = idx + 1;
          const col = geometry.columns[li];
          return (
            <div
              key={li}
              className="absolute pointer-events-auto"
              style={{ left: col.x - 12, top: 0 }}
            >
              <div className="flex flex-col items-center">
                <div className="flex gap-1">
                  <button
                    type="button"
                    aria-label={`add neuron to layer ${li}`}
                    className="w-6 h-6 rounded-full bg-pg-raised border border-pg-border hover:bg-violet-950/60 text-pg-muted text-base leading-none flex items-center justify-center"
                    onClick={() => onAddNeuron(li - 1)}
                    disabled={layer.length >= 16}
                  >
                    +
                  </button>
                  <button
                    type="button"
                    aria-label={`remove neuron from layer ${li}`}
                    className="w-6 h-6 rounded-full bg-pg-raised border border-pg-border hover:bg-violet-950/60 text-pg-muted text-base leading-none flex items-center justify-center"
                    onClick={() => onRemoveNeuron(li - 1)}
                    disabled={layer.length <= 1}
                  >
                  </button>
                </div>
                <div className="text-xs text-pg-muted mt-1 whitespace-nowrap">
                  {layer.length} neuron{layer.length !== 1 ? "s" : ""}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Layer count controls */}
      <div className="flex items-center gap-2 mt-3">
        <button
          type="button"
          onClick={onAddLayer}
          className="px-2 py-1 text-xs rounded border border-pg-border bg-pg-raised hover:bg-violet-950/50 text-pg-text"
          disabled={layers.length - 2 >= 6}
        >
          + Add layer
        </button>
        <button
          type="button"
          onClick={onRemoveLayer}
          className="px-2 py-1 text-xs rounded border border-pg-border bg-pg-raised hover:bg-violet-950/50 text-pg-text"
          disabled={layers.length - 2 <= 0}
        >
          − Remove layer
        </button>
        <span className="text-xs text-pg-dim">
          {layers.length - 2} hidden layer{layers.length - 2 !== 1 ? "s" : ""}
          {features.length > 0 && (
            <>
              {" "}
              ({features.length} feature{features.length !== 1 ? "s" : ""})
            </>
          )}
        </span>
      </div>

      {hovered && (
        <div
          className="absolute pointer-events-none bg-violet-950/95 border border-pg-border text-pg-text text-xs px-2 py-1 rounded-md shadow-lg"
          style={{
            left: hovered.x + 14,
            top: hovered.y - 12,
            whiteSpace: "nowrap",
          }}
        >
          {hovered.type === "weight" ? "weight" : "bias"}:{" "}
          {hovered.value.toFixed(3)}
        </div>
      )}
    </div>
  );
}
