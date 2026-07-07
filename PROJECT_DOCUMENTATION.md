# Neural Network Playground — Project Documentation

> A from-scratch, zero-dependency reimplementation of the classic
> [TensorFlow Playground](https://playground.tensorflow.org/). The neural-network
> engine, datasets, and training loop run in **Java 11**; a **React + Tailwind**
> single-page app draws the network, the decision boundary, and the loss curves.
>
> This document has two parts:
> 1. **Part A — Project Information**: what the project is, how it is structured,
>    how the pieces talk to each other, and how to run it.
> 2. **Part B — Neural-Network Glossary**: a concise, plain-English explanation of
>    *every* term and concept the network uses, mapped to the exact class/method
>    that implements it.

---

# Part A — Project Information

## A.1 What this project is

The project is an **interactive neural-network simulator**. You pick a dataset (a
cloud of 2-D points, each labelled `+1` or `−1`), design a small neural network by
choosing how many layers and neurons it has, choose its activation function and
regulariser, then press **Play**. The network trains in real time and you watch:

- the **decision boundary** (the coloured background) reshape itself to separate
  the two classes,
- each neuron's **individual output map**,
- the **weights** (edge thickness/colour) and **biases** change every frame,
- and the **training / test loss** fall on a live line chart.

It is a *teaching* tool: the whole point is to *see* what forward-propagation,
back-propagation, gradient descent, activations, and regularisation actually do.

Because it was built for an **OOP (Object-Oriented Programming) course project**,
the Java engine is deliberately written to showcase OOP principles — encapsulation,
inheritance, polymorphism, composition, interfaces, and common design patterns —
rather than to be maximally terse. Almost every class carries a Javadoc block that
names the OOP concept it demonstrates.

## A.2 Repository layout

```
OOP_Neural_Network_Playground/
├── backend/          # Java 11 REST engine — the "brain" (zero dependencies)
│   └── src/main/java/com/playground/
│       ├── Main.java                 # process entry point, boots the API server
│       ├── nn/                       # the neural-network engine itself
│       │   ├── NeuralNetwork.java    #   build / forwardProp / backProp / updateWeights
│       │   ├── Node.java             #   a single neuron
│       │   ├── Link.java             #   a weighted edge between two neurons
│       │   ├── DifferentiableFunction.java   # interface: output(x) + derivative(x)
│       │   ├── ErrorFunction.java    #   interface for loss functions
│       │   ├── SquaredError.java     #   the one shipped loss (½·(o−t)²)
│       │   ├── activation/           #   Tanh / ReLU / Sigmoid / Linear (+ registry)
│       │   └── regularization/       #   None / L1 / L2 (+ registry)
│       ├── data/                     # datasets and input features
│       │   ├── Example2D.java        #   one labelled (x, y) point
│       │   ├── InputFeatures.java    #   X₁ / X₂ input transforms
│       │   └── datasets/             #   Circle / XOR / Gaussian / Spiral / 2 regression
│       ├── api/                      # HTTP layer
│       │   ├── ApiServer.java        #   wraps com.sun.net.httpserver
│       │   ├── RequestHandlers.java  #   endpoint routing + JSON shaping
│       │   ├── Session.java          #   one user's dataset + network + train state
│       │   ├── SessionManager.java   #   per-session locks, idle timeout, cap
│       │   └── Trainable.java        #   interface a "thing you can train" satisfies
│       ├── exceptions/               # typed exception hierarchy
│       └── util/Json.java            # hand-written JSON encoder/decoder (no libs)
│
├── frontend/         # React 18 + Vite + Tailwind — the "face"
│   └── src/
│       ├── api/client.ts, types.ts   # typed fetch wrapper around the backend
│       └── util/                     # colour maps, dataset thumbnails, debounce
│
├── playground/       # Original TensorFlow Playground (TypeScript) — read-only reference
└── README.md
```

## A.3 Architecture and data flow

The frontend never does any maths. It is a thin client that sends the current UI
configuration to the backend and renders whatever snapshot comes back.

```
 ┌──────────────┐   POST /api/sessions/{id}/train  { lr, regRate, batch, epochs }   ┌──────────────┐
 │   React UI   │  ───────────────────────────────────────────────────────────────▶ │  Java engine │
 │ (frontend/)  │                                                                    │  (backend/)  │
 │              │  ◀─────────────────────────────────────────────────────────────── │              │
 └──────────────┘   snapshot { iter, lossTrain, lossTest, layers, links, ... }       └──────────────┘
```

- **Session model.** Each browser gets a `Session` on the server holding *its own*
  dataset, network, and training counters. `SessionManager` guards each session with
  a lock, evicts it after 30 minutes idle, and caps the server at 256 sessions so it
  cannot grow without bound.
- **The play loop.** Pressing Play makes the UI fire `POST …/train` with `epochs=1`
  over and over. Doing one epoch per request means any slider or drop-down change
  (learning rate, activation, dataset…) takes effect on the *very next* step.
- **Decision boundary.** The UI asks for a grid of network outputs and paints it onto
  a single `<canvas>` via `putImageData` — the fast path the original used. To keep
  traffic sane it only refetches the boundary every few steps.
- **Reproducibility.** A `Session` stores a `dataSeed` and a `networkSeed`; every
  random draw goes through a seeded `java.util.Random`, so the same seeds reproduce
  the same dataset and the same initial weights exactly.

## A.4 The training pipeline (what one "epoch" does)

`Session.trainOneEpoch(learningRate, regularizationRate, batchSize)` is the heart of
the loop (`api/Session.java`). For each training example it:

1. **Forward pass** — `NeuralNetwork.forwardProp` pushes the point's features through
   every layer and reads the single output.
2. **Backward pass** — `NeuralNetwork.backProp` compares the output to the label and
   walks the error backwards, *accumulating* gradients on every node and link.
3. **Batched weight update** — every `batchSize` examples,
   `NeuralNetwork.updateWeights` applies one gradient-descent step and then the
   regularisation step, and clears the accumulators.

After the epoch it recomputes train and test loss so the UI can plot them.

## A.5 How to run it

**Prerequisites:** JDK 11+ (no Maven/Gradle needed — it compiles with plain `javac`)
and Node.js 18+.

```bash
# 1. Backend
cd backend
./build.sh      # build.bat on Windows
./run.sh        # run.bat on Windows  → http://localhost:8080

# 2. Frontend (separate terminal)
cd frontend
npm install
npm run dev     # → http://localhost:5173
```

Open <http://localhost:5173>. Smoke-test the engine with `curl http://localhost:8080/health`
→ `{"status":"ok"}`. Pick **circle**, press **Play**, and the loss should fall from
~0.5 to <0.01 within ~50 epochs.

## A.6 OOP design highlights (why the code looks the way it does)

| OOP concept | Where to see it | What it buys us |
|---|---|---|
| **Interface / abstraction** | `DifferentiableFunction`, `ErrorFunction`, `Trainable` | The engine depends on a *contract* (`output`/`derivative`), not on concrete classes. |
| **Abstract class** | `ActivationFunction`, `DataGenerator` | Shares a name/key + helpers while forcing subclasses to fill in the maths. |
| **Inheritance** | `TanhActivation extends ActivationFunction`, six `…Dataset extends DataGenerator` | New activations/datasets are "just add a subclass". |
| **Polymorphism** | `Node.updateOutput()` calls `activation.output(sum)` | The node never asks *which* activation it has; the JVM dispatches. No `if/switch`. |
| **Encapsulation** | every field in `Node`/`Link`/`Session` is `private` | Training state is mutated only through named methods (`applyBiasUpdate`, …), never raw setters. |
| **Composition ("has-a")** | `Node` *has-a* `ActivationFunction` and a list of `Link`s | A neuron is *assembled from* parts, not a subclass of them. |
| **Static factory** | `Activations.byKey("relu")`, `Regularizations.byKey("L2")` | Callers pass a string key and stay decoupled from concrete types. |
| **Strategy pattern** | activation, regularisation, and dataset choices are swappable objects | Behaviour is selected at runtime by swapping the plugged-in object. |
| **Singleton (stateless)** | `SquaredError.INSTANCE` | One shared instance since the class holds no state. |
| **Method overloading** | `trainOneEpoch(lr, reg, batch)` vs `trainOneEpoch()` | Same name, convenient default-argument variant. |
| **Typed exception hierarchy** | `PlaygroundException` → `ConfigurationException`, `TrainingException`, `NotFoundException`, `SessionNotFoundException` | The API can map each error type to the right HTTP status. |

---

# Part B — Neural-Network Glossary

Everything below is explained in plain language and tied to the exact place it lives
in this codebase. Read it top-to-bottom and you will understand the whole engine.

## B.1 Structural building blocks

### Neuron / Node
The basic computing unit, implemented by **`nn/Node.java`**. A node takes several
numbers in, combines them into a single number (its **output**), and passes that
forward. Each node owns: a **bias**, an **activation function**, a list of incoming
links, and a list of outgoing links. Formula for its output:

```
output = activation( bias + Σ (weightᵢ × inputᵢ) )
```

### Link / Edge / Weight
A one-directional wire from a source node to a destination node, implemented by
**`nn/Link.java`**. Its single most important field is the **weight** — a number that
says "how strongly does the source's output influence the destination?" Learning =
finding good weights. A link also caches gradient information during training.

### Weight
The multiplier on a link. Positive weight = the source *excites* the destination;
negative = it *inhibits* it; near-zero = it barely matters. Initialised to a small
random value in `(−0.5, 0.5)` (`Link` constructor) so different neurons start out
different — this "symmetry breaking" is why the network can learn distinct features.

### Bias
A per-node constant added *before* the activation (`Node.bias`). It lets a neuron
shift its activation left or right — i.e. decide *how easily* it fires, independent of
its inputs. Think of it as the neuron's built-in "eagerness". Starts at `0.1` (or `0`
if "initialise to zero" is on).

