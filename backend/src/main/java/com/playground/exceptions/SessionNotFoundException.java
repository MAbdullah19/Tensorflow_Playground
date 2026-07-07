package com.playground.exceptions;

/**
 * The client used a session id that the server doesn't know about
 * (or that has already been evicted).
 *
 * <p>This is a specialisation of {@link NotFoundException} that carries the
 * offending id so it can be logged or surfaced verbatim. It demonstrates
 * a multi-level <b>inheritance</b> chain:
 * {@code SessionNotFoundException -> NotFoundException -> PlaygroundException
 *  -> RuntimeException -> Exception -> Throwable}.
 */
public class SessionNotFoundException extends NotFoundException {

    private final String sessionId;

    public SessionNotFoundException(String sessionId) {
        super("Unknown session: " + sessionId);
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
