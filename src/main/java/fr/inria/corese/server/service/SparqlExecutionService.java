package fr.inria.corese.server.service;

import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.api.ResultFormatDef;
import fr.inria.corese.server.http.model.SparqlRequest;
import fr.inria.corese.server.http.model.SparqlResponse;
import fr.inria.corese.server.store.TripleStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes SPARQL queries and updates.
 */
public class SparqlExecutionService {

    private static final Logger log = LoggerFactory.getLogger(SparqlExecutionService.class);

    private final TripleStoreManager store;

    /**
     * Creates a new SparqlExecutionService.
     *
     * @param store the triplestore to execute queries against
     */
    public SparqlExecutionService(TripleStoreManager store) {
        this.store = store;
    }

    // Query

    /**
     * Execute a SPARQL query (SELECT, ASK, CONSTRUCT, DESCRIBE).
     * Returns 200 on success, 400 for malformed SPARQL, 500 for execution failure.
     *
     * @param request the SPARQL protocol request
     * @return the HTTP response to send to the client
     */
    public SparqlResponse executeQuery(SparqlRequest request) {
        if (!request.hasQuery()) {
            return SparqlResponse.badRequest(
                    "Missing required parameter 'query'. " +
                            "See https://www.w3.org/TR/sparql11-protocol/#query-operation"
            );
        }

        store.readLock();
        try {
            QueryProcess exec = store.newQueryProcess();
            String queryStr = applyDataset(request);
            Mappings mappings = exec.query(queryStr);

            // Resolve content-type string first
            String contentType = resolveContentType(request, mappings);
            // Map to corese-core enum — confirmed from ResultFormat.initFormat()
            ResultFormatDef.format fmt = ContentNegotiator.toFormat(contentType);

            // ResultFormat.create(Mappings, format) — confirmed in decompiled source
            ResultFormat rf = ResultFormat.create(mappings, fmt);
            String body = rf.toString();

            log.debug("Query OK — {} result(s), format={}", mappings.size(), contentType);
            return SparqlResponse.ok(contentType, body);

        } catch (Exception e) {
            log.warn("Query error: {}", e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            boolean syntax = msg.toLowerCase().contains("parse")
                    || msg.toLowerCase().contains("syntax")
                    || msg.toLowerCase().contains("unexpected");
            return syntax
                    ? SparqlResponse.badRequest("SPARQL syntax error: %s".formatted(msg))
                    : SparqlResponse.serverError("SPARQL execution error: %s".formatted(msg));
        } finally {
            store.readUnlock();
        }
    }

    //  Update

    /**
     * Execute a SPARQL update (INSERT, DELETE, LOAD, CLEAR, DROP…).
     * Returns 204 on success, 400 for malformed update, 500 for execution failure.
     *
     * @param request the SPARQL protocol request
     * @return the HTTP response to send to the client
     */
    public SparqlResponse executeUpdate(SparqlRequest request) {
        if (!request.hasUpdate()) {
            return SparqlResponse.badRequest(
                    "Missing required parameter 'update'. " +
                            "See https://www.w3.org/TR/sparql11-protocol/#update-operation"
            );
        }

        store.writeLock();
        try {
            store.newQueryProcess().query(request.updateString());
            log.debug("Update OK");
            return SparqlResponse.noContent();

        } catch (Exception e) {
            log.warn("Update error: {}", e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            boolean syntax = msg.toLowerCase().contains("parse")
                    || msg.toLowerCase().contains("syntax");
            return syntax
                    ? SparqlResponse.badRequest("SPARQL update syntax error: %s".formatted(msg))
                    : SparqlResponse.serverError("SPARQL update error: %s".formatted(msg));
        } finally {
            store.writeUnlock();
        }
    }

    // Helpers

    /**
     * prepend FROM / FROM NAMED clauses if dataset params are present.
     */
    private String applyDataset(SparqlRequest request) {
        if (!request.hasDataset()) return request.queryString();
        StringBuilder sb = new StringBuilder();
        for (String uri : request.defaultGraphUris())
            sb.append("FROM <").append(uri).append(">\n");
        for (String uri : request.namedGraphUris())
            sb.append("FROM NAMED <").append(uri).append(">\n");
        String query = request.queryString().trim();
        int pos = findQueryKeyword(query);
        return "%s\n%s%s".formatted(query.substring(0, pos), sb, query.substring(pos));
    }

    private int findQueryKeyword(String query) {
        String upper = query.toUpperCase();
        for (String kw : new String[]{"SELECT", "ASK", "CONSTRUCT", "DESCRIBE"}) {
            int k = upper.indexOf(kw);
            if (k != -1) return k;
        }
        return 0;
    }

    /**
     * CONSTRUCT/DESCRIBE → graph format; SELECT/ASK → result set format.
     */
    private String resolveContentType(SparqlRequest request, Mappings mappings) {
        if (mappings.getGraph() != null)
            return ContentNegotiator.resolveForGraph(request.acceptHeader());
        return ContentNegotiator.resolveForResultSet(request.acceptHeader());
    }
}
