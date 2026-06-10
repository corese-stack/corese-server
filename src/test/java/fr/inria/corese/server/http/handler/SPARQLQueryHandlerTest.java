package fr.inria.corese.server.http.handler;

import fr.inria.corese.server.config.ServerConfig;
import fr.inria.corese.server.http.model.SparqlRequest;
import fr.inria.corese.server.http.model.SparqlResponse;
import fr.inria.corese.server.service.SparqlExecutionService;
import fr.inria.corese.server.store.CoreseTripleStoreManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SPARQLQueryHandler}.
 * Uses a real SparqlExecutionService with an in-memory corese-core store.
 */
class SPARQLQueryHandlerTest {

    private SparqlExecutionService service;

    @BeforeEach
    void setUp() {
        ServerConfig config = new ServerConfig(8080, null, null, false, 0);
        CoreseTripleStoreManager store = new CoreseTripleStoreManager(config);
        service = new SparqlExecutionService(store);
    }


    @Test
    @DisplayName("GET SELECT on empty store -> 200 XML")
    void form1_get_select_returns200() {
        SparqlRequest req = SparqlRequest.query(
                "SELECT * WHERE { ?s ?p ?o }",
                null, null, "application/sparql-results+xml"
        );
        SparqlResponse res = service.executeQuery(req);
        assertEquals(200, res.statusCode());
        assertTrue(res.hasBody());
        assertTrue(res.bodyAsString().contains("sparql"));
    }

    @Test
    @DisplayName("Empty query string -> 400")
    void emptyQuery_returns400() {
        SparqlRequest req = SparqlRequest.query("", null, null, null);
        SparqlResponse res = service.executeQuery(req);
        assertEquals(400, res.statusCode());
    }

    @Test
    @DisplayName("Blank query string -> 400")
    void blankQuery_returns400() {
        SparqlRequest req = SparqlRequest.query("   ", null, null, null);
        SparqlResponse res = service.executeQuery(req);
        assertEquals(400, res.statusCode());
    }

    @Test
    @DisplayName("Malformed SPARQL -> 400")
    void malformedQuery_returns400() {
        SparqlRequest req = SparqlRequest.query("NOT VALID SPARQL", null, null, null);
        SparqlResponse res = service.executeQuery(req);
        assertEquals(400, res.statusCode());
    }

    @Test
    @DisplayName("Accept JSON -> JSON response")
    void acceptJson_returnsJson() {
        SparqlRequest req = SparqlRequest.query(
                "SELECT * WHERE { ?s ?p ?o }",
                null, null, "application/sparql-results+json"
        );
        SparqlResponse res = service.executeQuery(req);
        assertEquals(200, res.statusCode());
        assertEquals("application/sparql-results+json", res.contentType());
    }

    @Test
    @DisplayName("Accept CSV -> CSV response")
    void acceptCsv_returnsCsv() {
        SparqlRequest req = SparqlRequest.query(
                "SELECT * WHERE { ?s ?p ?o }",
                null, null, "text/csv"
        );
        SparqlResponse res = service.executeQuery(req);
        assertEquals(200, res.statusCode());
        assertEquals("text/csv", res.contentType());
    }

    @Test
    @DisplayName("Accept TSV -> TSV response")
    void acceptTsv_returnsTsv() {
        SparqlRequest req = SparqlRequest.query(
                "SELECT * WHERE { ?s ?p ?o }",
                null, null, "text/tab-separated-values"
        );
        SparqlResponse res = service.executeQuery(req);
        assertEquals(200, res.statusCode());
        assertEquals("text/tab-separated-values", res.contentType());
    }

    @Test
    @DisplayName("ASK query -> 200")
    void askQuery_returns200() {
        SparqlRequest req = SparqlRequest.query(
                "ASK { ?s ?p ?o }", null, null, null
        );
        SparqlResponse res = service.executeQuery(req);
        assertEquals(200, res.statusCode());
    }

    @Test
    @DisplayName("CONSTRUCT -> 200 Turtle")
    void constructQuery_returns200() {
        SparqlRequest req = SparqlRequest.query(
                "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }",
                null, null, "text/turtle"
        );
        SparqlResponse res = service.executeQuery(req);
        assertEquals(200, res.statusCode());
    }

    @Test
    @DisplayName("INSERT then SELECT -> data persists")
    void insertThenSelect_dataVisible() {
        SparqlRequest insert = SparqlRequest.update(
                "INSERT DATA { <http://ex.org/s> <http://ex.org/p> \"hello\" }",
                null, null
        );
        service.executeUpdate(insert);

        SparqlRequest select = SparqlRequest.query(
                "SELECT * WHERE { <http://ex.org/s> ?p ?o }",
                null, null, "application/sparql-results+xml"
        );
        SparqlResponse res = service.executeQuery(select);
        assertEquals(200, res.statusCode());
        assertTrue(res.bodyAsString().contains("hello"));
    }
}