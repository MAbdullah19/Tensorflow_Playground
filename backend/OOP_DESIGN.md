# OOP Design Notes — Playground Backend (Java)

This document maps every core Object-Oriented Programming concept covered in
a typical Java OOP course to a concrete class, interface, or method in this
backend so a reader (or grader) can jump straight to the code that
demonstrates each idea.

The backend has zero external dependencies — only `java.*` and
`com.sun.net.httpserver` from the JDK — so every line of the design below is
your own code.

## Package layout

```
com.playground/
├── Main.java                          # entry point (composition root)
├── exceptions/                        # custom exception hierarchy
│   ├── PlaygroundException.java       # abstract base
│   ├── ConfigurationException.java    # 400
│   ├── NotFoundException.java         # 404
│   ├── SessionNotFoundException.java  # 404 (extends NotFoundException)
│   └── TrainingException.java         # 500
├── nn/                                # neural-network core
│   ├── DifferentiableFunction.java    # interface
│   ├── ErrorFunction.java             # interface
│   ├── SquaredError.java              # implements ErrorFunction
│   ├── Node.java                      # encapsulated neuron
│   ├── Link.java                      # encapsulated weighted edge
│   ├── NeuralNetwork.java             # orchestrator (Tell-Don't-Ask)
│   ├── activation/
│   │   ├── ActivationFunction.java    # abstract class
│   │   ├── TanhActivation.java
│   │   ├── ReluActivation.java
│   │   ├── SigmoidActivation.java
│   │   ├── LinearActivation.java
│   │   └── Activations.java           # registry / factory
│   └── regularization/
│       ├── RegularizationFunction.java   # abstract class
│       ├── NoRegularization.java
│       ├── L1Regularization.java
│       ├── L2Regularization.java
│       └── Regularizations.java          # registry / factory
├── data/
│   ├── Example2D.java                 # immutable value object
│   ├── InputFeatures.java             # enum-based Strategy pattern
│   └── datasets/
│       ├── DataGenerator.java                  # abstract class
│       ├── CircleDataset.java
│       ├── XorDataset.java
│       ├── GaussianDataset.java
│       ├── SpiralDataset.java
│       ├── PlaneRegressionDataset.java
│       ├── GaussianRegressionDataset.java
│       └── DatasetRegistry.java
├── api/
│   ├── ApiServer.java                 # HTTP + central exception handler
│   ├── RequestHandlers.java           # routes
│   ├── Session.java                   # implements Trainable
│   ├── SessionManager.java            # in-memory store
│   └── Trainable.java                 # interface
└── util/
    └── Json.java                      # minimal JSON codec
```

---

## 1. Encapsulation

The class contract is "no public fields; mutate state through named methods".

| Where                          | What's hidden                                                                                              |
| ------------------------------ | ---------------------------------------------------------------------------------------------------------- |
| `nn/Node.java`                 | bias, all gradient accumulators, list of input/output links                                                |
| `nn/Link.java`                 | weight, dead flag, gradient accumulators                                                                   |
| `data/Example2D.java`          | x, y, label — `private final`, class is `final` ⇒ truly immutable                                          |
| `api/Session.java`             | every config knob has a validating setter (`setNoise` rejects out-of-range, `setDataset` rejects wrong problem type) |
| `nn/activation/ActivationFunction.java` | `name` is private, exposed via `final getName()`                                                  |

**Why it matters in this project**: the whole back-prop algorithm runs
through the encapsulated methods of `Node` and `Link`
(`updateOutput`, `computeAndAccumulateInputDer`, `applyWeightUpdate`, ...).
The orchestrator (`NeuralNetwork`) cannot mutate the gradient accumulators
directly — it can only ask the node to advance one step at a time. This is
the "Tell, Don't Ask" principle in action.

## 2. Inheritance

Three independent class hierarchies and one for exceptions:

