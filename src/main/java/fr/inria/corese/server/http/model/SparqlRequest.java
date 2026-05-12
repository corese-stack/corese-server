package fr.inria.corese.server.http.model;

import java.util.List;

/**
 * Immutable representation of a SPARQL Protocol request.
 * <p>Maps the parameters defined in SPARQL 1.1 Protocol (query) and (update).
 *
 * @param operationType       whether this is a QUERY or UPDATE request
 * @param queryString         the SPARQL query string ( parameter {@code query})
 * @param defaultGraphUris    zero or more default graph URIs ( parameter {@code default-graph-uri})
 * @param namedGraphUris      zero or more named graph URIs ( parameter {@code named-graph-uri})
 * @param updateString        the SPARQL update string (parameter {@code update})
 * @param usingGraphUris      zero or more graph URIs for the update dataset
 * @param usingNamedGraphUris zero or more named graph URIs for the update dataset
 * @param acceptHeader        value of the HTTP {@code Accept} header, may be {@code null}
 * @see <a href="https://www.w3.org/TR/sparql11-protocol/#query-operation"> query operation</a>
 * @see <a href="https://www.w3.org/TR/sparql11-protocol/#update-operation">update operation</a>
 */
public record SparqlRequest(
        OperationType operationType,
        String queryString,
        List<String> defaultGraphUris,
        List<String> namedGraphUris,
        String updateString,
        List<String> usingGraphUris,
        List<String> usingNamedGraphUris,
        String acceptHeader
) {

    /**
     * Discriminator between a SPARQL query operation and an update operation.
     */
    public enum OperationType {
        /**
         * SPARQL 1.1 query operation — SELECT, ASK, CONSTRUCT, DESCRIBE.
         */
        QUERY,
        /**
         * SPARQL 1.1 update operation — INSERT, DELETE, LOAD, CLEAR, DROP…
         */
        UPDATE
    }

    // Factory methods

    /**
     * Build a query request (SELECT, ASK, CONSTRUCT, DESCRIBE).
     *
     * @param queryString      the SPARQL query string
     * @param defaultGraphUris default graph URIs, may be {@code null}
     * @param namedGraphUris   named graph URIs, may be {@code null}
     * @param acceptHeader     HTTP Accept header value, may be {@code null}
     * @return a new {@link SparqlRequest} of type QUERY
     */
    public static SparqlRequest query(String queryString,
                                      List<String> defaultGraphUris,
                                      List<String> namedGraphUris,
                                      String acceptHeader) {
        return new SparqlRequest(
                OperationType.QUERY,
                queryString,
                safe(defaultGraphUris),
                safe(namedGraphUris),
                null, List.of(), List.of(),
                acceptHeader
        );
    }

    /**
     * Build an update request (INSERT DATA, DELETE DATA, LOAD, CLEAR…).
     *
     * @param updateString        the SPARQL update string
     * @param usingGraphUris      graph URIs for the update dataset, may be {@code null}
     * @param usingNamedGraphUris named graph URIs for the update dataset, may be {@code null}
     * @return a new {@link SparqlRequest} of type UPDATE
     */
    public static SparqlRequest update(String updateString,
                                       List<String> usingGraphUris,
                                       List<String> usingNamedGraphUris) {
        return new SparqlRequest(
                OperationType.UPDATE,
                null, List.of(), List.of(),
                updateString,
                safe(usingGraphUris),
                safe(usingNamedGraphUris),
                null
        );
    }


    /**
     * Checks if this is a SPARQL query operation.
     *
     * @return {@code true} for QUERY type
     */
    public boolean isQuery() {
        return operationType == OperationType.QUERY;
    }

    /**
     * Checks if this is a SPARQL update operation.
     *
     * @return {@code true} for UPDATE type
     */
    public boolean isUpdate() {
        return operationType == OperationType.UPDATE;
    }

    /**
     * Checks if a non-blank query string is present.
     *
     * @return {@code true} if the query string is non-null and non-blank
     */
    public boolean hasQuery() {
        return queryString != null && !queryString.isBlank();
    }

    /**
     * Checks if a non-blank update string is present.
     *
     * @return {@code true} if the update string is non-null and non-blank
     */
    public boolean hasUpdate() {
        return updateString != null && !updateString.isBlank();
    }

    /**
     * Checks if any dataset parameters are present.
     *
     * @return {@code true} if default or named graph URIs are non-empty
     */
    public boolean hasDataset() {
        return !defaultGraphUris.isEmpty() || !namedGraphUris.isEmpty();
    }

    private static List<String> safe(List<String> list) {
        return list != null ? list : List.of();
    }
}