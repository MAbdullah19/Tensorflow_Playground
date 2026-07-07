package com.playground.data.datasets;

import com.playground.data.Example2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Six bumps (alternating positive and negative) arranged on a 2x3 grid - a
 * non-linear regression target that needs hidden units to fit.
 */
public final class GaussianRegressionDataset extends DataGenerator {

    private static final double[][] GAUSSIANS = {
            {-4,  2.5,  1},
            { 0,  2.5, -1},
            { 4,  2.5,  1},
            {-4, -2.5, -1},
            { 0, -2.5,  1},
            { 4, -2.5, -1}
    };

    public GaussianRegressionDataset() {
        super("reg-gauss", true);
    }

    @Override
    public List<Example2D> generate(int numSamples, double noise, Random rng) {
        double radius = 6;
        List<Example2D> points = new ArrayList<>();
        for (int i = 0; i < numSamples; i++) {
            double x = randUniform(rng, -radius, radius);
            double y = randUniform(rng, -radius, radius);
            double noiseX = randUniform(rng, -radius, radius) * noise;
            double noiseY = randUniform(rng, -radius, radius) * noise;
            double xx = x + noiseX, yy = y + noiseY;
            double label = 0;
            for (double[] g : GAUSSIANS) {
                double cx = g[0], cy = g[1], sign = g[2];
                double bell = clampLinear(dist(xx, yy, cx, cy), 0, 2, 1, 0);
                double newLabel = sign * bell;
                if (Math.abs(newLabel) > Math.abs(label)) {
                    label = newLabel;
                }
            }
            points.add(new Example2D(x, y, label));
        }
        return points;
    }
}
