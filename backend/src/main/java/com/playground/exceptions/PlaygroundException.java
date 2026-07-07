package com.playground.exceptions;

/**
 * Abstract root of every exception thrown by the playground service.
 *
 * <p><b>OOP concepts demonstrated</b>: this class is the base of an
 * <b>inheritance hierarchy</b> ({@link ConfigurationException},
 * {@link SessionNotFoundException}, {@link NotFoundException},
 * {@link TrainingException}). Each subclass overrides nothing in particular -
 * the value comes from {@code httpStatus}, which uses <b>encapsulation</b>:
 * subclasses provide their status to the {@code protected} constructor and the
 * field is exposed through a {@code final} getter so callers can never change
 * it.
 *
 * <p>The class is declared {@code abstract} so callers cannot instantiate the
 * raw "playground exception" - they have to choose a concrete subtype. Code
 * that catches {@code PlaygroundException} can then dispatch
 * <b>polymorphically</b> on {@link #getHttpStatus()} without using
 * {@code instanceof} at all.
 */
public abstract class PlaygroundException extends RuntimeException {

    private final int httpStatus;

    protected PlaygroundException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    protected PlaygroundException(int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    /** HTTP status that should accompany this error in the API response. */
    public final int getHttpStatus() {
        return httpStatus;
    }
}
