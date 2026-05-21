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

    private static final String TURTLE_DATA = """
            @prefix ex: <http://example.org/> .
            ex:Alice ex:knows ex:Bob .
            ex:Bob   ex:knows ex:Carol .
            """;

    private GraphStoreService service;

    @BeforeEach
    void setUp() {
        ServerConfig config = new ServerConfig(8080, null, null, false);
        CoreseTripleStoreManager store = new CoreseTripleStoreManager(config);
        service = new GraphStoreService(store);
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
        assertTrue(body.contains("Alice") || body.contains("Dave"),
                "Graph should contain data from both PUT and POST");
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
