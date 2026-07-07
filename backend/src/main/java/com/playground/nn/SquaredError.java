package com.playground.nn;

/**
 * Plain mean-squared-error loss: {@code 0.5 * (output - target)^2}.
 *
 * <p>Provided as a real class implementing {@link ErrorFunction} so the
 * inheritance / interface relationship is visible. A {@link #INSTANCE}
 * singleton is offered for convenience because the class holds no state.
 */
public final class SquaredError implements ErrorFunction {

    public static final SquaredError INSTANCE = new SquaredError();

    @Override
    public double error(double output, double target) {
        double d = output - target;
        return 0.5 * d * d;
    }

    @Override
    public double derivative(double output, double target) {
        return output - target;
    }
}
