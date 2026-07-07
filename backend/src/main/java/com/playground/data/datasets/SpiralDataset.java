package com.playground.data.datasets;

import com.playground.data.Example2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Two interleaved Archimedean spirals - the iconic "hard" playground task. */
public final class SpiralDataset extends DataGenerator {

    public SpiralDataset() {
        super("spiral", false);
    }

    @Override
    public List<Example2D> generate(int numSamples, double noise, Random rng) {
        List<Example2D> points = new ArrayList<>();
        int n = numSamples / 2;
        addSpiral(points, n, 0,        1.0, noise, rng);
        addSpiral(points, n, Math.PI, -1.0, noise, rng);
        return points;
    }

    private static void addSpiral(List<Example2D> points, int n,
                                  double deltaT, double label,
                                  double noise, Random rng) {
        for (int i = 0; i < n; i++) {
            double r = (double) i / n * 5;
            double t = 1.75 * i / n * 2 * Math.PI + deltaT;
            double x = r * Math.sin(t) + randUniform(rng, -1, 1) * noise;
            double y = r * Math.cos(t) + randUniform(rng, -1, 1) * noise;
            points.add(new Example2D(x, y, label));
        }
    }
}
