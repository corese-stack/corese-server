package fr.inria.corese.server.service;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.sparql.api.ResultFormatDef;
import fr.inria.corese.server.http.model.SparqlResponse;
import fr.inria.corese.server.store.TripleStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the 5 CRUD operations of the SPARQL 1.1 Graph Store HTTP Protocol.
 *
 */
public class GraphStoreService {

    private static final Logger log = LoggerFactory.getLogger(GraphStoreService.class);

    private final TripleStoreManager store;

    /**
     * Creates a new GraphStoreService.
     *
     * @param store the triplestore to operate on
     */
    public GraphStoreService(TripleStoreManager store) {
        this.store = store;
    }


    /**
     * Check if a named graph exists.
     * Corresponds to HEAD /rdf-graph-store?graph=&lt;uri&gt;
     *
     * @param graphUri the URI of the named graph to check
     * @return {@code true} if the graph exists and is non-empty
     */
    public boolean graphExists(String graphUri) {
        store.readLock();
        try {
            Graph graph = store.getNamedGraph(graphUri);
            return graph != null;
        } finally {
            store.readUnlock();
        }
    }


    /**
     * Retrieve the content of a named graph.
     *
     * @param graphUri     the URI of the named graph to retrieve
     * @param acceptHeader the HTTP Accept header for content negotiation
     * @return 200 with serialized graph, 404 if not found, 500 on error
     */
    public SparqlResponse getGraph(String graphUri, String acceptHeader) {
        store.readLock();
        try {
            Graph graph = store.getNamedGraph(graphUri);
            if (graph == null) {
                return SparqlResponse.notFound("Graph not found: " + graphUri);
            }

            String                 contentType = ContentNegotiator.resolveForGraph(acceptHeader);
            ResultFormatDef.format fmt         = ContentNegotiator.toFormat(contentType);
            String                 body        = ResultFormat.create(graph, fmt).toString();

            log.debug("GET graph <{}> — {} triple(s)", graphUri, graph.size());
            return SparqlResponse.ok(contentType, body);

        } catch (Exception e) {
            log.error("getGraph({}) failed: {}", graphUri, e.getMessage(), e);
            return SparqlResponse.serverError("Failed to retrieve graph: " + e.getMessage());
        } finally {
            store.readUnlock();
        }
    }

    /**
     * Replace the content of a named graph.
     * Creates the graph if it does not exist.
     *
     * @param graphUri    the URI of the named graph to replace
     * @param body        the RDF payload as a string
     * @param contentType the media type of the payload
     * @return 204 No Content on success, 400 on parse error, 500 on error
     */
    public SparqlResponse putGraph(String graphUri, String body, String contentType) {
        try {
            Graph newGraph = parseRdf(body, contentType, graphUri);
            store.replaceGraph(graphUri, newGraph);

            log.debug("PUT graph <{}> — {} triple(s)", graphUri, newGraph.size());
            return SparqlResponse.noContent();

        } catch (LoadException e) {
            log.warn("PUT graph <{}> parse error: {}", graphUri, e.getMessage());
            return SparqlResponse.badRequest("RDF parse error: " + e.getMessage());
        } catch (Error e) {
            log.warn("PUT graph <{}> parse Error: {}", graphUri, e.getMessage());
            return SparqlResponse.badRequest("RDF parse error: " + e.getMessage());
        } catch (Exception e) {
            log.error("putGraph({}) failed: {}", graphUri, e.getMessage(), e);
            return SparqlResponse.serverError("Failed to replace graph: " + e.getMessage());
        }
    }


    /**
     * Add triples from the given RDF body to a named graph.
     * Creates the graph if it does not exist.
     *
     * @param graphUri    the URI of the named graph
     * @param body        the RDF payload as a string
     * @param contentType the media type of the payload
     * @return 204 No Content on success, 400 on parse error, 500 on error
     */
    public SparqlResponse postGraph(String graphUri, String body, String contentType) {
        try {
            Graph newGraph = parseRdf(body, contentType, graphUri);
            store.addToGraph(graphUri, newGraph);

            log.debug("POST graph <{}> — added {} triple(s)", graphUri, newGraph.size());
            return SparqlResponse.noContent();

        } catch (LoadException e) {
            log.warn("POST graph <{}> parse error: {}", graphUri, e.getMessage());
            return SparqlResponse.badRequest("RDF parse error: " + e.getMessage());
        } catch (Error e) {
            log.warn("POST graph <{}> parse Error: {}", graphUri, e.getMessage());
            return SparqlResponse.badRequest("RDF parse error: " + e.getMessage());
        } catch (Exception e) {
            log.error("postGraph({}) failed: {}", graphUri, e.getMessage(), e);
            return SparqlResponse.serverError("Failed to add to graph: " + e.getMessage());
        }
    }


    /**
     * Drop the named graph identified by the given URI.
     *
     * @param graphUri the URI of the named graph to delete
     * @return 204 No Content on success, 404 if not found, 500 on error
     */
    public SparqlResponse deleteGraph(String graphUri) {
        try {
            Graph existing = store.getNamedGraph(graphUri);
            if (existing == null) {
                return SparqlResponse.notFound("Graph not found: " + graphUri);
            }
            store.deleteGraph(graphUri);

            log.debug("DELETE graph <{}>", graphUri);
            return SparqlResponse.noContent();

        } catch (Exception e) {
            log.error("deleteGraph({}) failed: {}", graphUri, e.getMessage(), e);
            return SparqlResponse.serverError("Failed to delete graph: " + e.getMessage());
        }
    }


    /**
     * Parse an RDF string payload into a Graph.
     *
     *
     * @param body        the RDF content as a string
     * @param contentType the HTTP Content-Type header value
     * @param baseUri     base URI for relative IRI resolution
     * @return parsed Graph
     * @throws LoadException if the RDF content cannot be parsed
     */
    private Graph parseRdf(String body, String contentType, String baseUri)
            throws LoadException {
        Graph         graph  = Graph.create();
        Load          loader = Load.create(graph);
        Loader.format fmt    = resolveLoadFormat(contentType);
        try {
            loader.loadString(body, baseUri, fmt);
        } catch (Error e) {
            throw new LoadException(new Exception("RDF parse error: " + e.getMessage()));
        }
        return graph;
    }

    /**
     * Map an HTTP Content-Type to a {@link Loader.format} enum value.
     *
     * @param contentType the HTTP Content-Type header value
     * @return the corresponding Loader.format enum value
     */
    private Loader.format resolveLoadFormat(String contentType) {
        if (contentType == null) return Loader.format.TURTLE_FORMAT;
        String ct = contentType.toLowerCase();
        if (ct.contains("turtle"))    return Loader.format.TURTLE_FORMAT;
        if (ct.contains("rdf+xml"))   return Loader.format.RDFXML_FORMAT;
        if (ct.contains("n-triples")) return Loader.format.NT_FORMAT;
        if (ct.contains("n-quads"))   return Loader.format.NQUADS_FORMAT;
        if (ct.contains("trig"))      return Loader.format.TRIG_FORMAT;
        if (ct.contains("ld+json"))   return Loader.format.JSONLD_FORMAT;
        return Loader.format.TURTLE_FORMAT;
    }
}
