package com.playground.exceptions;

/**
 * The caller asked for an unknown / illegal configuration value
 * (unknown dataset, activation, layer size out of range, ...).
 *
 * <p>Maps to HTTP 400 - Bad Request.
 */
public class ConfigurationException extends PlaygroundException {

    public ConfigurationException(String message) {
        super(400, message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(400, message, cause);
    }
}
