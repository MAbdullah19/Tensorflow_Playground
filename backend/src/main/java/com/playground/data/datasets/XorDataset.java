package com.playground.data.datasets;

import com.playground.data.Example2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** XOR layout: the two diagonals of the plane have opposite labels. */
public final class XorDataset extends DataGenerator {

    public XorDataset() {
        super("xor", false);
    }

    @Override
    public List<Example2D> generate(int numSamples, double noise, Random rng) {
        List<Example2D> points = new ArrayList<>();
        for (int i = 0; i < numSamples; i++) {
            double x = randUniform(rng, -5, 5);
            double padding = 0.3;
            x += x > 0 ? padding : -padding;
            double y = randUniform(rng, -5, 5);
            y += y > 0 ? padding : -padding;
            double noiseX = randUniform(rng, -5, 5) * noise;
            double noiseY = randUniform(rng, -5, 5) * noise;
            double label = ((x + noiseX) * (y + noiseY) >= 0) ? 1.0 : -1.0;
            points.add(new Example2D(x, y, label));
        }
        return points;
    }
}
