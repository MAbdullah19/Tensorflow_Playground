package com.playground.api;

import com.playground.data.Example2D;
import com.playground.data.InputFeatures;
import com.playground.data.InputFeatures.Feature;
import com.playground.data.datasets.DataGenerator;
import com.playground.data.datasets.DatasetRegistry;
import com.playground.exceptions.ConfigurationException;
import com.playground.exceptions.NotFoundException;
import com.playground.exceptions.TrainingException;
import com.playground.nn.Link;
import com.playground.nn.NeuralNetwork;
import com.playground.nn.Node;
import com.playground.nn.activation.Activations;
import com.playground.nn.regularization.Regularizations;
import com.playground.util.Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * REST endpoints exposed by the playground backend.
 *
 * <p><b>OOP role</b>: this class is a stateless façade in front of the rich
 * domain model. It only ever talks to {@link Session} via the encapsulated
 * setters / domain operations - it never reaches into the network state
 * directly. The custom exception hierarchy ({@link ConfigurationException},
 * {@link NotFoundException}, {@link TrainingException}, ...) flows out
 * untouched and is translated to HTTP status codes by {@link ApiServer}.
 */
public final class RequestHandlers {

    private final SessionManager sessions;

    public RequestHandlers(SessionManager sessions) {
        this.sessions = sessions;
    }

    public ApiServer.ApiResponse handle(String method, String path, String body) {
        // Route table:
        //   POST   /api/sessions
        //   DELETE /api/sessions/{id}
        //   GET    /api/sessions/{id}/state
        //   POST   /api/sessions/{id}/configure
        //   POST   /api/sessions/{id}/regenerate-data
        //   POST   /api/sessions/{id}/build-network
        //   POST   /api/sessions/{id}/train
        //   POST   /api/sessions/{id}/boundary
        //   POST   /api/sessions/{id}/weight
        //   POST   /api/sessions/{id}/bias

        if ("/api/sessions".equals(path) && "POST".equalsIgnoreCase(method)) {
            return createSession(parseBody(body));
        }
        if (path.startsWith("/api/sessions/")) {
            String[] parts = path.substring("/api/sessions/".length()).split("/");
            String id = parts[0];
            String action = parts.length >= 2 ? parts[1] : "";
            if (parts.length == 1 && "DELETE".equalsIgnoreCase(method)) {
                return deleteSession(id);
            }
            ReentrantLock lock = sessions.requireLockFor(id);
            lock.lock();
            try {
                Session session = sessions.require(id);
                Map<String, Object> req = parseBody(body);
                switch (action) {
                    case "state":             return ApiServer.ApiResponse.ok(Json.encode(snapshot(session, true)));
                    case "configure":         return configure(session, req);
                    case "regenerate-data":   return regenerateData(session, req);
                    case "build-network":     return buildNetwork(session, req);
                    case "train":             return train(session, req);
                    case "boundary":          return boundary(session, req);
                    case "weight":            return setWeight(session, req);
                    case "bias":              return setBias(session, req);
                    default:
                        throw new NotFoundException("Unknown action: " + action);
                }
            } finally {
                lock.unlock();
            }
        }
        throw new NotFoundException("Not found: " + method + " " + path);
    }

    // ------------------------------------------------------------------
    //  Session lifecycle
    // ------------------------------------------------------------------

    private ApiServer.ApiResponse createSession(Map<String, Object> req) {
        Session session = sessions.create();
        applyConfiguration(session, req);
        session.regenerateData();
        session.rebuildNetwork();
        return ApiServer.ApiResponse.ok(Json.encode(snapshot(session, true)));
    }

    private ApiServer.ApiResponse deleteSession(String id) {
        boolean removed = sessions.delete(id);
        if (!removed) throw new NotFoundException("Unknown session: " + id);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("deleted", true);
        return ApiServer.ApiResponse.ok(Json.encode(resp));
    }

    // ------------------------------------------------------------------
    //  Configuration / data / network
    // ------------------------------------------------------------------