### Layer
A column of nodes. This engine builds a **fully-connected feed-forward** network as a
`List<List<Node>>` (`NeuralNetwork.buildNetwork`):
- **Input layer** — holds the raw features (here X₁ and X₂); does no computation.
- **Hidden layers** — 0 to 6 of them, 1–8 neurons each, where the network builds up
  its internal representation.
- **Output layer** — a single node producing the final prediction.

### Fully-connected (dense)
Every node in a layer receives a link from *every* node in the previous layer. The
build loop wires this up: for each new node it creates one `Link` per node in the
previous layer.

### Feed-forward
Signals flow strictly one way, input → output, with no loops. Contrast with recurrent
networks. `forwardProp` simply walks the layers in order.

### Network shape
An integer array giving the size of each layer, e.g. `{2, 4, 2, 1}` = 2 inputs, two
hidden layers of 4 and 2, one output. Configured per-session via
`Session.setNetworkShape` (validated: ≤8 hidden layers, each 1–16 nodes).

### Input feature
A number fed into the input layer. Here the two features are just the raw coordinates
**X₁ = x** and **X₂ = y** (`data/InputFeatures.java`). Each `Feature` enum constant
carries its own transformation function — a compact Strategy pattern.

## B.2 The forward pass (making a prediction)

### Forward propagation
Computing the network's output for a given input, layer by layer
(`NeuralNetwork.forwardProp`). Set the input nodes to the feature values, then for
each later layer call `Node.updateOutput()`, which does the weighted-sum-plus-bias and
runs it through the activation. The last node's value is the prediction.

