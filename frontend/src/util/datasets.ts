import type { Point } from "../api/types";

/**
 * Tiny client-side dataset generators used purely for the thumbnails in
 * the dataset selector. They do **not** drive training - the actual training
 * data always comes from the Java backend so the visualizations stay
 * deterministic with the server.
 */

function rand(rng: () => number, a: number, b: number): number {
  return rng() * (b - a) + a;
}

function normal(rng: () => number, mean: number, variance: number): number {
  let v1 = 0,
    v2 = 0,
    s = 0;
  do {
    v1 = 2 * rng() - 1;
    v2 = 2 * rng() - 1;
    s = v1 * v1 + v2 * v2;
  } while (s > 1 || s === 0);
  return mean + Math.sqrt((-2 * Math.log(s) / s) * variance) * v1;
}

function dist(x1: number, y1: number, x2: number, y2: number) {
  const dx = x1 - x2,
    dy = y1 - y2;
  return Math.sqrt(dx * dx + dy * dy);
}

function mulberry32(seed: number): () => number {
  let a = seed | 0;
  return function () {
    a = (a + 0x6d2b79f5) | 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

const DEFAULT_SEED = 1234;

export function previewCircle(samples = 200): Point[] {
  const rng = mulberry32(DEFAULT_SEED);
  const radius = 5;
  const out: Point[] = [];
  for (let i = 0; i < samples / 2; i++) {
    const r = rand(rng, 0, radius * 0.5);
    const a = rand(rng, 0, 2 * Math.PI);
    out.push([r * Math.sin(a), r * Math.cos(a), 1]);
  }
  for (let i = 0; i < samples / 2; i++) {
    const r = rand(rng, radius * 0.7, radius);
    const a = rand(rng, 0, 2 * Math.PI);
    out.push([r * Math.sin(a), r * Math.cos(a), -1]);
  }
  return out;
}

export function previewXOR(samples = 200): Point[] {
  const rng = mulberry32(DEFAULT_SEED);
  const out: Point[] = [];
  for (let i = 0; i < samples; i++) {
    let x = rand(rng, -5, 5);
    let y = rand(rng, -5, 5);
    x += x > 0 ? 0.3 : -0.3;
    y += y > 0 ? 0.3 : -0.3;
    out.push([x, y, x * y >= 0 ? 1 : -1]);
  }
  return out;
}

export function previewGauss(samples = 200): Point[] {
  const rng = mulberry32(DEFAULT_SEED);
  const out: Point[] = [];
  for (let i = 0; i < samples / 2; i++) {
    out.push([normal(rng, 2, 0.5), normal(rng, 2, 0.5), 1]);
  }
  for (let i = 0; i < samples / 2; i++) {
    out.push([normal(rng, -2, 0.5), normal(rng, -2, 0.5), -1]);
  }
  return out;
}

export function previewSpiral(samples = 200): Point[] {
  const out: Point[] = [];
  function gen(deltaT: number, label: number) {
    for (let i = 0; i < samples / 2; i++) {
      const r = (i / (samples / 2)) * 5;
      const t = ((1.75 * i) / (samples / 2)) * 2 * Math.PI + deltaT;
      out.push([r * Math.sin(t), r * Math.cos(t), label]);
    }
  }
  gen(0, 1);
  gen(Math.PI, -1);
  return out;
}

export function previewRegPlane(samples = 200): Point[] {
  const rng = mulberry32(DEFAULT_SEED);
  const out: Point[] = [];
  for (let i = 0; i < samples; i++) {
    const x = rand(rng, -6, 6);
    const y = rand(rng, -6, 6);
    const lab = Math.max(-1, Math.min(1, (x + y) / 10));
    out.push([x, y, lab]);
  }
  return out;
}

export function previewRegGauss(samples = 200): Point[] {
  const rng = mulberry32(DEFAULT_SEED);
  const gaussians: Array<[number, number, number]> = [
    [-4, 2.5, 1],
    [0, 2.5, -1],
    [4, 2.5, 1],
    [-4, -2.5, -1],
    [0, -2.5, 1],
    [4, -2.5, -1],
  ];
  const out: Point[] = [];
  for (let i = 0; i < samples; i++) {
    const x = rand(rng, -6, 6);
    const y = rand(rng, -6, 6);
    let label = 0;
    for (const [cx, cy, sign] of gaussians) {
      const d = dist(x, y, cx, cy);
      const bell = Math.max(0, Math.min(1, 1 - d / 2));
      const newLabel = sign * bell;
      if (Math.abs(newLabel) > Math.abs(label)) label = newLabel;
    }
    out.push([x, y, label]);
  }
  return out;
}

export function buildAllThumbnails(): Record<string, Point[]> {
  return {
    circle: previewCircle(),
    xor: previewXOR(),
    gauss: previewGauss(),
    spiral: previewSpiral(),
    "reg-plane": previewRegPlane(),
    "reg-gauss": previewRegGauss(),
  };
}