    private ApiServer.ApiResponse configure(Session session, Map<String, Object> req) {
        boolean dataChanged    = applyDataConfiguration(session, req);
        boolean networkChanged = applyNetworkConfiguration(session, req);

        if (dataChanged)                           session.regenerateData();
        if (networkChanged || dataChanged)         session.rebuildNetwork();
        return ApiServer.ApiResponse.ok(Json.encode(snapshot(session, true)));
    }

    private ApiServer.ApiResponse regenerateData(Session session, Map<String, Object> req) {
        if (req.containsKey("dataSeed")) {
            session.setDataSeed(Json.getInt(req, "dataSeed", (int) session.getDataSeed()));
        } else {
            session.setDataSeed((long) (Math.random() * Long.MAX_VALUE));
        }
        applyConfiguration(session, req);
        session.regenerateData();
        session.rebuildNetwork();
        return ApiServer.ApiResponse.ok(Json.encode(snapshot(session, true)));
    }

    private ApiServer.ApiResponse buildNetwork(Session session, Map<String, Object> req) {
        if (req.containsKey("networkSeed")) {
            session.setNetworkSeed(Json.getInt(req, "networkSeed", (int) session.getNetworkSeed()));
        } else {
            session.setNetworkSeed((long) (Math.random() * Long.MAX_VALUE));
        }
        applyConfiguration(session, req);
        session.rebuildNetwork();
        return ApiServer.ApiResponse.ok(Json.encode(snapshot(session, true)));
    }

    /** Both data + network knobs in one go. Used for the full {@code /configure} endpoint. */
    private void applyConfiguration(Session session, Map<String, Object> req) {
        applyDataConfiguration(session, req);
        applyNetworkConfiguration(session, req);
    }

    /** Returns true if any data-affecting field was touched. */
    private boolean applyDataConfiguration(Session session, Map<String, Object> req) {
        boolean changed = false;
        if (req.containsKey("dataset")) {
            session.setDataset(DatasetRegistry.byKey(Json.getString(req, "dataset", session.getDataset().getKey())));
            changed = true;
        }
        if (req.containsKey("regDataset")) {
            session.setRegDataset(DatasetRegistry.byKey(Json.getString(req, "regDataset", session.getRegDataset().getKey())));
            changed = true;
        }
        if (req.containsKey("problem")) {
            session.setProblem(Session.Problem.fromKey(Json.getString(req, "problem", session.getProblem().key())));
            changed = true;
        }
        if (req.containsKey("noise")) {
            session.setNoise(Json.getDouble(req, "noise", session.getNoise()));
            changed = true;
        }
        if (req.containsKey("percTrainData")) {
            session.setPercTrainData(Json.getDouble(req, "percTrainData", session.getPercTrainData()));
            changed = true;
        }
        return changed;
    }

    /** Returns true if any network-affecting field was touched. */
    private boolean applyNetworkConfiguration(Session session, Map<String, Object> req) {
        boolean changed = false;
        if (req.containsKey("activation")) {
            session.setActivation(Activations.byKey(Json.getString(req, "activation", session.getActivation().getName())));
            changed = true;
        }
        if (req.containsKey("regularization")) {
            session.setRegularization(Regularizations.byKey(Json.getString(req, "regularization", session.getRegularization().getName())));
            changed = true;
        }
        if (req.containsKey("initZero")) {
            session.setInitZero(Json.getBoolean(req, "initZero", session.isInitZero()));
            changed = true;
        }
        if (req.containsKey("networkShape")) {
            session.setNetworkShape(Json.toIntArray(Json.getList(req, "networkShape")));
            changed = true;
        }
        if (req.containsKey("features")) {
            List<String> keys = Json.toStringList(Json.getList(req, "features"));
            List<Feature> feats = InputFeatures.parseKeys(keys);
            session.setFeatures(feats);
            changed = true;
        }
        return changed;
    }

    // ------------------------------------------------------------------
    //  Training / boundary
    // ------------------------------------------------------------------

