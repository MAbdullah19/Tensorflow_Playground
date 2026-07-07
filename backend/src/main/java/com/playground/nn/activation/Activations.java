package com.playground.nn.activation;

import com.playground.exceptions.ConfigurationException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry / factory for the four built-in activation functions.
 *
 * <p><b>OOP concepts demonstrated</b>:
 * <ul>
 *   <li><b>Static factory method</b> - {@link #byKey(String)} hides the
 *       {@code switch} or hash lookup and lets callers stay decoupled from
 *       the concrete subclasses.</li>
 *   <li><b>Final class + private constructor</b> - this utility is not
 *       intended to be instantiated or extended.</li>
 *   <li><b>Encapsulation</b> - the underlying map is private and exposed via
 *       a read-only view.</li>
 * </ul>
 */
public final class Activations {

    public static final TanhActivation     TANH    = new TanhActivation();
    public static final ReluActivation     RELU    = new ReluActivation();
    public static final SigmoidActivation  SIGMOID = new SigmoidActivation();
    public static final LinearActivation   LINEAR  = new LinearActivation();

    private static final Map<String, ActivationFunction> BY_KEY = new LinkedHashMap<>();
    static {
        register(TANH);
        register(RELU);
        register(SIGMOID);
        register(LINEAR);
    }

    private Activations() {
        // utility class
    }

    private static void register(ActivationFunction af) {
        BY_KEY.put(af.getName(), af);
    }

    /**
     * Looks up an activation by its short key.
     *
     * @throws ConfigurationException if the key is unknown
     */
    public static ActivationFunction byKey(String key) {
        if (key == null) {
            return TANH;
        }
        ActivationFunction af = BY_KEY.get(key.toLowerCase());
        if (af == null) {
            throw new ConfigurationException("Unknown activation: " + key);
        }
        return af;
    }

    /** Read-only view of every known activation. */
    public static Collection<ActivationFunction> all() {
        return Collections.unmodifiableCollection(BY_KEY.values());
    }
}
