package com.playground.nn;

/**
 * Common contract for any 1-D function whose value <i>and</i> first
 * derivative we need at training time.
 *
 * <p><b>OOP concepts demonstrated</b>: this is a Java <b>interface</b>, the
 * lightest form of <b>abstraction</b> Java offers. Multiple unrelated class
 * hierarchies (activation functions and regularization functions) implement
 * this single interface, which is exactly the situation interfaces - rather
 * than abstract classes - are designed for.
 */
public interface DifferentiableFunction {

    /** Value of the function at {@code x}. */
    double output(double x);

    /** First derivative of the function at {@code x}. */
    double derivative(double x);
}
