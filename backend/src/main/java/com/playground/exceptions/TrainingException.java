package com.playground.exceptions;

/**
 * Wraps anything that goes wrong while training the network
 * (e.g. attempting to train before the network has been built).
 *
 * <p>Maps to HTTP 500 - Internal Server Error.
 */
public class TrainingException extends PlaygroundException {

    public TrainingException(String message) {
        super(500, message);
    }

    public TrainingException(String message, Throwable cause) {
        super(500, message, cause);
    }
}
