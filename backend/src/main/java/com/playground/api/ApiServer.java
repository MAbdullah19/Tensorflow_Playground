package com.playground.api;

import com.playground.exceptions.PlaygroundException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Bootstraps the embedded {@link HttpServer} that serves the playground REST API.
 *
 * <p><b>OOP role</b>: this class owns one {@link SessionManager} (composition)
 * and one {@link RequestHandlers} dispatcher. The interesting OOP moment is in
 * {@link #dispatch(HttpExchange)}: catching the abstract
 * {@link PlaygroundException} and reading {@code getHttpStatus()} works for
 * <i>every</i> subclass without any {@code instanceof} - that's polymorphism
 * doing real work.
 */
public final class ApiServer {

    private final SessionManager sessions = new SessionManager();
    private final RequestHandlers handlers = new RequestHandlers(sessions);
    private HttpServer server;
    private final int port;

    public ApiServer(int port) { this.port = port; }

    public void start() throws IOException {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (BindException be) {
            System.err.println();
            System.err.println("ERROR: port " + port + " is already in use.");
            System.err.println("Another playground instance (or some other service) is already");
            System.err.println("listening on this port. Either stop it, or pick a different port:");
            System.err.println();
            System.err.println("    ./run.sh --port=8081");
            System.err.println();
            System.err.println("To find the offender on Windows:  netstat -ano | findstr :" + port);
            System.err.println("To find it on Linux/macOS:        lsof -i :" + port);
            System.err.println();
            throw be;
        }
        server.createContext("/api/", this::dispatch);
        server.createContext("/health", exchange -> {
            byte[] body = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            applyCors(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });
        // Bounded thread pool: parallelize across sessions but don't drown the
        // host. 8 workers covers reasonable concurrency for a single-user dev
        // setup while still leaving CPU for the JVM.
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Playground backend listening on http://localhost:" + port);
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    private void dispatch(HttpExchange exchange) throws IOException {
        try {
            applyCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String body = readBody(exchange);

            ApiResponse response;
            try {
                response = handlers.handle(method, path, body);
            } catch (PlaygroundException pe) {
                // Polymorphic dispatch: every subclass carries its own status.
                response = ApiResponse.error(pe.getHttpStatus(), pe.getMessage());
            } catch (IllegalArgumentException iae) {
                // Defensive fallback for stdlib argument errors.
                response = ApiResponse.error(400, iae.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
                response = ApiResponse.error(500, "Internal server error: " + ex.getMessage());
            }
            byte[] bytes = response.body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(response.status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        } catch (Throwable t) {
            t.printStackTrace();
            try { exchange.close(); } catch (Exception ignored) { /* */ }
        }
    }

    private static void applyCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Accept");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Max-Age", "3600");
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        // try-with-resources guarantees the request body stream is closed even
        // if reading throws.
        try (InputStream is = exchange.getRequestBody()) {
            byte[] all = is.readAllBytes();
            return new String(all, StandardCharsets.UTF_8);
        }
    }

    /** Response envelope used by handlers. */
    public static final class ApiResponse {
        public final int status;
        public final String body;
        private ApiResponse(int status, String body) { this.status = status; this.body = body; }
        public static ApiResponse ok(String json) { return new ApiResponse(200, json); }
        public static ApiResponse error(int status, String message) {
            String escaped = message == null ? "" : message.replace("\\", "\\\\").replace("\"", "\\\"");
            return new ApiResponse(status, "{\"error\":\"" + escaped + "\"}");
        }
    }
}
