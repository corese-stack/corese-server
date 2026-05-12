package fr.inria.corese.server.http.handler;

import fr.inria.corese.server.http.model.SparqlRequest;
import fr.inria.corese.server.http.model.SparqlResponse;
import fr.inria.corese.server.service.SparqlExecutionService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * HTTP handler for the SPARQL 1.1 Protocol {@code update} operation.
 * Implements the two transmission
 */
public class SPARQLUpdateHandler {

    private static final Logger log = LoggerFactory.getLogger(SPARQLUpdateHandler.class);

    private static final String CT_FORM_URLENCODED = "application/x-www-form-urlencoded";
    private static final String CT_SPARQL_UPDATE = "application/sparql-update";

    private final SparqlExecutionService service;

    /**
     * Creates a new handler backed by the given execution service.
     *
     * @param service the SPARQL execution service used to run updates
     */
    public SPARQLUpdateHandler(SparqlExecutionService service) {
        this.service = service;
    }

    /**
     * Handle a SPARQL update request.
     *
     * @param ctx the current Javalin request context
     */
    public void handle(Context ctx) {
        String update = extractUpdateString(ctx);

        if (update == null) {
            ctx.status(400).result(
                    """
                            Bad Request: missing required parameter 'update'.
                            Supported forms:
                              Form 1 : POST /sparql  Content-Type: application/x-www-form-urlencoded  Body: update=...
                              Form 2 : POST /sparql  Content-Type: application/sparql-update  Body: INSERT DATA { ... }
                            """
            );
            return;
        }

        List<String> usingGraphUris = ctx.queryParams("using-graph-uri");
        List<String> usingNamedGraphUris = ctx.queryParams("using-named-graph-uri");

        SparqlRequest request = SparqlRequest.update(update, usingGraphUris, usingNamedGraphUris);
        SparqlResponse response = service.executeUpdate(request);

        ctx.status(response.statusCode());
        if (response.contentType() != null) {
            ctx.contentType(response.contentType());
        }
        if (response.hasBody()) {
            ctx.result(response.body());
        }
    }

    private String extractUpdateString(Context ctx) {
        String ct = ctx.contentType();

        // Form 1: POST URL-encoded
        if (ct != null && ct.contains(CT_FORM_URLENCODED)) {
            String update = ctx.formParam("update");
            if (present(update)) {
                log.debug("update via POST URL-encoded");
                return update.trim();
            }
        }

        // Form 2: POST direct SPARQL update body
        if (ct != null && ct.contains(CT_SPARQL_UPDATE)) {
            String update = ctx.body();
            if (present(update)) {
                log.debug("update via POST direct");
                return update.trim();
            }
        }

        return null;
    }

    private boolean present(String s) {
        return s != null && !s.isBlank();
    }
}