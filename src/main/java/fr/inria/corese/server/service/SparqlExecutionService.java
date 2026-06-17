package fr.inria.corese.server.service;

import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.api.ResultFormatDef;
import fr.inria.corese.server.http.model.SparqlRequest;
import fr.inria.corese.server.http.model.SparqlResponse;
import fr.inria.corese.server.store.TripleStoreManager;
import org.jetbrains.annotations.NotNull;
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
            String queryStr = applyQueryDataset(request);
            Mappings mappings = exec.query(queryStr);

            String contentType = resolveContentType(request, mappings);
            ResultFormatDef.format fmt = ContentNegotiator.toFormat(contentType);
            ResultFormat rf = ResultFormat.create(mappings, fmt);
            String body = rf.toString();

            log.debug("Query OK — {} result(s), format={}", mappings.size(), contentType);
            return SparqlResponse.ok(contentType, body);

        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            boolean syntax = isSyntaxError(msg);
            if (syntax) {
                log.debug("SPARQL syntax error (400): {}", msg);
            } else {
                log.warn("Query error (500): {}", msg);
            }
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

        String updateStr;
        try {
            updateStr = applyUpdateDataset(request);
        } catch (IllegalArgumentException e) {
            return SparqlResponse.badRequest(e.getMessage());
        }

        store.writeLock();
        try {
            store.newQueryProcess().query(updateStr);
            log.debug("Update OK");
            return SparqlResponse.noContent();

        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            boolean syntax = isSyntaxError(msg);
            if (syntax) {
                log.debug("SPARQL update syntax error (400): {}", msg);
            } else {
                log.warn("Update error (500): {}", msg);
            }
            return syntax
                    ? SparqlResponse.badRequest("SPARQL update syntax error: %s".formatted(msg))
                    : SparqlResponse.serverError("SPARQL update error: %s".formatted(msg));
        } finally {
            store.writeUnlock();
        }
    }


    /**
     * insert FROM / FROM NAMED clauses right before the WHERE
     * keyword (valid SPARQL position), or right after the query form
     * keyword if no WHERE is present (e.g. DESCRIBE without WHERE).
     *
     */
    private String applyQueryDataset(SparqlRequest request) {
        if (!request.hasDataset()) return request.queryString();

        StringBuilder clauses = new StringBuilder();
        for (String uri : request.defaultGraphUris())
            clauses.append("FROM <").append(uri).append("> ");
        for (String uri : request.namedGraphUris())
            clauses.append("FROM NAMED <").append(uri).append("> ");

        String query = request.queryString().trim();
        String upper = query.toUpperCase();

        int wherePos = indexOfKeyword(upper);
        if (wherePos != -1) {
            return query.substring(0, wherePos) + clauses + query.substring(wherePos);
        }
        return query + " " + clauses;
    }

    /**
     * insert USING / USING NAMED clauses right before the WHERE
     * keyword of a DELETE/INSERT...WHERE operation.
     *
     *
     * @throws IllegalArgumentException if the request already contains
     *         USING, USING NAMED, or WITH (§2.2.3 conflict)
     */
    private String applyUpdateDataset(SparqlRequest request) {
        boolean hasUsingParams = !request.usingGraphUris().isEmpty()
                || !request.usingNamedGraphUris().isEmpty();

        if (!hasUsingParams) {
            return request.updateString();
        }

        String update = request.updateString().trim();
        String upper = getString(update);

        int wherePos = indexOfKeyword(upper);
        if (wherePos == -1) {
            return update;
        }

        StringBuilder clauses = new StringBuilder();
        for (String uri : request.usingGraphUris())
            clauses.append("USING <").append(uri).append("> ");
        for (String uri : request.usingNamedGraphUris())
            clauses.append("USING NAMED <").append(uri).append("> ");

        return update.substring(0, wherePos) + clauses + update.substring(wherePos);
    }

    @NotNull
    private String getString(String update) {
        String upper  = update.toUpperCase();

        if (containsUsingClause(upper) || containsWithClause(upper)) {
            throw new IllegalArgumentException(
                    "Bad Request: using-graph-uri/using-named-graph-uri protocol parameters " +
                            "cannot be combined with an update request that already contains a " +
                            "USING, USING NAMED, or WITH clause. " +
                            "See https://www.w3.org/TR/sparql11-protocol/#update-operation"
            );
        }
        return upper;
    }

    private boolean containsUsingClause(String upperUpdate) {
        return upperUpdate.matches("(?s).*\\bUSING\\b.*");
    }

    private boolean containsWithClause(String upperUpdate) {
        return upperUpdate.matches("(?s).*\\bWITH\\s+<.*");
    }

    /**
     * Find the index of a SPARQL keyword as a whole word (not a substring
     * of another identifier), case-insensitively, in the uppercased input.
     *
     * @param upper the query/update string already uppercased
     * @return index of the keyword, or -1 if not found
     */
    private int indexOfKeyword(String upper) {
        var matcher = java.util.regex.Pattern
                .compile("\\b" + "WHERE" + "\\b")
                .matcher(upper);
        return matcher.find() ? matcher.start() : -1;
    }

    private boolean isSyntaxError(String msg) {
        String lower = msg.toLowerCase();
        return lower.contains("parse")
                || lower.contains("syntax")
                || lower.contains("unexpected")
                || lower.contains("encountered");
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