| Base (abstract)                                | Subclasses (final)                                                              |
| ---------------------------------------------- | ------------------------------------------------------------------------------- |
| `nn/activation/ActivationFunction`             | `TanhActivation`, `ReluActivation`, `SigmoidActivation`, `LinearActivation`     |
| `nn/regularization/RegularizationFunction`     | `NoRegularization`, `L1Regularization`, `L2Regularization`                      |
| `data/datasets/DataGenerator`                  | `CircleDataset`, `XorDataset`, `GaussianDataset`, `SpiralDataset`, `PlaneRegressionDataset`, `GaussianRegressionDataset` |
| `exceptions/PlaygroundException`               | `ConfigurationException`, `NotFoundException`, `TrainingException`              |
| `exceptions/NotFoundException`                 | `SessionNotFoundException` (multi-level: 3 levels deep before reaching `RuntimeException`) |

The `DataGenerator` base class also demonstrates **template-method-style
helpers** (`randUniform`, `normalRandom`, `dist`, `linearScale`,
`clampLinear`) that subclasses inherit but the outside world never sees
because they're `protected static`.

## 3. Polymorphism

Three pieces of code rely on runtime polymorphic dispatch and would have to
use ugly `instanceof` chains without it:

1. **Forward / back-prop** — `nn/NeuralNetwork.java`:
   `node.updateOutput()` invokes `activation.output(...)` which dispatches to
   the right `ActivationFunction` subclass.
2. **L1 zero-clamp** — `nn/Link.java#applyWeightUpdate`:
   asks `regularization.crossesZero(oldW, newW)`. Default returns `false`;
   `L1Regularization` overrides to detect sign changes. The Link class never
   knows whether it has L1, L2 or no regularizer.
3. **HTTP status mapping** — `api/ApiServer.java#dispatch`:
   the central `catch (PlaygroundException pe)` pulls
   `pe.getHttpStatus()`. Each subclass carries its own status (400 / 404 /
   500). Adding a new exception type means 0 changes to the dispatcher.

A fourth example: `data/InputFeatures.java`. Each enum constant overrides
`apply(x, y)` via its own `DoubleBinaryOperator`, so `Feature.values()`
followed by `.apply(...)` is polymorphic without any `if`.

## 4. Abstraction

| Form               | Example                                                                            |
| ------------------ | ---------------------------------------------------------------------------------- |
| Abstract class     | `ActivationFunction`, `RegularizationFunction`, `DataGenerator`, `PlaygroundException` |
| Interface          | `nn/DifferentiableFunction.java`, `nn/ErrorFunction.java`, `api/Trainable.java`    |
| Static utility     | `Activations`, `Regularizations`, `DatasetRegistry`, `util/Json.java`              |

When to use which is also illustrated:

* `DifferentiableFunction` is an **interface** because two unrelated class
  hierarchies (activations and regularizers) share the contract but no state.
* `ActivationFunction` is an **abstract class** because the four subclasses
  share state (the `name` field) and a final `getName()` implementation.
* `Trainable` is an **interface** so test harnesses or alternate transports
  can drive any session-like object without depending on the concrete
  `Session` class.

## 5. Composition (HAS-A) vs Inheritance (IS-A)

* `Session` **HAS-A** `DataGenerator`, `ActivationFunction`,
  `RegularizationFunction`, list of `Feature`s, and a network of `Node`s —
  none of which it inherits from.
* `Node` **HAS-A** `ActivationFunction` and lists of `Link`s.
* `ApiServer` **HAS-A** `SessionManager` and `RequestHandlers`.

Inheritance was deliberately avoided where composition was clearer (e.g.,
Session is not a subclass of NetworkConfig — it composes one).

## 6. Exception handling

Custom hierarchy under `exceptions/`:

```
RuntimeException
  └── PlaygroundException        (abstract, holds httpStatus)
        ├── ConfigurationException     ⇒ 400
        ├── TrainingException          ⇒ 500
        └── NotFoundException          ⇒ 404
              └── SessionNotFoundException
```

Highlights:

* `RequestHandlers` calls `sessions.require(id)` — throws
  `SessionNotFoundException` automatically.
