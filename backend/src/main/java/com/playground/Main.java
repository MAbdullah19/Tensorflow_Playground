package com.playground;

import com.playground.api.ApiServer;

/**
 * Entry point. Picks up the listen port from {@code --port=NNNN},
 * the {@code PORT} environment variable, or defaults to 8080.
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) throws Exception {
        int port = resolvePort(args);
        ApiServer server = new ApiServer(port);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "api-shutdown"));
    }

    private static int resolvePort(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                try { return Integer.parseInt(arg.substring("--port=".length())); }
                catch (NumberFormatException ignored) { /* fall through */ }
            }
        }
        String env = System.getenv("PORT");
        if (env != null && !env.isEmpty()) {
            try { return Integer.parseInt(env); } catch (NumberFormatException ignored) { /* fall through */ }
        }
        return 8080;
    }
}
