package fr.inria.corese.server.service;

import fr.inria.corese.server.config.ServerConfig;
import fr.inria.corese.server.http.model.SparqlResponse;
import fr.inria.corese.server.store.CoreseTripleStoreManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link GraphStoreService}.
 * SPARQL 1.1 Graph Store HTTP Protocol compliance.
 */
class GraphStoreServiceTest {

    private static final String GRAPH_URI = "http://example.org/test-graph";
    private static final String GRAPH_URI2 = "http://example.org/test-graph-2";

    private static final String TURTLE_DATA = """
            @prefix ex: <http://example.org/> .
            ex:Alice ex:knows ex:Bob .
            ex:Bob   ex:knows ex:Carol .
            """;

    private GraphStoreService service;

    @BeforeEach
    void setUp() {
        ServerConfig config = new ServerConfig(8080, null, null, false, 0);
        CoreseTripleStoreManager store = new CoreseTripleStoreManager(config);
        service = new GraphStoreService(store);
    }


    @Test
    @DisplayName("LIST empty store -> 200 empty Turtle")
    void list_emptyStore_returns200() {
        SparqlResponse res = service.listGraphs();
        assertEquals(200, res.statusCode());
        assertTrue(res.hasBody());
        assertEquals("text/turtle", res.contentType());
    }

    @Test
    @DisplayName("LIST after PUT -> contains graph URI")
    void list_afterPut_containsGraphUri() {
        service.putGraph(GRAPH_URI, TURTLE_DATA, "text/turtle");
        SparqlResponse res = service.listGraphs();
        assertEquals(200, res.statusCode());
        assertTrue(res.bodyAsString().contains(GRAPH_URI));
    }

    @Test
    @DisplayName("LIST multiple graphs -> contains all URIs")
    void list_multipleGraphs_containsAll() {
        service.putGraph(GRAPH_URI, TURTLE_DATA, "text/turtle");
        service.putGraph(GRAPH_URI2, TURTLE_DATA, "text/turtle");
        SparqlResponse res = service.listGraphs();
        assertEquals(200, res.statusCode());
        assertTrue(res.bodyAsString().contains(GRAPH_URI));
        assertTrue(res.bodyAsString().contains(GRAPH_URI2));
    }

    @Test
    @DisplayName("LIST after DELETE -> graph URI removed")
    void list_afterDelete_graphRemoved() {
        service.putGraph(GRAPH_URI, TURTLE_DATA, "text/turtle");
        service.deleteGraph(GRAPH_URI);
        SparqlResponse res = service.listGraphs();
        assertEquals(200, res.statusCode());
        assertFalse(res.bodyAsString().contains(GRAPH_URI));
    }


    @Test
    @DisplayName("HEAD non-existent graph -> false")
    void head_unknownGraph_returnsFalse() {
        assertFalse(service.graphExists(GRAPH_URI));
    }

    @Test
    @DisplayName("HEAD after PUT -> true")
    void head_afterPut_returnsTrue() {
        service.putGraph(GRAPH_URI, TURTLE_DATA, "text/turtle");
        assertTrue(service.graphExists(GRAPH_URI));
    }

    @Test
    @DisplayName("HEAD after DELETE -> false")
    void head_afterDelete_returnsFalse() {
        service.putGraph(GRAPH_URI, TURTLE_DATA, "text/turtle");
        service.deleteGraph(GRAPH_URI);
        assertFalse(service.graphExists(GRAPH_URI));
    }


    @Test
    @DisplayName("GET non-existent graph -> 404")
    void get_unknownGraph_returns404() {
        SparqlResponse res = service.getGraph(GRAPH_URI, null);
        assertEquals(404, res.statusCode());
    }

    @Test
    @DisplayName(" GET after PUT -> 200 with body")
    void get_afterPut_returns200() {
        service.putGraph(GRAPH_URI, TURTLE_DATA, "text/turtle");
        SparqlResponse res = service.getGraph(GRAPH_URI, "text/turtle");
        assertEquals(200, res.statusCode());
        assertTrue(res.hasBody());
    }


    @Test
    @DisplayName("PUT valid Turtle -> 204")
    void put_validTurtle_returns204() {
        SparqlResponse res = service.putGraph(GRAPH_URI, TURTLE_DATA, "text/turtle");
        assertEquals(204, res.statusCode());
    }

