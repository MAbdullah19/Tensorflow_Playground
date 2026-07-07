package com.playground.api;

/**
 * Anything in the system that can be driven through epochs of mini-batch SGD.
 *
 * <p><b>OOP concepts demonstrated</b>: an interface that captures the
 * "training" contract independently of where the data and the network live.
 * Today only {@link Session} implements it, but the surface is small and
 * stable enough that you could write tests, a CLI runner or a different
 * frontend transport against this interface alone, without ever touching the
 * heavy {@code Session} class directly.
 */
public interface Trainable {

    /** Runs a single epoch (one pass over the training set). */
    void trainOneEpoch(double learningRate, double regularizationRate, int batchSize);

    /** Number of completed epochs so far. */
    int getIteration();

    double getTrainLoss();

    double getTestLoss();
}
