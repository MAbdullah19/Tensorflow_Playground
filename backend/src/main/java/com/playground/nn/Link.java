package com.playground.nn;

import com.playground.nn.regularization.RegularizationFunction;
import com.playground.nn.regularization.Regularizations;

import java.util.Random;

/**
 * Weighted directed edge connecting two {@link Node}s.
 *
 * <p><b>OOP concepts demonstrated</b>:
 * <ul>
 *   <li><b>Encapsulation</b> - the weight, dead flag, and gradient
 *       accumulators are all private. The two SGD operations
 *       ({@link #computeAndAccumulateErrorDer()} and
 *       {@link #applyWeightUpdate(double, double)}) are exposed as methods
 *       so callers describe <i>intent</i> rather than mutating state by
 *       hand.</li>
 *   <li><b>Polymorphism</b> - {@link #applyWeightUpdate} dispatches
 *       {@code regularization.derivative(...)} and
 *       {@code regularization.crossesZero(...)} to whichever subclass
 *       (None / L1 / L2) is wired in - no {@code instanceof} required.</li>
 *   <li><b>Composition</b> - a Link <i>has-a</i> source Node, a destination
 *       Node, and a regularization strategy.</li>
 * </ul>
 */
public final class Link {

    private final String id;
    private final Node source;
    private final Node dest;
    private final RegularizationFunction regularization;

    private double weight;
    private boolean dead = false;

    // gradient accumulators reset every batch
    private double errorDer;
    private double accErrorDer;
    private int    numAccumulatedDers;

    public Link(Node source, Node dest, RegularizationFunction regularization,
                boolean initZero, Random rng) {
        this.id = source.getId() + "-" + dest.getId();
        this.source = source;
        this.dest = dest;
        this.regularization = regularization == null ? Regularizations.NONE : regularization;
        this.weight = initZero ? 0.0 : (rng.nextDouble() - 0.5);
    }

    // --- accessors -------------------------------------------------------

    public String getId()                            { return id; }
    public Node   getSource()                        { return source; }
    public Node   getDest()                          { return dest; }
    public double getWeight()                        { return weight; }
    public boolean isDead()                          { return dead; }
    public RegularizationFunction getRegularization(){ return regularization; }

    /** Manual override (used by the API "/weight" endpoint). Resurrects a dead link. */
    public void setWeight(double weight) {
        this.weight = weight;
        this.dead = false;
    }

    // --- gradient accumulation / update ---------------------------------

    /** Compute {@code errorDer = source.output * dest.inputDer} and accumulate. */
    public void computeAndAccumulateErrorDer() {
        if (dead) return;
        errorDer = dest.getInputDer() * source.getOutput();
        accErrorDer += errorDer;
        numAccumulatedDers++;
    }

    /**
     * Apply one SGD update step using the accumulated batch gradient and the
     * regularizer. The regularizer's {@code crossesZero} hook decides whether
     * the weight just transitioned through zero (only L1 says yes), in which
     * case the link is permanently disabled for the rest of training.
     */
    public void applyWeightUpdate(double learningRate, double regularizationRate) {
        if (dead || numAccumulatedDers == 0) {
            return;
        }
        // 1. gradient step
        weight -= (learningRate / numAccumulatedDers) * accErrorDer;
        // 2. regularization step
        double regulDer = regularization.derivative(weight);
        double newWeight = weight - (learningRate * regularizationRate) * regulDer;
        if (regularization.crossesZero(weight, newWeight)) {
            weight = 0.0;
            dead = true;
        } else {
            weight = newWeight;
        }
        // 3. reset accumulators
        accErrorDer = 0;
        numAccumulatedDers = 0;
    }

    @Override
    public String toString() {
        return "Link[" + id + ", w=" + weight + (dead ? ", DEAD" : "") + "]";
    }
}