    @Test
    @DisplayName("PUT replaces existing graph")
    void put_replacesExistingGraph() {
        service.putGraph(GRAPH_URI, TURTLE_DATA, "text/turtle");
        String newData = """
                @prefix ex: <http://example.org/> .
                ex:Dave ex:knows ex:Eve .
                """;
        service.putGraph(GRAPH_URI, newData, "text/turtle");
        SparqlResponse res = service.getGraph(GRAPH_URI, "text/turtle");
        assertEquals(200, res.statusCode());
        assertFalse(res.bodyAsString().contains("Alice"), "Old data should be replaced");
    }

    @Test
    @DisplayName("PUT invalid RDF -> 400")
    void put_invalidRdf_returns400() {
        SparqlResponse res = service.putGraph(GRAPH_URI, "THIS IS NOT RDF !!!", "text/turtle");
        assertEquals(400, res.statusCode());
    }


    @Test
    @DisplayName("POST valid Turtle -> 204")
    void post_validTurtle_returns204() {
        SparqlResponse res = service.postGraph(GRAPH_URI, TURTLE_DATA, "text/turtle");
        assertEquals(204, res.statusCode());
    }

    @Test
    @DisplayName("POST invalid RDF -> 400")
    void post_invalidRdf_returns400() {
        SparqlResponse res = service.postGraph(GRAPH_URI, "INVALID RDF !!!", "text/turtle");
        assertEquals(400, res.statusCode());
    }

    @Test
    @DisplayName("POST adds triples to existing graph")
    void post_addsTriplesToExistingGraph() {
        service.putGraph(GRAPH_URI, TURTLE_DATA, "text/turtle");
        String moreData = """
                @prefix ex: <http://example.org/> .
                ex:Dave ex:knows ex:Eve .
                """;
        service.postGraph(GRAPH_URI, moreData, "text/turtle");
        SparqlResponse res = service.getGraph(GRAPH_URI, "text/turtle");
        assertEquals(200, res.statusCode());
        String body = res.bodyAsString();
        assertTrue(body.contains("Alice") || body.contains("Dave"));
    }


    @Test
    @DisplayName("PATCH non-existent graph -> 404")
    void patch_unknownGraph_returns404() {
        SparqlResponse res = service.patchGraph(GRAPH_URI,
                "INSERT DATA { <http://ex.org/s> <http://ex.org/p> <http://ex.org/o> }");
        assertEquals(404, res.statusCode());
    }


    @Test
    @DisplayName("PATCH empty body -> 400")
    void patch_emptyBody_returns400() {
        service.putGraph(GRAPH_URI, TURTLE_DATA, "text/turtle");
        SparqlResponse res = service.patchGraph(GRAPH_URI, "");
        assertEquals(400, res.statusCode());
    }

    @Test
    @DisplayName("PATCH malformed SPARQL -> 400")
    void patch_malformedSparql_returns400() {
        service.putGraph(GRAPH_URI, TURTLE_DATA, "text/turtle");
        SparqlResponse res = service.patchGraph(GRAPH_URI, "NOT VALID SPARQL UPDATE");
        assertEquals(400, res.statusCode());
    }


    @Test
    @DisplayName("DELETE existing graph -> 204")
    void delete_existingGraph_returns204() {
        service.putGraph(GRAPH_URI, TURTLE_DATA, "text/turtle");
        SparqlResponse res = service.deleteGraph(GRAPH_URI);
        assertEquals(204, res.statusCode());
    }

    @Test
    @DisplayName("DELETE non-existent graph -> 404")
    void delete_unknownGraph_returns404() {
        SparqlResponse res = service.deleteGraph("http://example.org/unknown");
        assertEquals(404, res.statusCode());
    }

    @Test
    @DisplayName("DELETE then GET -> 404")
    void delete_thenGet_returns404() {
        service.putGraph(GRAPH_URI, TURTLE_DATA, "text/turtle");
        service.deleteGraph(GRAPH_URI);
        SparqlResponse res = service.getGraph(GRAPH_URI, null);
        assertEquals(404, res.statusCode());
    }
}
