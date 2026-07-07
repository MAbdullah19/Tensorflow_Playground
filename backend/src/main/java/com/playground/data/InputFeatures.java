package com.playground.data;

import com.playground.exceptions.ConfigurationException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleBinaryOperator;

/**
 * Hand-crafted feature transformations used as the input layer of the network.
 *
 * <p>Input layer uses the 2D coordinates X1 and X2 only.
 *
 * <p><b>OOP note</b>: in Java, an {@code enum} is a class whose values are
 * singletons. Each constant below provides its own implementation of
 * {@link #apply(double, double)} (via the {@code DoubleBinaryOperator} field),
 * so iterating over {@code values()} and calling {@code apply} dispatches
 * <b>polymorphically</b> to the right transformation. This is the "Strategy
 * pattern" expressed compactly.
 */
public final class InputFeatures {

    public enum Feature {
        X("x", (x, y) -> x),
        Y("y", (x, y) -> y);

        private final String key;
        private final DoubleBinaryOperator op;

        Feature(String key, DoubleBinaryOperator op) {
            this.key = key;
            this.op = op;
        }

        public String getKey() {
            return key;
        }

        public double apply(double x, double y) {
            return op.applyAsDouble(x, y);
        }

        public static Feature fromKey(String key) {
            for (Feature f : values()) {
                if (f.key.equals(key)) return f;
            }
            throw new ConfigurationException("Unknown feature: " + key);
        }
    }

    private InputFeatures() {
        // utility class
    }

    /**
     * Builds a feature vector for the supplied (x, y) using the ordered list
     * of active features.
     */
    public static double[] build(List<Feature> active, double x, double y) {
        double[] out = new double[active.size()];
        for (int i = 0; i < active.size(); i++) {
            out[i] = active.get(i).apply(x, y);
        }
        return out;
    }

    public static List<Feature> parseKeys(List<String> keys) {
        List<Feature> result = new ArrayList<>();
        if (keys == null) return result;
        for (String k : keys) result.add(Feature.fromKey(k));
        return result;
    }
}
