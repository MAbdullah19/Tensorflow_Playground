package com.playground.data;

/**
 * Immutable training example: an (x, y) coordinate paired with its label.
 *
 * <p><b>OOP concepts demonstrated</b>:
 * <ul>
 *   <li><b>Encapsulation</b> - the three values are stored in {@code private
 *       final} fields and exposed only through getters. The class is itself
 *       declared {@code final} so it cannot be subclassed and broken.</li>
 *   <li><b>Immutability</b> - once constructed, an {@code Example2D} cannot
 *       change, which makes it safe to share between threads (used by
 *       concurrent training sessions).</li>
 * </ul>
 */
public final class Example2D {

    private final double x;
    private final double y;
    private final double label;

    public Example2D(double x, double y, double label) {
        this.x = x;
        this.y = y;
        this.label = label;
    }

    public double getX()     { return x; }
    public double getY()     { return y; }
    public double getLabel() { return label; }

    @Override
    public String toString() {
        return "Example2D{x=" + x + ", y=" + y + ", label=" + label + "}";
    }
}
