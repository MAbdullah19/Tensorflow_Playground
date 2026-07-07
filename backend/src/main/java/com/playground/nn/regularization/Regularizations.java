package com.playground.nn.regularization;

import com.playground.exceptions.ConfigurationException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static registry / factory for the three built-in regularisers.
 * Mirrors the layout of {@link com.playground.nn.activation.Activations}.
 */
public final class Regularizations {

    public static final NoRegularization NONE = new NoRegularization();
    public static final L1Regularization L1   = new L1Regularization();
    public static final L2Regularization L2   = new L2Regularization();

    private static final Map<String, RegularizationFunction> BY_KEY = new LinkedHashMap<>();
    static {
        register(NONE);
        register(L1);
        register(L2);
    }

    private Regularizations() {
        // utility class
    }

    private static void register(RegularizationFunction r) {
        BY_KEY.put(r.getName(), r);
    }

    public static RegularizationFunction byKey(String key) {
        if (key == null || key.isEmpty()) {
            return NONE;
        }
        // L1 / L2 are case-sensitive; "none" can be either case.
        if (key.equalsIgnoreCase("none")) return NONE;
        RegularizationFunction r = BY_KEY.get(key);
        if (r == null) {
            throw new ConfigurationException("Unknown regularization: " + key);
        }
        return r;
    }

    public static Collection<RegularizationFunction> all() {
        return Collections.unmodifiableCollection(BY_KEY.values());
    }
}
