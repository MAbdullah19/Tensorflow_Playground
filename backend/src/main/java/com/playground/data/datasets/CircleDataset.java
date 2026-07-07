package com.playground.data.datasets;

import com.playground.data.Example2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Two concentric clouds: positive samples inside a small inner circle,
 *  negatives in an outer ring. */
public final class CircleDataset extends DataGenerator {

    public CircleDataset() {
        super("circle", false);
    }

    @Override
    public List<Example2D> generate(int numSamples, double noise, Random rng) {
        List<Example2D> points = new ArrayList<>();
        double radius = 5;

        // Positive samples - inner circle.
        for (int i = 0; i < numSamples / 2; i++) {
            double r = randUniform(rng, 0, radius * 0.5);
            double angle = randUniform(rng, 0, 2 * Math.PI);
            double x = r * Math.sin(angle);
            double y = r * Math.cos(angle);
            double noiseX = randUniform(rng, -radius, radius) * noise;
            double noiseY = randUniform(rng, -radius, radius) * noise;
            double label = circleLabel(x + noiseX, y + noiseY, radius);
            points.add(new Example2D(x, y, label));
        }
        // Negative samples - outer ring.
        for (int i = 0; i < numSamples / 2; i++) {
            double r = randUniform(rng, radius * 0.7, radius);
            double angle = randUniform(rng, 0, 2 * Math.PI);
            double x = r * Math.sin(angle);
            double y = r * Math.cos(angle);
            double noiseX = randUniform(rng, -radius, radius) * noise;
            double noiseY = randUniform(rng, -radius, radius) * noise;
            double label = circleLabel(x + noiseX, y + noiseY, radius);
            points.add(new Example2D(x, y, label));
        }
        return points;
    }

    private static double circleLabel(double x, double y, double radius) {
        return dist(x, y, 0, 0) < (radius * 0.5) ? 1.0 : -1.0;
    }
}
