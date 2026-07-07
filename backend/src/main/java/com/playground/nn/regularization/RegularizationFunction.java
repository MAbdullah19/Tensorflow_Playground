package com.playground.nn.regularization;

import com.playground.nn.DifferentiableFunction;

/**
 * Abstract base for weight-regularisation strategies.
 *
 * <p><b>OOP concepts demonstrated</b>:
 * <ul>
 *   <li><b>Abstract class</b> - subclasses must provide both
 *       {@link #output(double)} and {@link #derivative(double)}.</li>
 *   <li><b>Template method</b> + <b>polymorphic hook</b> -
 *       {@link #crossesZero(double, double)} has a default implementation
 *       (always {@code false}) but {@link L1Regularization} overrides it,
 *       so the {@code Link} class can perform the L1 dead-link clamp without
 *       ever using {@code instanceof}.</li>
 *   <li><b>Encapsulation</b> - the {@code name} field is private, exposed
 *       through a {@code final} getter.</li>
 * </ul>
 */
public abstract class RegularizationFunction implements DifferentiableFunction {

    private final String name;

    protected RegularizationFunction(String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    @Override
    public abstract double output(double weight);

    @Override
    public abstract double derivative(double weight);

    /**
     * Hook used by {@code Link.applyWeightUpdate}. If the post-gradient and
     * post-regularisation weights have opposite signs, L1 will set the weight
     * to zero and mark the link dead. Default behaviour: no clamp.
     */
    public boolean crossesZero(double oldWeight, double newWeight) {
        return false;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + name + "]";
    }
}
