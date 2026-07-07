package com.playground.nn.activation;

import com.playground.nn.DifferentiableFunction;

/**
 * Abstract base for the four activation functions supported by the playground.
 *
 * <p><b>OOP concepts demonstrated</b>:
 * <ul>
 *   <li><b>Abstract class</b> - cannot be instantiated; concrete subclasses
 *       must supply {@link #output(double)} and {@link #derivative(double)}.</li>
 *   <li><b>Interface implementation</b> - the abstract class itself fulfills
 *       the {@link DifferentiableFunction} contract on behalf of every
 *       subclass.</li>
 *   <li><b>Encapsulation</b> - the {@code name} field is private and only
 *       reachable through {@link #getName()}.</li>
 *   <li><b>Polymorphism</b> - the network treats every activation as an
 *       {@code ActivationFunction}, calls {@code output} / {@code derivative},
 *       and the JVM dispatches to the right subclass at runtime.</li>
 * </ul>
 */
public abstract class ActivationFunction implements DifferentiableFunction {

    private final String name;

    /** Subclass constructor: every concrete activation must declare its key. */
    protected ActivationFunction(String name) {
        this.name = name;
    }

    /** Stable URL-friendly key (e.g. {@code "tanh"}). */
    public final String getName() {
        return name;
    }

    @Override
    public abstract double output(double x);

    @Override
    public abstract double derivative(double x);

    /** Pretty form for logs. */
    @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + name + "]";
    }
}
