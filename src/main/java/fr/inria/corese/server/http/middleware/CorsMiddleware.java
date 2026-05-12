package fr.inria.corese.server.http.middleware;

import io.javalin.http.Context;

/**
 * Adds CORS headers to all responses.
 * Required for browser-based SPARQL clients (e.g. SPARQL editors running on a
 * different origin than the server).
 * permissive — allows any origin.
 */
public class CorsMiddleware {

    /**
     * Creates a permissive CORS middleware
     */
    public CorsMiddleware() {
    }

    /**
     * Adds CORS headers to the response and short-circuits OPTIONS preflight requests.
     *
     * @param ctx the current Javalin request context
     */
    public void apply(Context ctx) {
        ctx.header("Access-Control-Allow-Origin", "*");
        ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ctx.header("Access-Control-Allow-Headers", "Content-Type, Accept, Authorization");

        // Handle CORS preflight (OPTIONS) — return immediately
        if ("OPTIONS".equalsIgnoreCase(ctx.method().name())) {
            ctx.status(204).result("");
        }
    }
}
