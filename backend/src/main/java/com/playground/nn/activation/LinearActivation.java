package com.playground.nn.activation;

/**
 * Identity activation. Useful for the output layer of a regression network
 * where we want the final value to live in an unbounded range.
 */
public final class LinearActivation extends ActivationFunction {

    public LinearActivation() {
        super("linear");
    }

    @Override
    public double output(double x) {
        return x;
    }

    @Override
    public double derivative(double x) {
        return 1.0;
    }
}
