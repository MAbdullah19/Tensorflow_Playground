package com.playground.data.datasets;

import com.playground.exceptions.ConfigurationException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static registry of every {@link DataGenerator} the playground knows about.
 *
 * <p>Same shape as {@link com.playground.nn.activation.Activations} and
 * {@link com.playground.nn.regularization.Regularizations}: the consistency
 * is intentional - swap one of these out and the rest of the system keeps
 * working unchanged.
 */
public final class DatasetRegistry {

    public static final CircleDataset              CIRCLE    = new CircleDataset();
    public static final XorDataset                 XOR       = new XorDataset();
    public static final GaussianDataset            GAUSS     = new GaussianDataset();
    public static final SpiralDataset              SPIRAL    = new SpiralDataset();
    public static final PlaneRegressionDataset     REG_PLANE = new PlaneRegressionDataset();
    public static final GaussianRegressionDataset  REG_GAUSS = new GaussianRegressionDataset();

    private static final Map<String, DataGenerator> BY_KEY = new LinkedHashMap<>();
    static {
        register(CIRCLE);
        register(XOR);
        register(GAUSS);
        register(SPIRAL);
        register(REG_PLANE);
        register(REG_GAUSS);
    }

    private DatasetRegistry() {
        // utility class
    }

    private static void register(DataGenerator g) {
        BY_KEY.put(g.getKey(), g);
    }

    public static DataGenerator byKey(String key) {
        DataGenerator g = BY_KEY.get(key);
        if (g == null) {
            throw new ConfigurationException("Unknown dataset: " + key);
        }
        return g;
    }

    public static Collection<DataGenerator> all() {
        return Collections.unmodifiableCollection(BY_KEY.values());
    }
}
