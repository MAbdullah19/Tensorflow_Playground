package com.playground.nn;

import com.playground.nn.activation.ActivationFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single neuron in the network.
 *
 * <p><b>OOP concepts demonstrated</b>:
 * <ul>
 *   <li><b>Encapsulation</b> - every field is private. The internal state
 *       used by back-propagation is mutated through small, named methods
 *       ({@link #updateOutput()}, {@link #seedOutputDer(double)},
 *       {@link #computeAndAccumulateInputDer()},
 *       {@link #recomputeOutputDer()}, {@link #applyBiasUpdate(double)}),
 *       not by exposing raw setters.</li>
 *   <li><b>Composition</b> - a {@code Node} <i>has-a</i>
 *       {@link ActivationFunction} and a list of {@link Link}s; it is
 *       <i>not</i> a subclass of either.</li>
 *   <li><b>Polymorphism</b> - {@link #updateOutput()} calls
 *       {@code activation.output(...)}; the JVM dispatches to the right
 *       activation subclass at runtime without this class knowing the
 *       concrete type.</li>
 * </ul>
 */
public final class Node {

    private final String id;
    private final ActivationFunction activation;
    private final List<Link> inputLinks = new ArrayList<>();
    private final List<Link> outputs    = new ArrayList<>();

    private double bias;

    // --- transient training state ---------------------------------------
    private double totalInput;
    private double output;
    private double outputDer;
    private double inputDer;
    private double accInputDer;
    private int    numAccumulatedDers;

    public Node(String id, ActivationFunction activation, boolean initZero) {
        this.id = id;
        this.activation = activation;
        this.bias = initZero ? 0.0 : 0.1;
    }

    // --- read-only accessors --------------------------------------------

    public String getId()                       { return id; }
    public ActivationFunction getActivation()   { return activation; }
    public double getBias()                     { return bias; }
    public double getOutput()                   { return output; }
    public double getTotalInput()               { return totalInput; }
    public double getInputDer()                 { return inputDer; }
    public double getOutputDer()                { return outputDer; }

    /** Read-only view of incoming links. */
    public List<Link> getInputLinks() {
        return Collections.unmodifiableList(inputLinks);
    }

    /** Read-only view of outgoing links. */
    public List<Link> getOutputs() {
        return Collections.unmodifiableList(outputs);
    }

    // --- mutation entry points ------------------------------------------

    /** Manual bias override (used by the API "/bias" endpoint). */
    public void setBias(double bias) {
        this.bias = bias;
    }

    /** Used by the network builder to seed the input layer with raw values. */
    public void setOutput(double output) {
        this.output = output;
    }

    void addInputLink(Link link)  { inputLinks.add(link); }
    void addOutputLink(Link link) { outputs.add(link); }

    // --- forward / backward passes --------------------------------------

    /** Forward pass: weighted sum + activation. Returns the new output. */
    public double updateOutput() {
        double sum = bias;
        for (int j = 0; j < inputLinks.size(); j++) {
            Link link = inputLinks.get(j);
            sum += link.getWeight() * link.getSource().output;
        }
        totalInput = sum;
        output = activation.output(sum);
        return output;
    }

    /** Seed back-prop on the output node. {@code v == dE/d(output)}. */
    public void seedOutputDer(double v) {
        this.outputDer = v;
    }

    /**
     * Computes {@code inputDer = outputDer * f'(totalInput)} and adds it to
     * the running batch accumulator. Called once per node per back-prop pass.
     */
    public void computeAndAccumulateInputDer() {
        inputDer = outputDer * activation.derivative(totalInput);
        accInputDer += inputDer;
        numAccumulatedDers++;
    }

    /**
     * Recomputes {@code outputDer} for a hidden node by summing weighted
     * contributions from its outgoing links.
     */
    public void recomputeOutputDer() {
        double sum = 0.0;
        for (int j = 0; j < outputs.size(); j++) {
            Link out = outputs.get(j);
            sum += out.getWeight() * out.getDest().inputDer;
        }
        outputDer = sum;
    }

    /**
     * Applies the SGD update to the bias from the accumulated derivatives,
     * then resets the accumulator for the next batch.
     */
    public void applyBiasUpdate(double learningRate) {
        if (numAccumulatedDers > 0) {
            bias -= learningRate * accInputDer / numAccumulatedDers;
            accInputDer = 0;
            numAccumulatedDers = 0;
        }
    }

    @Override
    public String toString() {
        return "Node[" + id + ", bias=" + bias + ", activation=" + activation.getName() + "]";
    }
}
