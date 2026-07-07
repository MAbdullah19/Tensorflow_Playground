# Neural Network Playground

An interactive, from-scratch reimplementation of the classic
[TensorFlow Playground](https://playground.tensorflow.org/). The neural-network
engine, datasets, and training loop run in **Java 11** with zero third-party
dependencies; a **React 18 + Vite + Tailwind** single-page app draws the network,
the live decision boundary, and the loss curves.

Built as an **Object-Oriented Programming course project**, so the Java engine is
written to showcase OOP principles (encapsulation, inheritance, polymorphism,
composition, interfaces, and common design patterns) rather than to be maximally
terse. See [PROJECT_DOCUMENTATION.md](PROJECT_DOCUMENTATION.md) for a full
concept-by-concept walkthrough and a plain-English neural-network glossary.

## Features

- 4 classification datasets (circle, XOR, gaussian, spiral) and 2 regression
  datasets (plane, multi-gaussian)
- 0 to 6 hidden layers, each with 1 to 8 neurons, configurable per layer
- 4 activations (Tanh, ReLU, Sigmoid, Linear) and 3 regularisers (None, L1, L2)
- Live decision-boundary heat-map, per-node mini heat-maps, and a train/test
  loss line chart
- Adjustable learning rate, regularisation rate, batch size, noise, and
  train/test split
- Play, Pause, Step, Reset, "Regenerate data", and "Discretise output"
- Seeded randomness, so the same dataset and initial weights reproduce exactly

## Architecture

The frontend does no maths. It is a thin client that sends the current UI
configuration to the backend and renders whatever snapshot comes back.

```
 ┌──────────────┐   POST /api/sessions/{id}/train  { lr, regRate, batch, epochs }   ┌──────────────┐
 │   React UI   │  ───────────────────────────────────────────────────────────────▶ │  Java engine │
 │ (frontend/)  │                                                                    │  (backend/)  │
 │              │  ◀─────────────────────────────────────────────────────────────── │              │
 └──────────────┘   snapshot { iter, lossTrain, lossTest, layers, links, ... }       └──────────────┘
```

- **Sessions.** Each browser gets its own server-side `Session` holding its
  dataset, network, and training counters. `SessionManager` locks each session,
  evicts it after 30 minutes idle, and caps the server at 256 sessions.
- **Play loop.** Pressing Play fires `POST .../train` with `epochs=1` repeatedly,
  so any slider or drop-down change takes effect on the very next step.
- **One epoch** runs a forward pass, a backward pass that accumulates gradients,
  then a batched gradient-descent weight update every `batchSize` examples,
  followed by a regularisation step. Train and test loss are recomputed after.

## Repository layout

```
OOP_Neural_Network_Playground/
├── backend/      Java 11 REST engine (zero dependencies)
│   └── src/main/java/com/playground/
│       ├── Main.java            process entry point
│       ├── nn/                  neural-network core (Node, Link, NeuralNetwork)
│       │   ├── activation/      Tanh / ReLU / Sigmoid / Linear (+ registry)
│       │   └── regularization/  None / L1 / L2 (+ registry)
│       ├── data/                datasets and input features
│       ├── api/                 HTTP server, routing, sessions
│       ├── exceptions/          typed exception hierarchy
│       └── util/Json.java       hand-written JSON encoder/decoder
├── frontend/     React 18 + Vite + Tailwind UI
│   └── src/
│       ├── api/                 typed fetch wrapper (client.ts, types.ts)
│       ├── components/          controls, network graph, heatmap, loss chart
│       └── util/                colour maps, dataset thumbnails, debounce
├── playground/   Original TensorFlow Playground (TypeScript), read-only reference
└── PROJECT_DOCUMENTATION.md
```

## Getting started

**Prerequisites:** JDK 11 or newer (no Maven or Gradle; it compiles with plain
`javac`) and Node.js 18 or newer.

### 1. Backend

```bash
cd backend
./build.sh      # build.bat on Windows
./run.sh        # run.bat on Windows  -> http://localhost:8080
```

Pass `--port=NNNN` or set the `PORT` env var to override the default `8080`.

### 2. Frontend (separate terminal)

```bash
cd frontend
npm install
npm run dev     # -> http://localhost:5173
```

Open <http://localhost:5173>. Smoke-test the engine with
`curl http://localhost:8080/health`, which returns `{"status":"ok"}`. Pick
**circle**, press **Play**, and the loss should fall from ~0.5 to under 0.01
within roughly 50 epochs.

## REST API

All endpoints accept and return `application/json`. CORS is enabled for any origin.

| Method | Path | Description |
| ------ | ---- | ----------- |
| GET    | `/health` | Returns `{"status":"ok"}` |
| POST   | `/api/sessions` | Creates a session and returns an initial snapshot |
| DELETE | `/api/sessions/{id}` | Deletes a session |
| GET    | `/api/sessions/{id}/state` | Returns the current snapshot |
| POST   | `/api/sessions/{id}/configure` | Updates config; rebuilds network/data as needed |
| POST   | `/api/sessions/{id}/regenerate-data` | Re-rolls the dataset |
| POST   | `/api/sessions/{id}/build-network` | Re-rolls the network weights |
| POST   | `/api/sessions/{id}/train` | Runs `epochs` mini-batch SGD epochs |
| POST   | `/api/sessions/{id}/boundary` | Computes the decision boundary on a grid |
| POST   | `/api/sessions/{id}/weight` | Manually overrides one weight |
| POST   | `/api/sessions/{id}/bias` | Manually overrides one bias |

## OOP design highlights

| OOP concept | Where to see it |
| ----------- | --------------- |
| Interface / abstraction | `DifferentiableFunction`, `ErrorFunction`, `Trainable` |
| Abstract class | `ActivationFunction`, `DataGenerator` |
| Inheritance | `TanhActivation extends ActivationFunction`, six `...Dataset extends DataGenerator` |
| Polymorphism | `Node.updateOutput()` calls `activation.output(sum)`, no `if`/`switch` |
| Encapsulation | every field in `Node`/`Link`/`Session` is `private` |
| Composition | a `Node` has-a `ActivationFunction` and a list of `Link`s |
| Static factory | `Activations.byKey("relu")`, `Regularizations.byKey("L2")` |
| Strategy pattern | activation, regularisation, and dataset are swappable objects |
| Singleton | `SquaredError.INSTANCE` |
| Typed exceptions | `PlaygroundException` maps each error type to an HTTP status |

## Deployment (free tier)

The backend is a stateful long-lived process, so it needs an always-on container
rather than a serverless function.

- **Backend on Render:** New Web Service, root directory `backend`, runtime Docker
  (uses `backend/Dockerfile`). Render injects `PORT`, which `Main.java` reads.
- **Frontend on Vercel:** New Project, root directory `frontend` (framework
  auto-detects as Vite). `frontend/vercel.json` proxies `/api` and `/health` to
  the Render backend, so the relative API calls work with no CORS setup. Replace
  the placeholder host in `vercel.json` with your Render URL before deploying.

In production generally, run `npm run build` and serve `frontend/dist` from any
static host, pointing the `/api` path at the Java backend.
