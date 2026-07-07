import type { BoundaryResponse, Config, Snapshot } from "./types";

// In dev, VITE_API_BASE is unset -> "/api" is proxied to the backend by Vite.
// In production, set VITE_API_BASE to the backend's origin
// (e.g. https://nn-playground-backend.onrender.com) so calls go cross-origin.
const API_ROOT = import.meta.env.VITE_API_BASE ?? "";
const BASE = `${API_ROOT}/api`;

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(BASE + path, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...(init?.headers ?? {}),
    },
  });
  if (!res.ok) {
    let detail = res.statusText;
    try {
      const body = (await res.json()) as { error?: string };
      if (body && body.error) detail = body.error;
    } catch {
      /* ignore */
    }
    throw new Error(`HTTP ${res.status}: ${detail}`);
  }
  return (await res.json()) as T;
}

export const api = {
  createSession: (config: Partial<Config>) =>
    request<Snapshot>("/sessions", {
      method: "POST",
      body: JSON.stringify(config ?? {}),
    }),
  deleteSession: (id: string) =>
    request<{ deleted: true }>(`/sessions/${id}`, { method: "DELETE" }),
  configure: (id: string, patch: Partial<Config>) =>
    request<Snapshot>(`/sessions/${id}/configure`, {
      method: "POST",
      body: JSON.stringify(patch),
    }),
  regenerateData: (id: string, patch?: Partial<Config>) =>
    request<Snapshot>(`/sessions/${id}/regenerate-data`, {
      method: "POST",
      body: JSON.stringify(patch ?? {}),
    }),
  buildNetwork: (id: string, patch?: Partial<Config>) =>
    request<Snapshot>(`/sessions/${id}/build-network`, {
      method: "POST",
      body: JSON.stringify(patch ?? {}),
    }),
  train: (
    id: string,
    body: {
      epochs: number;
      learningRate: number;
      regularizationRate: number;
      batchSize: number;
    },
  ) =>
    request<Snapshot>(`/sessions/${id}/train`, {
      method: "POST",
      body: JSON.stringify(body),
    }),
  boundary: (
    id: string,
    body: { density: number; discretize?: boolean; nodeIds?: string[] },
  ) =>
    request<BoundaryResponse>(`/sessions/${id}/boundary`, {
      method: "POST",
      body: JSON.stringify(body),
    }),
  setWeight: (
    id: string,
    body: { source: string; dest: string; weight: number },
  ) =>
    request<Snapshot>(`/sessions/${id}/weight`, {
      method: "POST",
      body: JSON.stringify(body),
    }),
  setBias: (id: string, body: { id: string; bias: number }) =>
    request<Snapshot>(`/sessions/${id}/bias`, {
      method: "POST",
      body: JSON.stringify(body),
    }),
};
