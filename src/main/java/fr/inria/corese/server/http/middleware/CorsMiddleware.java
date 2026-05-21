package fr.inria.corese.server.http.middleware;

import io.javalin.http.Context;

/**
 * Adds CORS headers to all responses.
 * Required for browser-based SPARQL clients (e.g. SPARQL editors running on a
 * different origin than the server).
 * permissive — allows any origin.
 */
public class CorsMiddleware {
    private static final String SERVER_PREFIX = "Corese/";
    private final String serverHeader;


    /**
     * Creates a permissive CORS middleware with Corese server identification.
     *
     * @param version the server version (used in the Server header)
     */
    public CorsMiddleware(String version) {
        this.serverHeader = SERVER_PREFIX + version;
    }

    /**
     * Adds CORS headers to the response and short-circuits OPTIONS preflight requests.
     *
     * @param ctx the current Javalin request context
     */
    public void apply(Context ctx) {
        // CORS headers
        ctx.header("Access-Control-Allow-Origin", "*");
        ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ctx.header("Access-Control-Allow-Headers", "Content-Type, Accept, Authorization");

        ctx.header("Server", serverHeader);

        // Handle CORS preflight (OPTIONS) — return immediately
        if ("OPTIONS".equalsIgnoreCase(ctx.method().name())) {
            ctx.status(204).result("");
        }
    }
}
