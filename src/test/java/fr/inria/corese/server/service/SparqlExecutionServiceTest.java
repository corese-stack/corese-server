package fr.inria.corese.server.service;

import fr.inria.corese.server.config.ServerConfig;
import fr.inria.corese.server.http.model.SparqlRequest;
import fr.inria.corese.server.http.model.SparqlResponse;
import fr.inria.corese.server.store.CoreseTripleStoreManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link SparqlExecutionService}.
 * Uses a real in-memory corese-core store.
 * Validates SPARQL 1.1 Protocol  / compliance.
 */
class SparqlExecutionServiceTest {

    private SparqlExecutionService service;

    @BeforeEach
    void setUp() {
        ServerConfig config = new ServerConfig(8080, null, null, false, 0);
        CoreseTripleStoreManager store = new CoreseTripleStoreManager(config);
        service = new SparqlExecutionService(store);
    }

    @Test
    @DisplayName("SELECT on empty store -> 200 XML")
    void select_emptyStore_returns200XML() {
        SparqlRequest req = SparqlRequest.query(
                "SELECT * WHERE { ?s ?p ?o }",
                null, null, ContentNegotiator.SPARQL_RESULTS_XML
        );
        SparqlResponse res = service.executeQuery(req);

        assertEquals(200, res.statusCode());
        assertTrue(res.hasBody());
        assertTrue(res.contentType().contains("xml"));
        assertTrue(res.bodyAsString().contains("sparql"));
    }

    @Test
    @DisplayName("Accept JSON -> 200 JSON")
    void select_acceptJson_returns200JSON() {
        SparqlRequest req = SparqlRequest.query(
                "SELECT * WHERE { ?s ?p ?o }",
                null, null, ContentNegotiator.SPARQL_RESULTS_JSON
        );
        SparqlResponse res = service.executeQuery(req);

        assertEquals(200, res.statusCode());
        assertEquals(ContentNegotiator.SPARQL_RESULTS_JSON, res.contentType());
    }

    @Test
    @DisplayName("Accept CSV -> 200 CSV")
    void select_acceptCsv_returns200CSV() {
        SparqlRequest req = SparqlRequest.query(
                "SELECT * WHERE { ?s ?p ?o }",
                null, null, ContentNegotiator.SPARQL_RESULTS_CSV
        );
        SparqlResponse res = service.executeQuery(req);

        assertEquals(200, res.statusCode());
        assertEquals(ContentNegotiator.SPARQL_RESULTS_CSV, res.contentType());
    }

    @Test
    @DisplayName("Accept TSV -> 200 TSV")
    void select_acceptTsv_returns200TSV() {
        SparqlRequest req = SparqlRequest.query(
                "SELECT * WHERE { ?s ?p ?o }",
                null, null, ContentNegotiator.SPARQL_RESULTS_TSV
        );
        SparqlResponse res = service.executeQuery(req);

        assertEquals(200, res.statusCode());
        assertEquals(ContentNegotiator.SPARQL_RESULTS_TSV, res.contentType());
    }

    @Test
    @DisplayName("ASK -> 200")
    void ask_emptyStore_returns200() {
        SparqlRequest req = SparqlRequest.query(
                "ASK { ?s ?p ?o }",
                null, null, ContentNegotiator.SPARQL_RESULTS_XML
        );
        SparqlResponse res = service.executeQuery(req);

        assertEquals(200, res.statusCode());
    }

    @Test
    @DisplayName("CONSTRUCT -> 200 Turtle")
    void construct_returns200Turtle() {
        SparqlRequest req = SparqlRequest.query(
                "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }",
                null, null, ContentNegotiator.TURTLE
        );
        SparqlResponse res = service.executeQuery(req);

        assertEquals(200, res.statusCode());
    }


    @Test
    @DisplayName("Missing query -> 400")
    void missingQuery_returns400() {
        SparqlRequest req = SparqlRequest.query("", null, null, null);
        SparqlResponse res = service.executeQuery(req);

        assertEquals(400, res.statusCode());
    }

    @Test
    @DisplayName("Malformed SPARQL -> 400")
    void malformedQuery_returns400() {
        SparqlRequest req = SparqlRequest.query(
                "NOT VALID SPARQL !!!", null, null, null
        );
        SparqlResponse res = service.executeQuery(req);

        assertEquals(400, res.statusCode());
    }

    @Test
    @DisplayName("default-graph-uri parameter accepted")
    void datasetParam_defaultGraph_accepted() {
        SparqlRequest req = SparqlRequest.query(
                "SELECT * WHERE { ?s ?p ?o }",
                List.of("http://example.org/graph"),
                null, null
        );
        SparqlResponse res = service.executeQuery(req);

        assertNotEquals(500, res.statusCode());
    }

    @Test
    @DisplayName("named-graph-uri parameter accepted")
    void datasetParam_namedGraph_accepted() {
        SparqlRequest req = SparqlRequest.query(
                "SELECT * WHERE { ?s ?p ?o }",
                null,
                List.of("http://example.org/named"),
                null
        );
        SparqlResponse res = service.executeQuery(req);

        assertNotEquals(500, res.statusCode());
    }

    @Test
    @DisplayName("INSERT DATA -> 204 No Content")
    void insertData_returns204() {
        SparqlRequest req = SparqlRequest.update(
                "INSERT DATA { <http://ex.org/s> <http://ex.org/p> <http://ex.org/o> }",
                null, null
        );
        SparqlResponse res = service.executeUpdate(req);

        assertEquals(204, res.statusCode());
        assertFalse(res.hasBody());
    }

    @Test
    @DisplayName("Missing update -> 400")
    void missingUpdate_returns400() {
        SparqlRequest req = SparqlRequest.update("", null, null);
        SparqlResponse res = service.executeUpdate(req);

        assertEquals(400, res.statusCode());
    }

    @Test
    @DisplayName("INSERT then SELECT — data persists")
    void insertThenSelect_dataVisible() {
        // Insert a triple
        service.executeUpdate(SparqlRequest.update(
                "INSERT DATA { <http://ex.org/s> <http://ex.org/p> \"hello\" }",
                null, null
        ));

        // Query it back
        SparqlResponse res = service.executeQuery(SparqlRequest.query(
                "SELECT * WHERE { <http://ex.org/s> ?p ?o }",
                null, null, ContentNegotiator.SPARQL_RESULTS_XML
        ));

        assertEquals(200, res.statusCode());
        assertTrue(res.bodyAsString().contains("hello"));
    }
}
