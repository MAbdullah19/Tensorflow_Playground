package com.playground.nn.regularization;

/**
 * L1 (Lasso) regulariser: penalty proportional to {@code |w|}.
 *
 * <p>L1 is special because its sub-gradient at zero pulls weights all the way
 * down to zero in a single update, which we represent by overriding
 * {@link #crossesZero(double, double)} - the only way the rest of the engine
 * tells L1 apart from L2.
 */
public final class L1Regularization extends RegularizationFunction {

    public L1Regularization() {
        super("L1");
    }

    @Override
    public double output(double weight) {
        return Math.abs(weight);
    }

    @Override
    public double derivative(double weight) {
        if (weight < 0) return -1.0;
        if (weight > 0) return  1.0;
        return 0.0;
    }

    /** L1 clamps weights that crossed zero during the regularisation step. */
    @Override
    public boolean crossesZero(double oldWeight, double newWeight) {
        return oldWeight * newWeight < 0;
    }
}