    private ApiServer.ApiResponse train(Session session, Map<String, Object> req) {
        int epochs = Math.max(1, Json.getInt(req, "epochs", 1));
        if (epochs > 5000) throw new ConfigurationException("epochs capped at 5000 per request");
        double learningRate     = Json.getDouble(req, "learningRate", 0.03);
        double regularizationRate = Json.getDouble(req, "regularizationRate", 0.0);
        int batchSize           = Math.max(1, Json.getInt(req, "batchSize", 10));

        try {
            for (int e = 0; e < epochs; e++) {
                session.trainOneEpoch(learningRate, regularizationRate, batchSize);
            }
        } catch (TrainingException te) {
            throw te; // already typed correctly
        } catch (RuntimeException re) {
            // Wrap any unexpected runtime error so the API surfaces it as 500
            // with a useful message rather than the bare stacktrace.
            throw new TrainingException("Training failed: " + re.getMessage(), re);
        }
        return ApiServer.ApiResponse.ok(Json.encode(snapshot(session, false)));
    }

    private ApiServer.ApiResponse boundary(Session session, Map<String, Object> req) {
        int density = clamp(Json.getInt(req, "density", 30), 4, 100);
        boolean discretize = Json.getBoolean(req, "discretize", false);
        List<String> nodeIds = Json.toStringList(Json.getList(req, "nodeIds"));
        boolean includeAll = nodeIds == null || nodeIds.isEmpty();

        double xMin = -6, xMax = 6, yMin = -6, yMax = 6;
        Map<String, double[][]> result = new LinkedHashMap<>();

        // Always preview every input-feature so the FeatureSelector can show
        // every option (active or not) using the same renderer as the rest.
        Map<String, double[][]> inputBoundary = new LinkedHashMap<>();
        for (Feature f : Feature.values()) {
            inputBoundary.put(f.getKey(), new double[density][density]);
        }

        // Internal node boundaries.
        Map<String, double[][]> nodeBoundary = new LinkedHashMap<>();
        if (session.getNetwork() != null) {
            NeuralNetwork.forEachNode(session.getNetwork(), true, n -> {
                if (includeAll || nodeIds.contains(n.getId())) {
                    nodeBoundary.put(n.getId(), new double[density][density]);
                }
            });
        }

        for (int i = 0; i < density; i++) {
            double x = xMin + (xMax - xMin) * (i / (double) (density - 1));
            for (int j = 0; j < density; j++) {
                // Match the original convention: y axis is inverted (top is +y).
                double y = yMax + (yMin - yMax) * (j / (double) (density - 1));
                // Update input-feature boundaries (compute for ALL features).
                for (Feature f : Feature.values()) {
                    double v = f.apply(x, y);
                    inputBoundary.get(f.getKey())[i][j] = discretize ? Math.signum(v) : v;
                }
                if (session.getNetwork() != null) {
                    double[] input = InputFeatures.build(session.getFeatures(), x, y);
                    NeuralNetwork.forwardProp(session.getNetwork(), input);
                    for (Map.Entry<String, double[][]> e : nodeBoundary.entrySet()) {
                        double v = nodeOutput(session, e.getKey());
                        e.getValue()[i][j] = discretize ? Math.signum(v) : v;
                    }
                }
            }
        }

        result.putAll(inputBoundary);
        result.putAll(nodeBoundary);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("density", density);
        resp.put("xDomain", new double[]{xMin, xMax});
        resp.put("yDomain", new double[]{yMin, yMax});
        Map<String, Object> boundaries = new LinkedHashMap<>();
        for (Map.Entry<String, double[][]> e : result.entrySet()) {
            boundaries.put(e.getKey(), e.getValue());
        }
        resp.put("boundaries", boundaries);
        return ApiServer.ApiResponse.ok(Json.encode(resp));
    }

    private static double nodeOutput(Session s, String id) {
        if (s.getNetwork() == null) return 0.0;
        for (List<Node> layer : s.getNetwork()) {
            for (Node n : layer) {
                if (n.getId().equals(id)) return n.getOutput();
            }
        }
        return 0.0;
    }

    private ApiServer.ApiResponse setWeight(Session session, Map<String, Object> req) {
        if (session.getNetwork() == null) {
            throw new ConfigurationException("Network has not been built");
        }
        String source = Json.getString(req, "source", null);
        String dest   = Json.getString(req, "dest", null);
        double weight = Json.getDouble(req, "weight", 0.0);
        if (source == null || dest == null) {
            throw new ConfigurationException("source and dest are required");
        }
        boolean updated = false;
        for (List<Node> layer : session.getNetwork()) {
            for (Node n : layer) {
                for (Link link : n.getInputLinks()) {
                    if (link.getSource().getId().equals(source) && link.getDest().getId().equals(dest)) {
                        link.setWeight(weight);
                        updated = true;
                    }
                }
            }
        }
        if (!updated) throw new NotFoundException("Link " + source + "->" + dest + " not found");
        session.recomputeLoss();
        return ApiServer.ApiResponse.ok(Json.encode(snapshot(session, false)));
    }

