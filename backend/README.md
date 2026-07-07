# Playground Backend (Java)

A zero-dependency Java port of the original Tensorflow Playground neural-network engine.
Exposes a small REST API consumed by the React frontend.

## Requirements

- JDK 11 or newer (built and tested with Temurin 17)
- No build tool required: the project compiles with plain `javac` using
  `com.sun.net.httpserver` from the standard library.

## Quick start

```bash
# from the backend/ folder
./build.sh        # or build.bat on Windows
./run.sh          # or run.bat on Windows
# pass --port=NNNN or set PORT=NNNN to override the default 8080
```

You should see:

```
Playground backend listening on http://localhost:8080
```

## Layout

```
backend/
  src/main/java/com/playground/
    Main.java                          -- entry point
    api/                               -- HTTP layer + sessions
      ApiServer.java
      RequestHandlers.java
      Session.java                     -- implements Trainable
      SessionManager.java
      Trainable.java                   -- interface
    exceptions/                        -- custom exception hierarchy
      PlaygroundException.java         -- abstract base, holds HTTP status
      ConfigurationException.java      -- 400
      NotFoundException.java           -- 404
      SessionNotFoundException.java    -- 404 (extends NotFoundException)
      TrainingException.java           -- 500
    nn/                                -- neural-network core
      DifferentiableFunction.java      -- interface
      ErrorFunction.java               -- interface
      SquaredError.java                -- ErrorFunction implementation
      Node.java                        -- encapsulated neuron
      Link.java                        -- encapsulated weighted edge
      NeuralNetwork.java               -- orchestrator
      activation/                      -- ActivationFunction hierarchy
        ActivationFunction.java        -- abstract class
        TanhActivation.java
        ReluActivation.java
        SigmoidActivation.java
        LinearActivation.java
        Activations.java               -- registry / factory
      regularization/                  -- RegularizationFunction hierarchy
        RegularizationFunction.java    -- abstract class
        NoRegularization.java
        L1Regularization.java
        L2Regularization.java
        Regularizations.java           -- registry / factory
    data/                              -- datasets and feature engineering
      Example2D.java                   -- immutable value object
      InputFeatures.java
      datasets/                        -- DataGenerator hierarchy
        DataGenerator.java             -- abstract class
        CircleDataset.java
        XorDataset.java
        GaussianDataset.java
        SpiralDataset.java
        PlaneRegressionDataset.java
        GaussianRegressionDataset.java
        DatasetRegistry.java           -- registry / factory
    util/
      Json.java                        -- minimal JSON encoder / decoder
```

> See **[OOP_DESIGN.md](OOP_DESIGN.md)** for a concept-by-concept walkthrough
> (encapsulation, inheritance, polymorphism, exception hierarchy, ...) tied
> to specific files in this tree.

## REST API

All endpoints accept and return `application/json`. CORS is enabled for any origin.

| Method | Path | Description |
| ------ | ---- | ----------- |
| GET    | `/health` | Returns `{"status":"ok"}` |
| POST   | `/api/sessions` | Creates a session and returns an initial snapshot. The body may include any of the configuration fields listed below. |
| DELETE | `/api/sessions/{id}` | Deletes a session. |
| GET    | `/api/sessions/{id}/state` | Returns the current snapshot. |
| POST   | `/api/sessions/{id}/configure` | Updates the configuration. Will rebuild the network and / or regenerate data when needed. |
| POST   | `/api/sessions/{id}/regenerate-data` | Re-rolls the dataset (changes the seed unless `dataSeed` is provided). |
| POST   | `/api/sessions/{id}/build-network` | Re-rolls the network weights (changes the seed unless `networkSeed` is provided). |
| POST   | `/api/sessions/{id}/train` | Runs `epochs` mini-batch SGD epochs. Body: `{epochs, learningRate, regularizationRate, batchSize}`. |
| POST   | `/api/sessions/{id}/boundary` | Computes the decision boundary on a dense grid. Body: `{density, discretize, nodeIds}`. |
| POST   | `/api/sessions/{id}/weight` | Manually overrides one weight. Body: `{source, dest, weight}`. |
| POST   | `/api/sessions/{id}/bias`   | Manually overrides one bias. Body: `{id, bias}`. |

### Configuration fields

| field | type | notes |
| ----- | ---- | ----- |
| `dataset` | `circle` \| `xor` \| `gauss` \| `spiral` | classification dataset |
| `regDataset` | `reg-plane` \| `reg-gauss` | regression dataset |
| `problem` | `classification` \| `regression` | |
| `noise` | number 0 - 50 | as a percentage like the original UI |
| `percTrainData` | number 10 - 90 | percentage of data used for training |
| `activation` | `tanh` \| `relu` \| `sigmoid` \| `linear` | hidden layer activation |
| `regularization` | `none` \| `L1` \| `L2` | weight regularization |
| `initZero` | boolean | initialise weights/biases to 0 |
| `networkShape` | int[]  | sizes of hidden layers (each in `[1, 16]`, max 8 hidden layers) |
| `features` | string[] | `x` and/or `y` (2D input coordinates) |

### Snapshot shape

```jsonc
{
  "sessionId": "...",
  "iter": 0,
  "lossTrain": 0.52, "lossTest": 0.51,
  "config": { ... },
  "layers": [ [ { "id": "x", "bias": 0 }, ... ], ... ],
  "links": [ { "source": "x", "dest": "1", "weight": -0.36, "dead": false }, ... ],
  "trainData": [ [x, y, label], ... ],          // only on session creation
  "testData":  [ [x, y, label], ... ]
}
```
