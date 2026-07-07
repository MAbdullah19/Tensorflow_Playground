package com.playground.data.datasets;

import com.playground.data.Example2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Two Gaussian blobs, one in the upper-right and one in the lower-left. */
public final class GaussianDataset extends DataGenerator {

    public GaussianDataset() {
        super("gauss", false);
    }

    @Override
    public List<Example2D> generate(int numSamples, double noise, Random rng) {
        List<Example2D> points = new ArrayList<>();
        // Variance grows linearly with the noise slider, identical to the original.
        double variance = linearScale(noise, 0, 0.5, 0.5, 4.0);
        addGauss(points, numSamples / 2, 2,  2,  1.0, variance, rng);
        addGauss(points, numSamples / 2, -2, -2, -1.0, variance, rng);
        return points;
    }

    private static void addGauss(List<Example2D> points, int count,
                                 double cx, double cy, double label,
                                 double variance, Random rng) {
        for (int i = 0; i < count; i++) {
            double x = normalRandom(rng, cx, variance);
            double y = normalRandom(rng, cy, variance);
            points.add(new Example2D(x, y, label));
        }
    }
}
