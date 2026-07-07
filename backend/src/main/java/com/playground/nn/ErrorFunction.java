package com.playground.nn;

/**
 * Loss / error function used by back-propagation to start the gradient
 * computation at the output node.
 *
 * <p><b>OOP concepts demonstrated</b>: this is a deliberate <b>interface</b>
 * (not an abstract class) because two error functions may have nothing in
 * common - they don't even need to share state - but they must agree on the
 * same two-method contract. {@link SquaredError} is the only implementation
 * shipped today, but adding cross-entropy or hinge loss is just "implement
 * this interface".
 */
public interface ErrorFunction {

    /** Per-example loss between predicted output and target label. */
    double error(double output, double target);

    /** {@code dE / d(output)}, used to seed back-propagation. */
    double derivative(double output, double target);
}
