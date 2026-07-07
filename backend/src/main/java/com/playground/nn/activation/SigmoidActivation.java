package com.playground.nn.activation;

/**
 * Logistic sigmoid: {@code 1 / (1 + exp(-x))}. Output range is (0, 1) so it's
 * mostly useful when the network must produce probabilities or 0/1 targets.
 */
public final class SigmoidActivation extends ActivationFunction {

    public SigmoidActivation() {
        super("sigmoid");
    }

    @Override
    public double output(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    @Override
    public double derivative(double x) {
        double o = output(x);
        return o * (1.0 - o);
    }
}
