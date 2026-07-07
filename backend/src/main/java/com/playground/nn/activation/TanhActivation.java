package com.playground.nn.activation;

/**
 * Hyperbolic tangent activation: smooth, zero-centred, range (-1, 1).
 * The default activation in the original Tensorflow Playground.
 */
public final class TanhActivation extends ActivationFunction {

    public TanhActivation() {
        super("tanh");
    }

    @Override
    public double output(double x) {
        return Math.tanh(x);
    }

    @Override
    public double derivative(double x) {
        double t = Math.tanh(x);
        return 1.0 - t * t;
    }
}
