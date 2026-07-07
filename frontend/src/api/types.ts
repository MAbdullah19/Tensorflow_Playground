export type Activation = "tanh" | "relu" | "sigmoid" | "linear";
export type Regularization = "none" | "L1" | "L2";
export type Problem = "classification" | "regression";
export type ClassificationDataset = "circle" | "xor" | "gauss" | "spiral";
export type RegressionDataset = "reg-plane" | "reg-gauss";
export type FeatureKey = "x" | "y";

export interface Config {
  dataset: ClassificationDataset;
  regDataset: RegressionDataset;
  problem: Problem;
  noise: number;
  percTrainData: number;
  activation: Activation;
  regularization: Regularization;
  initZero: boolean;
  networkShape: number[];
  features: FeatureKey[];
}

export interface NodeInfo {
  id: string;
  bias: number;
}

export interface LinkInfo {
  source: string;
  dest: string;
  weight: number;
  dead: boolean;
}

/** Tuple form returned by the backend: [x, y, label]. */
export type Point = [number, number, number];

export interface Snapshot {
  sessionId: string;
  iter: number;
  lossTrain: number;
  lossTest: number;
  config: Config;
  layers: NodeInfo[][];
  links: LinkInfo[];
  trainData?: Point[];
  testData?: Point[];
}

export interface BoundaryResponse {
  density: number;
  xDomain: [number, number];
  yDomain: [number, number];
  /** Map from nodeId / featureKey -> matrix [density][density]. */
  boundaries: Record<string, number[][]>;
}
