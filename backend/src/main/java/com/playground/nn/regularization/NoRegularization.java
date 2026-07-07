package com.playground.nn.regularization;

/**
 * No-op regularizer used when the user picks "none" in the UI.
 *
 * <p>Implemented as a real subclass instead of a {@code null} sentinel so
 * that the rest of the code can treat <i>every</i> link as having a
 * regularizer (no special-casing, no {@code NullPointerException}s).
 */
public final class NoRegularization extends RegularizationFunction {

    public NoRegularization() {
        super("none");
    }

    @Override
    public double output(double weight) {
        return 0.0;
    }

    @Override
    public double derivative(double weight) {
        return 0.0;
    }
}
