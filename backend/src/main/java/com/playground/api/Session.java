package com.playground.api;

import com.playground.data.Example2D;
import com.playground.data.InputFeatures;
import com.playground.data.InputFeatures.Feature;
import com.playground.data.datasets.DataGenerator;
import com.playground.data.datasets.DatasetRegistry;
import com.playground.exceptions.ConfigurationException;
import com.playground.exceptions.TrainingException;
import com.playground.nn.NeuralNetwork;
import com.playground.nn.Node;
import com.playground.nn.SquaredError;
import com.playground.nn.activation.ActivationFunction;
import com.playground.nn.activation.Activations;
import com.playground.nn.regularization.RegularizationFunction;
import com.playground.nn.regularization.Regularizations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * One playground session: dataset, network, and current training state.
 *
 * <p><b>OOP concepts demonstrated</b>:
 * <ul>
 *   <li><b>Encapsulation</b> - every mutable piece of configuration has a
 *       getter and (where appropriate) a setter that validates input. The
 *       network state cannot be reached except through {@link #getNetwork()}
 *       which is read-only.</li>
 *   <li><b>Composition</b> - a Session <i>has-a</i> {@link DataGenerator},
 *       <i>has-a</i> {@link ActivationFunction}, <i>has-a</i>
 *       {@link RegularizationFunction}, and <i>has-a</i> network of Nodes.</li>
 *   <li><b>Interface implementation</b> - the class implements
 *       {@link Trainable}, so anything that just needs to step the network
 *       can program against the interface.</li>
 *   <li><b>Method overloading</b> - {@link #trainOneEpoch(double, double, int)}
 *       and {@link #trainOneEpoch()} share a name and differ only in their
 *       parameter list.</li>
 * </ul>
 */
public final class Session implements Trainable {

    public enum Problem {
        CLASSIFICATION, REGRESSION;

        public static Problem fromKey(String k) {
            if (k == null) return CLASSIFICATION;
            switch (k.toLowerCase()) {
                case "classification": return CLASSIFICATION;
                case "regression":     return REGRESSION;
                default: throw new ConfigurationException("Unknown problem: " + k);
            }
        }

        public String key() {
            return this == CLASSIFICATION ? "classification" : "regression";
        }
    }

    // -------- identity --------
    private final String id;

    // -------- configuration ---------------------------------------------
    private DataGenerator dataset    = DatasetRegistry.CIRCLE;
    private DataGenerator regDataset = DatasetRegistry.REG_PLANE;
    private Problem problem          = Problem.CLASSIFICATION;
    private double noise             = 0.0;
    private double percTrainData     = 50.0;

    private int[] networkShape                     = {4, 2};
    private ActivationFunction activation          = Activations.TANH;
    private RegularizationFunction regularization  = Regularizations.NONE;
    private boolean initZero                       = false;
    private List<Feature> features                 = defaultFeatures();

    private long dataSeed;
    private long networkSeed;

    // -------- runtime state ---------------------------------------------
    private List<Example2D> trainData = new ArrayList<>();
    private List<Example2D> testData  = new ArrayList<>();
    private List<List<Node>> network;
    private int iter = 0;
    private double lossTrain = 0.0;
    private double lossTest  = 0.0;

    public Session(String id, long initialSeed) {
        this.id = id;
        this.dataSeed = initialSeed;
        this.networkSeed = initialSeed ^ 0xC0FFEEL;
    }

    public static List<Feature> defaultFeatures() {
        List<Feature> list = new ArrayList<>();
        list.add(Feature.X);
        list.add(Feature.Y);
        return list;
    }

    // -------- accessors --------------------------------------------------

    public String getId()                                  { return id; }
    public DataGenerator getDataset()                      { return dataset; }
    public DataGenerator getRegDataset()                   { return regDataset; }
    public Problem getProblem()                            { return problem; }
    public double getNoise()                               { return noise; }
    public double getPercTrainData()                       { return percTrainData; }
    public int[] getNetworkShape()                         { return networkShape.clone(); }
    public ActivationFunction getActivation()              { return activation; }
    public RegularizationFunction getRegularization()      { return regularization; }
    public boolean isInitZero()                            { return initZero; }
    public List<Feature> getFeatures()                     { return Collections.unmodifiableList(features); }
    public long getDataSeed()                              { return dataSeed; }
    public long getNetworkSeed()                           { return networkSeed; }
    public List<Example2D> getTrainData()                  { return Collections.unmodifiableList(trainData); }
    public List<Example2D> getTestData()                   { return Collections.unmodifiableList(testData); }
    public List<List<Node>> getNetwork()                   { return network; }

    @Override public int    getIteration()                 { return iter; }
    @Override public double getTrainLoss()                 { return lossTrain; }
    @Override public double getTestLoss()                  { return lossTest; }

    // -------- mutators (validate input, then assign) --------------------

    public void setDataset(DataGenerator d) {
        if (d == null) throw new ConfigurationException("dataset must not be null");
        if (d.isRegression()) throw new ConfigurationException("'" + d.getKey() + "' is a regression dataset");
        this.dataset = d;
    }

    public void setRegDataset(DataGenerator d) {
        if (d == null) throw new ConfigurationException("regDataset must not be null");
        if (!d.isRegression()) throw new ConfigurationException("'" + d.getKey() + "' is not a regression dataset");
        this.regDataset = d;
    }

    public void setProblem(Problem p) {
        if (p == null) throw new ConfigurationException("problem must not be null");
        this.problem = p;
    }

    public void setNoise(double n) {
        if (n < 0 || n > 50) throw new ConfigurationException("noise must be in [0, 50]");
        this.noise = n;
    }

    public void setPercTrainData(double p) {
        if (p < 10 || p > 90) throw new ConfigurationException("percTrainData must be in [10, 90]");
        this.percTrainData = p;
    }

    public void setNetworkShape(int[] shape) {
        if (shape == null) throw new ConfigurationException("networkShape must not be null");
        if (shape.length > 8) throw new ConfigurationException("At most 8 hidden layers supported");
        for (int n : shape) {
            if (n < 1 || n > 16) throw new ConfigurationException("Layer size must be in [1, 16]");
        }
        this.networkShape = shape.clone();
    }

    public void setActivation(ActivationFunction a) {
        if (a == null) throw new ConfigurationException("activation must not be null");
        this.activation = a;
    }

    public void setRegularization(RegularizationFunction r) {
        this.regularization = (r == null) ? Regularizations.NONE : r;
    }

    public void setInitZero(boolean v) {
        this.initZero = v;
    }

    public void setFeatures(List<Feature> features) {
        if (features == null || features.isEmpty()) {
            throw new ConfigurationException("At least one feature must be enabled");
        }
        this.features = new ArrayList<>(features);
    }

    public void setDataSeed(long seed)    { this.dataSeed = seed; }
    public void setNetworkSeed(long seed) { this.networkSeed = seed; }

    // -------- domain operations -----------------------------------------

    public DataGenerator activeDataset() {
        return problem == Problem.REGRESSION ? regDataset : dataset;
    }

    public int defaultNumSamples() {
        return problem == Problem.REGRESSION ? 1200 : 500;
    }

    /** Regenerates train/test data based on the current configuration. */
    public void regenerateData() {
        Random rng = new Random(dataSeed);
        int numSamples = defaultNumSamples();
        // The "noise" slider in the UI is 0-50; the original code feeds the
        // generator a value in [0, 0.5]. We preserve that scaling.
        List<Example2D> all = activeDataset().generate(numSamples, noise / 100.0, rng);
        Collections.shuffle(all, rng);
        int splitIndex = (int) Math.floor(all.size() * percTrainData / 100.0);
        if (splitIndex < 1) splitIndex = 1;
        if (splitIndex > all.size() - 1) splitIndex = all.size() - 1;
        trainData = new ArrayList<>(all.subList(0, splitIndex));
        testData  = new ArrayList<>(all.subList(splitIndex, all.size()));
    }

    /** Rebuilds the network using the current configuration. */
    public void rebuildNetwork() {
        if (features.isEmpty()) {
            throw new ConfigurationException("At least one input feature must be enabled");
        }
        ActivationFunction outputActivation =
                problem == Problem.REGRESSION ? Activations.LINEAR : Activations.TANH;
        int numInputs = features.size();
        int hidden = (networkShape == null) ? 0 : networkShape.length;
        int[] shape = new int[hidden + 2];
        shape[0] = numInputs;
        for (int i = 0; i < hidden; i++) shape[i + 1] = networkShape[i];
        shape[shape.length - 1] = 1;

        String[] inputIds = new String[numInputs];
        for (int i = 0; i < numInputs; i++) inputIds[i] = features.get(i).getKey();

        Random rng = new Random(networkSeed);
        network = NeuralNetwork.buildNetwork(shape, activation,
                outputActivation, regularization, inputIds, initZero, rng);
        iter = 0;
        recomputeLoss();
    }

    /** Recomputes train/test loss without performing any updates. */
    public void recomputeLoss() {
        lossTrain = computeLoss(trainData);
        lossTest  = computeLoss(testData);
    }

    private double computeLoss(List<Example2D> data) {
        if (data.isEmpty() || network == null) return 0.0;
        double loss = 0.0;
        for (int i = 0; i < data.size(); i++) {
            Example2D p = data.get(i);
            double[] input = InputFeatures.build(features, p.getX(), p.getY());
            double output = NeuralNetwork.forwardProp(network, input);
            loss += SquaredError.INSTANCE.error(output, p.getLabel());
        }
        return loss / data.size();
    }

    // ----- Trainable implementation -------------------------------------

    /**
     * Train one epoch with the supplied hyper-parameters.
     */
    @Override
    public void trainOneEpoch(double learningRate, double regularizationRate, int batchSize) {
        if (network == null) {
            throw new TrainingException("Network not built; call rebuildNetwork() first");
        }
        if (trainData.isEmpty()) return;
        int batch = Math.max(1, batchSize);
        iter++;
        for (int i = 0; i < trainData.size(); i++) {
            Example2D p = trainData.get(i);
            double[] input = InputFeatures.build(features, p.getX(), p.getY());
            NeuralNetwork.forwardProp(network, input);
            NeuralNetwork.backProp(network, p.getLabel(), SquaredError.INSTANCE);
            if ((i + 1) % batch == 0) {
                NeuralNetwork.updateWeights(network, learningRate, regularizationRate);
            }
        }
        recomputeLoss();
    }

    /**
     * Convenience overload using the original Tensorflow Playground defaults
     * (lr=0.03, no regularisation, batch size 10). Demonstrates
     * <b>method overloading</b>.
     */
    public void trainOneEpoch() {
        trainOneEpoch(0.03, 0.0, 10);
    }
}
