package fr.inria.corese.server.http.handler;

import fr.inria.corese.server.http.model.SparqlRequest;
import fr.inria.corese.server.http.model.SparqlResponse;
import fr.inria.corese.server.service.SparqlExecutionService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * HTTP handler for the SPARQL 1.1 Protocol {@code query} operation.
 * Implements the three transmission forms
 */
public class SPARQLQueryHandler {

    private static final Logger log = LoggerFactory.getLogger(SPARQLQueryHandler.class);

    /**
     * POST with URL-encoded body
     */
    private static final String CT_FORM_URLENCODED = "application/x-www-form-urlencoded";

    /**
     * POST with SPARQL query directly as body
     */
    private static final String CT_SPARQL_QUERY = "application/sparql-query";

    private final SparqlExecutionService service;

    /**
     * Creates a new SPARQLQueryHandler.
     *
     * @param service the SPARQL execution service to delegate query execution to
     */
    public SPARQLQueryHandler(SparqlExecutionService service) {
        this.service = service;
    }


    /**
     * Handle any of the three SPARQL query transmission.
     *
     * @param ctx the current Javalin request context
     */
    public void handle(Context ctx) {

        // Extract query string (try all 3 forms in spec order)
        String query = extractQueryString(ctx);

        if (query == null) {
            // "requests MUST include exactly one SPARQL query string"
            ctx.status(400).result(
                    """
                            Bad Request: missing required parameter 'query'.
                            Supported forms:
                              Form 1 : GET  /sparql?query=<encoded>
                              Form 2 : POST /sparql  Content-Type: application/x-www-form-urlencoded
                              Form 3 : POST /sparql  Content-Type: application/sparql-query
                            """
            );
            return;
        }

        // Extract dataset parameters
        // For Form 3 these come from URL query string even on a POST request
        List<String> defaultGraphUris = ctx.queryParams("default-graph-uri");
        List<String> namedGraphUris = ctx.queryParams("named-graph-uri");

        //Content negotiation
        String acceptHeader = ctx.header("Accept");

        // Build protocol request and delegate to service
        SparqlRequest request = SparqlRequest.query(query, defaultGraphUris, namedGraphUris, acceptHeader);
        SparqlResponse response = service.executeQuery(request);

        // Write HTTP response
        writeResponse(ctx, response);
    }


    /**
     * Extract the SPARQL query string from the request, trying all three forms.
     * Returns null if none is found.
     */
    private String extractQueryString(Context ctx) {

        // Form 1: GET (or POST with ?query= in URL)
        String query = ctx.queryParam("query");
        if (present(query)) {
            log.debug("query via GET / URL param");
            return query.trim();
        }

        String ct = ctx.contentType();

        //Form 2: POST form-urlencoded
        if (ct != null && ct.contains(CT_FORM_URLENCODED)) {
            query = ctx.formParam("query");
            if (present(query)) {
                log.debug("query via POST URL-encoded");
                return query.trim();
            }
        }

        //Form 3: POST with direct SPARQL body
        if (ct != null && ct.contains(CT_SPARQL_QUERY)) {
            query = ctx.body();
            if (present(query)) {
                log.debug("query via POST direct");
                return query.trim();
            }
        }

        return null;
    }

    private boolean present(String s) {
        return s != null && !s.isBlank();
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
