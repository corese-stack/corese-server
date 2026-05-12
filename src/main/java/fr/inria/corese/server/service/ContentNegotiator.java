package fr.inria.corese.server.service;

import fr.inria.corese.core.sparql.api.ResultFormatDef;

/**
 * Maps HTTP Accept headers to corese-core ResultFormatDef.format enum values.
 * Source confirmed from ResultFormat.java
 */
public final class ContentNegotiator {

    private ContentNegotiator() {
    }

    /**
     * SPARQL result set — XML serialization (default for SELECT/ASK).
     */
    public static final String SPARQL_RESULTS_XML = "application/sparql-results+xml";
    /**
     * SPARQL result set — JSON serialization.
     */
    public static final String SPARQL_RESULTS_JSON = "application/sparql-results+json";
    /**
     * SPARQL result set — CSV serialization.
     */
    public static final String SPARQL_RESULTS_CSV = "text/csv";
    /**
     * SPARQL result set — TSV serialization.
     */
    public static final String SPARQL_RESULTS_TSV = "text/tab-separated-values";
    /**
     * RDF graph — Turtle serialization (default for CONSTRUCT/DESCRIBE).
     */
    public static final String TURTLE = "text/turtle";
    /**
     * RDF graph — RDF/XML serialization.
     */
    public static final String RDF_XML = "application/rdf+xml";
    /**
     * RDF graph — N-Triples serialization.
     */
    public static final String N_TRIPLES = "application/n-triples";
    /**
     * RDF graph — JSON-LD serialization.
     */
    public static final String JSON_LD = "application/ld+json";
    /**
     * RDF graph — TriG serialization.
     */
    public static final String TRIG = "application/trig";

    //Content negotiation

    /**
     * Resolve response content-type for SELECT / ASK queries.
     *
     * @param acceptHeader value of the HTTP {@code Accept} header, may be {@code null}
     * @return MIME type string — defaults to {@link #SPARQL_RESULTS_XML}
     */
    public static String resolveForResultSet(String acceptHeader) {
        if (acceptHeader == null || acceptHeader.isBlank()) return SPARQL_RESULTS_XML;
        String a = acceptHeader.toLowerCase();
        if (a.contains("sparql-results+json") || a.contains("application/json"))
            return SPARQL_RESULTS_JSON;
        if (a.contains("sparql-results+xml")) return SPARQL_RESULTS_XML;
        if (a.contains("text/csv")) return SPARQL_RESULTS_CSV;
        if (a.contains("tab-separated")) return SPARQL_RESULTS_TSV;
        return SPARQL_RESULTS_XML;
    }

    /**
     * Resolve response content-type for CONSTRUCT / DESCRIBE queries.
     *
     * @param acceptHeader value of the HTTP {@code Accept} header, may be {@code null}
     * @return MIME type string — defaults to {@link #TURTLE}
     */
    public static String resolveForGraph(String acceptHeader) {
        if (acceptHeader == null || acceptHeader.isBlank()) return TURTLE;
        String a = acceptHeader.toLowerCase();
        if (a.contains("text/turtle")) return TURTLE;
        if (a.contains("application/rdf+xml")) return RDF_XML;
        if (a.contains("n-triples")) return N_TRIPLES;
        if (a.contains("ld+json")) return JSON_LD;
        if (a.contains("application/trig")) return TRIG;
        return TURTLE;
    }

    /**
     * Map a content-type string to a {@link ResultFormatDef.format} enum value
     * for use with {@code ResultFormat.create(Mappings, format)}.
     *
     * @param contentType MIME type string (use constants from this class)
     * @return the matching corese-core format enum — defaults to {@code XML_FORMAT}
     */
    public static ResultFormatDef.format toFormat(String contentType) {
        return switch (contentType) {
            case SPARQL_RESULTS_JSON -> ResultFormatDef.format.JSON_FORMAT;
            case SPARQL_RESULTS_CSV -> ResultFormatDef.format.CSV_FORMAT;
            case SPARQL_RESULTS_TSV -> ResultFormatDef.format.TSV_FORMAT;
            case TURTLE -> ResultFormatDef.format.TURTLE_FORMAT;
            case RDF_XML -> ResultFormatDef.format.RDF_XML_FORMAT;
            case N_TRIPLES -> ResultFormatDef.format.NTRIPLES_FORMAT;
            case JSON_LD -> ResultFormatDef.format.JSONLD_FORMAT;
            case TRIG -> ResultFormatDef.format.TRIG_FORMAT;
            default -> ResultFormatDef.format.XML_FORMAT;
        };
    }
}
