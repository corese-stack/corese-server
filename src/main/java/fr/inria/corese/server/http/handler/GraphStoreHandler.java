package fr.inria.corese.server.http.handler;

import fr.inria.corese.server.http.model.SparqlResponse;
import fr.inria.corese.server.service.GraphStoreService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP handler for the SPARQL 1.1 Graph Store HTTP Protocol.
 */
public class GraphStoreHandler {

    private static final Logger log = LoggerFactory.getLogger(GraphStoreHandler.class);

    private final GraphStoreService service;

    /**
     * Creates a new GraphStoreHandler.
     *
     * @param service the graph store service to delegate operations to
     */
    public GraphStoreHandler(GraphStoreService service) {
        this.service = service;
    }


    /**
     * Check existence of a named graph without retrieving its content.
     * Returns 200 if the graph exists, 404 if not.
     *
     * @param ctx the Javalin request context
     */
    public void head(Context ctx) {
        String graphUri = extractGraphUri(ctx);
        if (graphUri == null) {
            ctx.status(400);
            return;
        }
        boolean exists = service.graphExists(graphUri);
        ctx.status(exists ? 200 : 404);
        log.debug("HEAD graph <{}> → {}", graphUri, exists ? 200 : 404);
    }


    /**
     * Retrieve a named graph, or list all graphs if no graph parameter.
     *
     * @param ctx the Javalin request context
     */
    public void get(Context ctx) {
        String graphUri = extractGraphUri(ctx);

        if (graphUri == null && !ctx.queryParamMap().containsKey("graph")
                && !ctx.queryParamMap().containsKey("default")) {
            SparqlResponse response = service.listGraphs();
            writeResponse(ctx, response);
            return;
        }

        if (graphUri == null) {
            ctx.status(400).result(missingGraphParam());
            return;
        }
        SparqlResponse response = service.getGraph(graphUri, ctx.header("Accept"));
        writeResponse(ctx, response);
    }


    /**
     * Replace the content of a named graph.
     *
     * @param ctx the Javalin request context
     */
    public void put(Context ctx) {
        String graphUri = extractGraphUri(ctx);
        if (graphUri == null) {
            ctx.status(400).result(missingGraphParam());
            return;
        }
        String body = ctx.body();
        if (body.isBlank()) {
            ctx.status(204);
            return;
        }
        SparqlResponse response = service.putGraph(graphUri, body, ctx.contentType());
        writeResponse(ctx, response);
    }


    /**
     * Add triples to a named graph.
     * Creates the graph if it does not exist.
     *
     * @param ctx the Javalin request context
     */
    public void post(Context ctx) {
        String graphUri = extractGraphUri(ctx);
        if (graphUri == null) {
            ctx.status(400).result(missingGraphParam());
            return;
        }
        String body = ctx.body();
        if (body.isBlank()) {
            ctx.status(204);
            return;
        }
        SparqlResponse response = service.postGraph(graphUri, body, ctx.contentType());
        writeResponse(ctx, response);
    }

    /**
     * Apply a SPARQL Update to a named graph (partial modification).
     *
     * @param ctx the Javalin request context
     */
    public void patch(Context ctx) {
        String graphUri = extractGraphUri(ctx);
        if (graphUri == null) {
            ctx.status(400).result(missingGraphParam());
            return;
        }

        String ct = ctx.contentType() != null ? ctx.contentType() : "";
        if (!ct.contains("sparql-update")) {
            ctx.status(415).result(
                    "Unsupported Media Type for PATCH. " +
                            "Use Content-Type: application/sparql-update"
            );
            return;
        }

        String body = ctx.body();
        SparqlResponse response = service.patchGraph(graphUri, body);
        writeResponse(ctx, response);
    }

    /**
     * Drop a named graph.
     *
     * @param ctx the Javalin request context
     */
    public void delete(Context ctx) {
        String graphUri = extractGraphUri(ctx);
        if (graphUri == null) {
            ctx.status(400).result(missingGraphParam());
            return;
        }
        SparqlResponse response = service.deleteGraph(graphUri);
        writeResponse(ctx, response);
    }


    /**
     * Extract the graph URI from the request.
     *
     * @param ctx the request context
     * @return graph URI string, or {@code null} if not present
     */
    private String extractGraphUri(Context ctx) {
        String graphParam = ctx.queryParam("graph");
        if (graphParam != null && !graphParam.isBlank()) {
            return graphParam.trim();
        }
        if (ctx.queryParamMap().containsKey("default")) {
            return "urn:x-arq:DefaultGraph";
        }
        return null;
    }

    private String missingGraphParam() {
        return """
                Bad Request: missing graph identifier.
                Use ?graph=<uri> to identify a named graph.
                Use ?default to identify the default graph.
                """;
    }

    private void writeResponse(Context ctx, SparqlResponse response) {
        ctx.status(response.statusCode());
        if (response.contentType() != null) {
            ctx.contentType(response.contentType());
        }
        if (response.hasBody()) {
            ctx.result(response.body());
        }
    }
}