* `Session.setNoise(double)` throws `ConfigurationException` if the value is
  out of range, **before** any state mutation happens, so the session is
  never left half-updated.
* `RequestHandlers#train` wraps any unexpected `RuntimeException` from the
  inner training loop in a `TrainingException` (chained via the
  `cause` constructor) so the API surfaces a meaningful 500 with a useful
  message.
* `ApiServer#readBody` uses **try-with-resources** to guarantee the request
  body stream is closed.
* `ApiServer#dispatch` uses one `try` / multiple `catch` blocks — first the
  rich `PlaygroundException`, then `IllegalArgumentException` (defensive
  fallback), then `Exception` (last-resort 500).

## 7. Static factory methods + Singletons

* `Activations.byKey(String)`, `Regularizations.byKey(String)`,
  `DatasetRegistry.byKey(String)` — each hides a `Map` lookup and throws
  `ConfigurationException` for unknown keys.
* `SquaredError.INSTANCE` — singleton because the class holds no state.
* The four/three concrete activations / regularizers are all stateless, so
  `Activations.TANH`, `Regularizations.L1`, `DatasetRegistry.SPIRAL` are
  reused everywhere.

## 8. `final` keyword: classes, methods, fields

* Every concrete activation / regularizer / dataset is `final` — they
  represent fixed mathematical formulas; no further subclassing is sensible.
* `Example2D` is `final` (immutable value).
* `PlaygroundException.getHttpStatus()` is `final` — subclasses must not
  override it; instead they pass the status into the constructor.
* `ActivationFunction.toString()` is `final` — same reason.
* Fields like `id`, `activation` in `Node`, `source`, `dest` in `Link`, and
  every field of `Example2D` are `final`.

## 9. Method overloading

* `Session#trainOneEpoch(double, double, int)` — full version.
* `Session#trainOneEpoch()` — overload that delegates with the original
  Tensorflow Playground defaults `(0.03, 0.0, 10)`.

## 10. Generics (used implicitly)

The codebase uses generics where they pay off (`Map<String, Object>`,
`List<Node>`, `Map<String, ActivationFunction>`) without inventing custom
generic types. The minimal JSON encoder in `util/Json.java` walks a generic
`Map<?, ?>`/`List<?>` shape.

## 11. Static vs instance members

* Pure helper classes (`Activations`, `DatasetRegistry`, `NeuralNetwork`,
  `Json`) have private constructors and only static methods.
* Domain objects (`Session`, `Node`, `Link`) have rich instance state and
  instance methods.

## 12. Two ways an enum demonstrates OOP in Java

`InputFeatures.Feature` and `Session.Problem` show that Java enums are
themselves classes:

* Each constant of `Feature` carries a different `DoubleBinaryOperator`, so
  iterating `Feature.values()` and calling `.apply(x, y)` dispatches
  polymorphically.
* `Session.Problem.fromKey(String)` is a static factory inside an enum.

---

## Where to look when you're asked about ...

| Concept              | Quickest single file to point to                         |
| -------------------- | -------------------------------------------------------- |
| Encapsulation        | `nn/Node.java`                                           |
| Inheritance          | `nn/activation/` (any subclass + `ActivationFunction`)   |
| Polymorphism         | `nn/Link.java#applyWeightUpdate` (uses `crossesZero`)    |
| Abstraction          | `nn/DifferentiableFunction.java`                         |
| Interface            | `api/Trainable.java`                                     |
| Abstract class       | `data/datasets/DataGenerator.java`                       |
| Custom exception     | `exceptions/PlaygroundException.java`                    |
| try / catch / finally| `api/ApiServer.java#dispatch`                            |
| try-with-resources   | `api/ApiServer.java#readBody`                            |
| Composition          | `api/Session.java`                                       |
| Static factory       | `nn/activation/Activations.java#byKey`                   |
| Method overloading   | `api/Session.java#trainOneEpoch()`                       |
| Immutability         | `data/Example2D.java`                                    |