### Weighted sum / total input
The number a neuron computes *before* activation:
`totalInput = bias + Σ(weight × source_output)` (`Node.updateOutput`, stored as
`totalInput`). It is kept around because back-propagation needs it.

### Activation function
The non-linear squashing function applied to a neuron's weighted sum. Without it, any
stack of layers would collapse into a single linear function and could only draw
straight decision boundaries. All activations extend **`activation/ActivationFunction`**
and provide two methods: `output(x)` (the value) and `derivative(x)` (its slope, used
in back-prop). The four shipped:

- **Tanh** (`TanhActivation`): `tanh(x)`, S-shaped, output in `(−1, 1)`, zero-centred.
  The default. Derivative `1 − tanh²(x)`.
- **ReLU** (`ReluActivation`): `max(0, x)`. Cheap, the modern default for deep nets.
  Derivative is `1` for `x>0`, else `0`.
- **Sigmoid** (`SigmoidActivation`): `1 / (1 + e⁻ˣ)`, output in `(0, 1)` — natural for
  probabilities. Derivative `σ(x)·(1 − σ(x))`.
- **Linear** (`LinearActivation`): `f(x) = x`, no squashing. Used as the **output**
  activation for regression problems (`Session.rebuildNetwork`).

### Non-linearity
The property (supplied by the activation) that lets the network bend its decision
boundary into curves, circles, and spirals rather than only straight lines.

