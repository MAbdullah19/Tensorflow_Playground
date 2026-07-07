package com.playground.exceptions;

/**
 * Generic 404 - the client referred to a resource that doesn't exist
 * (a node id, a route, a link, ...).
 *
 * <p>For the specific case of a missing session see
 * {@link SessionNotFoundException}, which carries the session id verbatim.
 */
public class NotFoundException extends PlaygroundException {

    public NotFoundException(String message) {
        super(404, message);
    }
}
