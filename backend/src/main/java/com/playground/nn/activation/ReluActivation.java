package com.playground.nn.activation;

/**
 * Rectified linear unit: {@code max(0, x)}. Cheap to compute and the de-facto
 * default in modern deep nets.
 */
public final class ReluActivation extends ActivationFunction {

    public ReluActivation() {
        super("relu");
    }

    @Override
    public double output(double x) {
        return Math.max(0.0, x);
    }

    @Override
    public double derivative(double x) {
        return x <= 0.0 ? 0.0 : 1.0;
    }
}