## B.3 Learning: loss, gradients, back-propagation

### Label / target
The correct answer for a data point (`Example2D.getLabel()`): `+1` or `−1` for the
classification datasets, a real number for regression.

### Loss / error function
A single number measuring how wrong the prediction is. Implemented behind the
**`ErrorFunction`** interface; the only shipped loss is **`SquaredError`**:
`error = ½·(output − target)²`. Its `derivative` is simply `output − target`, which is
the signal that *starts* back-propagation. Lower loss = better network.

### Train loss vs test loss
Loss measured on the data the network *learns from* (train) versus data it has *never
seen* (test). `Session` computes both after every epoch. Test loss much higher than
train loss is the classic sign of **overfitting**.

### Gradient
The derivative of the loss with respect to a parameter — it says "if I nudge this
weight up a little, does the loss go up or down, and how fast?" Training follows the
gradient *downhill*. Every `Node` and `Link` stores gradient accumulators.

### Back-propagation
The algorithm that computes every parameter's gradient efficiently by applying the
chain rule from the output backwards to the input (`NeuralNetwork.backProp`). Steps:
1. **Seed** the output node with `dE/d(output)` from the loss derivative
   (`Node.seedOutputDer`).
2. For each layer, back to front, each node computes
   `inputDer = outputDer × activation.derivative(totalInput)`
   (`Node.computeAndAccumulateInputDer`) — this is the chain rule crossing the
   activation.
3. Each incoming link computes its own gradient
   `errorDer = source.output × dest.inputDer` (`Link.computeAndAccumulateErrorDer`).
4. Each node in the *previous* layer recomputes its `outputDer` by summing the
   weighted gradients flowing back through its outgoing links
   (`Node.recomputeOutputDer`).

Crucially, back-prop only *accumulates* gradients; it never changes a weight itself.

### Chain rule
The calculus rule for differentiating nested functions. Because a network is functions
nested inside functions, the chain rule is exactly what lets an output error be
attributed back to a weight buried deep in the network. Every `derivative(...)` call in
back-prop is one link in that chain.

### Derivative / slope
How fast a function changes at a point. Each activation and the loss provide their own
`derivative`, which back-prop multiplies together. That is why activations must be
**differentiable** — the whole `DifferentiableFunction` interface exists to guarantee
each has both `output` and `derivative`.

## B.4 Optimisation: gradient descent and batching

### Gradient descent
The optimisation strategy: repeatedly step every parameter a little in the direction
that *reduces* loss (opposite the gradient). Applied by
`NeuralNetwork.updateWeights` → `Link.applyWeightUpdate` / `Node.applyBiasUpdate`:
`weight −= learningRate × (accumulated gradient / count)`.

### Stochastic Gradient Descent (SGD) & mini-batch
Instead of computing the gradient over the *entire* dataset before each step (slow),
SGD updates from small subsets. This engine uses **mini-batch** SGD: it accumulates
gradients over `batchSize` examples, then does one update and resets the accumulators
(`Session.trainOneEpoch` triggers `updateWeights` every `batchSize` points). More noise
per step, but far faster and often generalises better.

### Learning rate
The step size for gradient descent (`learningRate` argument). Too small = painfully
slow learning; too large = the loss bounces around or diverges. It is the single most
important knob to tune.

### Epoch
One full pass over all the training data (`Session.trainOneEpoch`). The play loop runs
one epoch per request; the iteration counter `iter` counts epochs completed.

### Iteration
Here, one epoch = one increment of `iter`, shown in the UI. (In some frameworks
"iteration" means one batch; in this codebase it tracks epochs.)

## B.5 Regularisation (fighting overfitting)

### Overfitting
When the network memorises the training points — including their noise — instead of
learning the underlying rule, so it does great on train data and poorly on test data.

### Regularisation
An extra penalty added to the loss that discourages large/complex weights, nudging the
network toward simpler solutions that generalise better. Applied as a *second* step
after the gradient step in `Link.applyWeightUpdate`. All regularisers extend
**`regularization/RegularizationFunction`** and expose `output`, `derivative`, and a
`crossesZero` hook. The three shipped:

