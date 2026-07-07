package com.playground.data.datasets;

import com.playground.data.Example2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Linear regression target: {@code label = clamp((x + y) / 10, -1, 1)}. */
public final class PlaneRegressionDataset extends DataGenerator {

    public PlaneRegressionDataset() {
        super("reg-plane", true);
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
            double label = clampLinear(x + noiseX + y + noiseY, -10, 10, -1, 1);
            points.add(new Example2D(x, y, label));
        }
        return points;
    }
}
