package com.playground.nn.regularization;

/**
 * L2 (Ridge) regulariser: penalty proportional to {@code w^2 / 2}, derivative
 * just {@code w}. Pulls weights smoothly towards zero without ever clamping
 * them all the way.
 */
public final class L2Regularization extends RegularizationFunction {

    public L2Regularization() {
        super("L2");
    }

    @Override
    public double output(double weight) {
        return 0.5 * weight * weight;
    }

    @Override
    public double derivative(double weight) {
        return weight;
    }
}