    private ApiServer.ApiResponse setBias(Session session, Map<String, Object> req) {
        if (session.getNetwork() == null) {
            throw new ConfigurationException("Network has not been built");
        }
        String id = Json.getString(req, "id", null);
        double bias = Json.getDouble(req, "bias", 0.0);
        if (id == null) throw new ConfigurationException("id is required");
        boolean updated = false;
        for (List<Node> layer : session.getNetwork()) {
            for (Node n : layer) {
                if (n.getId().equals(id)) {
                    n.setBias(bias);
                    updated = true;
                }
            }
        }
        if (!updated) throw new NotFoundException("Node " + id + " not found");
        session.recomputeLoss();
        return ApiServer.ApiResponse.ok(Json.encode(snapshot(session, false)));
    }

    // ------------------------------------------------------------------
    //  Snapshot serialisation
    // ------------------------------------------------------------------

    private static Map<String, Object> snapshot(Session session, boolean includeData) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("sessionId", session.getId());
        resp.put("iter", session.getIteration());
        resp.put("lossTrain", session.getTrainLoss());
        resp.put("lossTest", session.getTestLoss());

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("dataset", session.getDataset().getKey());
        config.put("regDataset", session.getRegDataset().getKey());
        config.put("problem", session.getProblem().key());
        config.put("noise", session.getNoise());
        config.put("percTrainData", session.getPercTrainData());
        config.put("activation", session.getActivation().getName());
        config.put("regularization", session.getRegularization().getName());
        config.put("initZero", session.isInitZero());
        config.put("networkShape", session.getNetworkShape());
        List<String> featureKeys = new ArrayList<>();
        for (Feature f : session.getFeatures()) featureKeys.add(f.getKey());
        config.put("features", featureKeys);
        resp.put("config", config);

        if (session.getNetwork() != null) {
            List<Object> layers = new ArrayList<>();
            for (List<Node> layer : session.getNetwork()) {
                List<Object> nodes = new ArrayList<>();
                for (Node n : layer) {
                    Map<String, Object> nObj = new LinkedHashMap<>();
                    nObj.put("id", n.getId());
                    nObj.put("bias", n.getBias());
                    nodes.add(nObj);
                }
                layers.add(nodes);
            }
            resp.put("layers", layers);

            List<Object> links = new ArrayList<>();
            for (int li = 1; li < session.getNetwork().size(); li++) {
                for (Node n : session.getNetwork().get(li)) {
                    for (Link link : n.getInputLinks()) {
                        Map<String, Object> l = new LinkedHashMap<>();
                        l.put("source", link.getSource().getId());
                        l.put("dest", link.getDest().getId());
                        l.put("weight", link.getWeight());
                        l.put("dead", link.isDead());
                        links.add(l);
                    }
                }
            }
            resp.put("links", links);
        }

        if (includeData) {
            resp.put("trainData", encodePoints(session.getTrainData()));
            resp.put("testData",  encodePoints(session.getTestData()));
        }
        return resp;
    }

    private static List<Object> encodePoints(List<Example2D> data) {
        List<Object> out = new ArrayList<>(data.size());
        for (Example2D p : data) {
            out.add(new double[]{p.getX(), p.getY(), p.getLabel()});
        }
        return out;
    }

    private static Map<String, Object> parseBody(String body) {
        if (body == null || body.isEmpty()) return new LinkedHashMap<>();
        try {
            return Json.parseObject(body);
        } catch (IllegalArgumentException iae) {
            throw new ConfigurationException("Malformed JSON body: " + iae.getMessage(), iae);
        }
    }

    private static int clamp(int value, int lo, int hi) {
        return Math.max(lo, Math.min(hi, value));
    }
}