- **None** (`NoRegularization`): no penalty.
- **L1 / Lasso** (`L1Regularization`): penalty ∝ `|w|`, derivative is the sign of `w`.
  Pushes many weights *exactly to zero*, producing a **sparse** network — good for
  feature selection. Its special `crossesZero` returns true when a weight flips sign,
  and the engine then permanently clamps that link to zero (a **"dead" link**).
- **L2 / Ridge** (`L2Regularization`): penalty ∝ `½·w²`, derivative is `w`. Shrinks all
  weights smoothly toward zero but never fully kills them.

### Regularisation rate
How strongly the penalty is applied (`regularizationRate`). Zero = off. Higher =
simpler network, but too high underfits.

### Dead link
A link L1 has driven to exactly zero and switched off for the rest of training
(`Link.dead`). It contributes nothing to forward or backward passes and is drawn
faded/absent in the UI. Manually setting a weight via the API "resurrects" it.

### Sparsity
Having many zero weights. L1 regularisation encourages it; a sparse network is simpler,
cheaper, and easier to interpret.

## B.6 Data, problems, and evaluation

### Example / data point
One labelled sample, `Example2D(x, y, label)` — a 2-D coordinate plus its class or
target value.

### Dataset generator
A synthetic data source extending **`datasets/DataGenerator`**. Each subclass produces
`numSamples` points from its own distribution, perturbed by a `noise` amount, using a
seeded `Random` for reproducibility. Shipped:
- **Classification:** Circle, XOR, Gaussian (two blobs), Spiral (two interleaved
  Archimedean spirals — the hardest).
- **Regression:** Plane, Multi-Gaussian.

### Classification vs regression
Two problem types (`Session.Problem`). **Classification** predicts a discrete class
(`+1` / `−1`) and uses a Tanh output; **regression** predicts a continuous number and
uses a **Linear** output. The output activation is chosen automatically in
`Session.rebuildNetwork`.

### Noise
Random jitter added to the generated points (UI slider 0–50, scaled to `[0, 0.5]` for
the generators). More noise = fuzzier, overlapping classes = a harder task and a
stronger temptation to overfit.

### Train/test split
The generated points are shuffled and split into a training set and a test set by the
"percent train data" ratio (`Session.regenerateData`, `percTrainData` 10–90%). The
network learns from train, and is *judged* on test.

### Decision boundary
The surface where the network's output flips from one class to the other — the coloured
background in the UI. It is produced by running `forwardProp` over a grid of points.
Watching it morph as weights change is the whole point of the tool.

### Discretise output
A UI toggle that snaps the continuous output to hard `+1` / `−1` regions, showing the
crisp classification rather than the smooth confidence gradient.

### Seed / reproducibility
A fixed starting number for the random generator. Same seed ⇒ same "random" dataset and
same initial weights every run (`dataSeed`, `networkSeed` on `Session`), so experiments
are repeatable and comparable.

### Symmetry breaking
Starting weights at small *random* values (not all equal) so different neurons learn
different things. If every weight started identical, all neurons would stay identical
forever. Hence `weight = rng.nextDouble() − 0.5` unless "initialise to zero" is set.

---

## B.7 One-glance formula summary

| Quantity | Formula | Code |
|---|---|---|
| Neuron output | `f( b + Σ wᵢ·xᵢ )` | `Node.updateOutput` |
| Squared-error loss | `½·(o − t)²` | `SquaredError.error` |
| Loss derivative (seed) | `o − t` | `SquaredError.derivative` |
| Node input-gradient | `outputDer · f'(totalInput)` | `Node.computeAndAccumulateInputDer` |
| Link gradient | `source.output · dest.inputDer` | `Link.computeAndAccumulateErrorDer` |
| Hidden node output-grad | `Σ (wₒᵤₜ · dest.inputDer)` | `Node.recomputeOutputDer` |
| Weight update (SGD) | `w −= lr · (Σ grad / n)` | `Link.applyWeightUpdate` |
| Bias update (SGD) | `b −= lr · (Σ grad / n)` | `Node.applyBiasUpdate` |
| L1 penalty / deriv | `|w|` / `sign(w)` | `L1Regularization` |
| L2 penalty / deriv | `½·w²` / `w` | `L2Regularization` |

---

*Generated as project documentation for the OOP Neural Network Playground. Every term
above is cross-referenced to the class and method that implements it, so the document
doubles as a guided tour of the source.*
