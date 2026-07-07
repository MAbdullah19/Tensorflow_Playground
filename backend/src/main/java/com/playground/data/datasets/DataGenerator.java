package com.playground.data.datasets;

import com.playground.data.Example2D;

import java.util.List;
import java.util.Random;

/**
 * Abstract base class for every synthetic 2-D dataset shipped with the
 * playground.
 *
 * <p><b>OOP concepts demonstrated</b>:
 * <ul>
 *   <li><b>Inheritance</b> - six concrete subclasses
 *       (CircleDataset, XorDataset, GaussianDataset, SpiralDataset,
 *        PlaneRegressionDataset, GaussianRegressionDataset).</li>
 *   <li><b>Encapsulation</b> - {@code key} and {@code regression} are
 *       private and exposed via {@code final} getters so subclasses can never
 *       mutate them.</li>
 *   <li><b>Polymorphism</b> - {@link #generate(int, double, java.util.Random)}
 *       is the single hook invoked by the rest of the engine; the JVM picks
 *       the right subclass implementation at runtime.</li>
 *   <li><b>Template method (lite)</b> - {@code protected static} helpers
 *       ({@link #randUniform}, {@link #normalRandom}, ...) live on the base
 *       class and are reused by the subclasses without forcing them to share
 *       any state.</li>
 * </ul>
 */
public abstract class DataGenerator {

    private final String key;
    private final boolean regression;

    protected DataGenerator(String key, boolean regression) {
        this.key = key;
        this.regression = regression;
    }

    /** URL-friendly identifier used in JSON requests. */
    public final String getKey() {
        return key;
    }

    /** {@code true} for the two regression datasets, {@code false} otherwise. */
    public final boolean isRegression() {
        return regression;
    }

    /**
     * Subclasses must produce {@code numSamples} labelled examples drawn from
     * their distribution, perturbed by {@code noise} (in the [0, 1] range
     * that the original Tensorflow Playground uses).
     */
    public abstract List<Example2D> generate(int numSamples, double noise, Random rng);

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + key + "]";
    }

    // --------------------------------------------------------------------
    //  Helpers shared by the concrete subclasses (kept package-private so
    //  the implementation detail does not leak into the public API).
    // --------------------------------------------------------------------

    protected static double randUniform(Random rng, double a, double b) {
        return rng.nextDouble() * (b - a) + a;
    }

    /**
     * Marsaglia polar method for Gaussian samples - matches the original
     * d3 / TS implementation exactly so seeded runs reproduce.
     */
    protected static double normalRandom(Random rng, double mean, double variance) {
        double v1, v2, s;
        do {
            v1 = 2 * rng.nextDouble() - 1;
            v2 = 2 * rng.nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
        } while (s > 1 || s == 0);
        double result = Math.sqrt(-2 * Math.log(s) / s) * v1;
        return mean + Math.sqrt(variance) * result;
    }

    protected static double dist(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2, dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    protected static double linearScale(double v, double d0, double d1, double r0, double r1) {
        double t = (v - d0) / (d1 - d0);
        return r0 + t * (r1 - r0);
    }

    protected static double clampLinear(double v, double d0, double d1, double r0, double r1) {
        double t = (v - d0) / (d1 - d0);
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        return r0 + t * (r1 - r0);
    }
}
