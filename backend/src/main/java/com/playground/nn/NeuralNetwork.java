package com.playground.nn;

import com.playground.exceptions.ConfigurationException;
import com.playground.nn.activation.ActivationFunction;
import com.playground.nn.regularization.RegularizationFunction;
import com.playground.nn.regularization.Regularizations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Static utilities for assembling and training a fully-connected feed-forward
 * neural network.
 *
 * <p><b>OOP role</b>: this is a thin orchestrator. The interesting state and
 * behaviour live on {@link Node}, {@link Link}, {@link ActivationFunction}
 * and {@link RegularizationFunction}; the methods here just walk the layered
 * structure and let polymorphism do its job. Compare with the original
 * TypeScript version where forward/back-prop manually mutated nested fields
 * on the data structures.
 */
public final class NeuralNetwork {

    private NeuralNetwork() { /* no instances */ }

    /**
     * Builds a fully connected network.
     *
     * @param networkShape       sizes of each layer including input and output
     * @param activation         hidden-layer activation function
     * @param outputActivation   activation used for the (single) output node
     * @param regularization     regularizer applied to every weight (may be
     *                           {@code null}, in which case {@code NONE} is used)
     * @param inputIds           ids assigned to the input layer nodes
     * @param initZero           if true, all weights and biases start at 0
     * @param rng                random source used for the initial weights
     */
    public static List<List<Node>> buildNetwork(
            int[] networkShape,
            ActivationFunction activation,
            ActivationFunction outputActivation,
            RegularizationFunction regularization,
            String[] inputIds,
            boolean initZero,
            Random rng) {

        if (networkShape == null || networkShape.length < 2) {
            throw new ConfigurationException("Network must have at least input + output layers");
        }
        if (inputIds.length != networkShape[0]) {
            throw new ConfigurationException(
                    "inputIds length (" + inputIds.length + ") must match input layer size ("
                            + networkShape[0] + ")");
        }
        RegularizationFunction reg = (regularization == null) ? Regularizations.NONE : regularization;

        int numLayers = networkShape.length;
        int id = 1;
        List<List<Node>> network = new ArrayList<>(numLayers);

        for (int layerIdx = 0; layerIdx < numLayers; layerIdx++) {
            boolean isOutputLayer = layerIdx == numLayers - 1;
            boolean isInputLayer = layerIdx == 0;
            List<Node> currentLayer = new ArrayList<>();
            network.add(currentLayer);

            int numNodes = networkShape[layerIdx];
            for (int i = 0; i < numNodes; i++) {
                String nodeId;
                if (isInputLayer) {
                    nodeId = inputIds[i];
                } else {
                    nodeId = Integer.toString(id);
                    id++;
                }
                Node node = new Node(nodeId, isOutputLayer ? outputActivation : activation, initZero);
                currentLayer.add(node);
                if (layerIdx >= 1) {
                    List<Node> prevLayer = network.get(layerIdx - 1);
                    for (Node prev : prevLayer) {
                        Link link = new Link(prev, node, reg, initZero, rng);
                        prev.addOutputLink(link);
                        node.addInputLink(link);
                    }
                }
            }
        }
        return network;
    }

    /**
     * Forward pass. Returns the network's output value (single output node).
     *
     * <p>The actual computation lives on {@link Node#updateOutput()} -
     * polymorphism takes care of dispatching to the right activation.
     */
    public static double forwardProp(List<List<Node>> network, double[] inputs) {
        List<Node> inputLayer = network.get(0);
        if (inputs.length != inputLayer.size()) {
            throw new ConfigurationException(
                    "Input length " + inputs.length + " does not match input layer size "
                            + inputLayer.size());
        }
        for (int i = 0; i < inputLayer.size(); i++) {
            inputLayer.get(i).setOutput(inputs[i]);
        }
        for (int layerIdx = 1; layerIdx < network.size(); layerIdx++) {
            List<Node> layer = network.get(layerIdx);
            for (int i = 0; i < layer.size(); i++) {
                layer.get(i).updateOutput();
            }
        }
        return network.get(network.size() - 1).get(0).getOutput();
    }

    /**
     * Backward pass. Updates the gradient accumulators on every Node and Link
     * but does <i>not</i> change weights yet (that's {@link #updateWeights}).
     */
    public static void backProp(List<List<Node>> network, double target, ErrorFunction errorFunc) {
        Node outputNode = network.get(network.size() - 1).get(0);
        outputNode.seedOutputDer(errorFunc.derivative(outputNode.getOutput(), target));

        for (int layerIdx = network.size() - 1; layerIdx >= 1; layerIdx--) {
            List<Node> currentLayer = network.get(layerIdx);

            for (int i = 0; i < currentLayer.size(); i++) {
                currentLayer.get(i).computeAndAccumulateInputDer();
            }
            for (int i = 0; i < currentLayer.size(); i++) {
                Node node = currentLayer.get(i);
                for (int j = 0; j < node.getInputLinks().size(); j++) {
                    node.getInputLinks().get(j).computeAndAccumulateErrorDer();
                }
            }
            if (layerIdx == 1) {
                continue;
            }
            List<Node> prevLayer = network.get(layerIdx - 1);
            for (int i = 0; i < prevLayer.size(); i++) {
                prevLayer.get(i).recomputeOutputDer();
            }
        }
    }

    /** Applies one SGD update from the currently accumulated gradients. */
    public static void updateWeights(List<List<Node>> network,
                                     double learningRate,
                                     double regularizationRate) {
        for (int layerIdx = 1; layerIdx < network.size(); layerIdx++) {
            List<Node> currentLayer = network.get(layerIdx);
            for (int i = 0; i < currentLayer.size(); i++) {
                Node node = currentLayer.get(i);
                node.applyBiasUpdate(learningRate);
                for (int j = 0; j < node.getInputLinks().size(); j++) {
                    node.getInputLinks().get(j).applyWeightUpdate(learningRate, regularizationRate);
                }
            }
        }
    }

    /** Iterates over every node in the network (optionally skipping inputs). */
    public static void forEachNode(List<List<Node>> network, boolean ignoreInputs, Consumer<Node> action) {
        for (int layerIdx = ignoreInputs ? 1 : 0; layerIdx < network.size(); layerIdx++) {
            List<Node> layer = network.get(layerIdx);
            for (int i = 0; i < layer.size(); i++) {
                action.accept(layer.get(i));
            }
        }
    }

    public static Node getOutputNode(List<List<Node>> network) {
        return network.get(network.size() - 1).get(0);
    }
}
