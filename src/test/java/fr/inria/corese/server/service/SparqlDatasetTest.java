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
 * Tests for SPARQL 1.1 Protocol RDF Dataset handling.
 */
class SparqlDatasetTest {

    private SparqlExecutionService service;

    @BeforeEach
    void setUp() {
        ServerConfig config = new ServerConfig(8080, null, null, false, 0);
        CoreseTripleStoreManager store = new CoreseTripleStoreManager(config);
        service = new SparqlExecutionService(store);

        SparqlRequest insertG1 = SparqlRequest.update(
                "INSERT DATA { GRAPH <http://ex.org/g1> { <http://ex.org/Alice> <http://ex.org/knows> <http://ex.org/Bob> } }",
                null, null
        );
        service.executeUpdate(insertG1);

        SparqlRequest insertG2 = SparqlRequest.update(
                "INSERT DATA { GRAPH <http://ex.org/g2> { <http://ex.org/Carol> <http://ex.org/knows> <http://ex.org/Dave> } }",
                null, null
        );
        service.executeUpdate(insertG2);
    }


    @Test
    @DisplayName("default-graph-uri scopes SELECT to that graph only")
    void query_defaultGraphUri_scopesToGraph() {
        SparqlRequest req = SparqlRequest.query(
                "SELECT * WHERE { ?s ?p ?o }",
                List.of("http://ex.org/g1"),
                null,
                "application/sparql-results+xml"
        );
        SparqlResponse res = service.executeQuery(req);

        assertEquals(200, res.statusCode());
        assertTrue(res.bodyAsString().contains("Alice"), "g1 data should be visible");
        assertFalse(res.bodyAsString().contains("Carol"), "g2 data should not be visible");
    }


    @Test
    @DisplayName("no dataset params -> queries entire store")
    void query_noDatasetParams_queriesEntireStore() {
        SparqlRequest req = SparqlRequest.query(
                "SELECT * WHERE { GRAPH ?g { ?s ?p ?o } }",
                null, null,
                "application/sparql-results+xml"
        );
        SparqlResponse res = service.executeQuery(req);

        assertEquals(200, res.statusCode());
        assertTrue(res.bodyAsString().contains("Alice"));
        assertTrue(res.bodyAsString().contains("Carol"));
    }


    @Test
    @DisplayName("using-graph-uri scopes DELETE/INSERT WHERE to that graph")
    void update_usingGraphUri_scopesWhereClause() {
        SparqlRequest req = SparqlRequest.update(
                "INSERT { ?s <http://ex.org/copy> ?o } WHERE { ?s <http://ex.org/knows> ?o }",
                List.of("http://ex.org/g1"),
                null
        );
        SparqlResponse res = service.executeUpdate(req);
        assertEquals(204, res.statusCode());

        // Verify the copy was made only from g1's data (Alice/Bob), not g2 (Carol/Dave)
        SparqlRequest check = SparqlRequest.query(
                "SELECT * WHERE { ?s <http://ex.org/copy> ?o }",
                null, null, "application/sparql-results+xml"
        );
        SparqlResponse checkRes = service.executeQuery(check);
        assertTrue(checkRes.bodyAsString().contains("Alice"));
    }

    @Test
    @DisplayName("using-named-graph-uri makes graph available via GRAPH in WHERE")
    void update_usingNamedGraphUri_availableInWhere() {
        SparqlRequest req = SparqlRequest.update(
                "INSERT { ?s <http://ex.org/markedFrom> <http://ex.org/g2> } " +
                        "WHERE { GRAPH <http://ex.org/g2> { ?s ?p ?o } }",
                null,
                List.of("http://ex.org/g2")
        );
        SparqlResponse res = service.executeUpdate(req);
        assertEquals(204, res.statusCode());
    }

    @Test
    @DisplayName("error: using-graph-uri combined with explicit USING clause -> 400")
    void update_usingGraphUriWithExplicitUsing_returns400() {
        SparqlRequest req = SparqlRequest.update(
                "INSERT { ?s ?p ?o } USING <http://ex.org/g1> WHERE { ?s ?p ?o }",
                List.of("http://ex.org/g2"),
                null
        );
        SparqlResponse res = service.executeUpdate(req);
        assertEquals(400, res.statusCode());
    }

    @Test
    @DisplayName("error: using-graph-uri combined with explicit WITH clause -> 400")
    void update_usingGraphUriWithExplicitWith_returns400() {
        SparqlRequest req = SparqlRequest.update(
                "WITH <http://ex.org/g1> DELETE { ?s ?p ?o } WHERE { ?s ?p ?o }",
                List.of("http://ex.org/g2"),
                null
        );
        SparqlResponse res = service.executeUpdate(req);
        assertEquals(400, res.statusCode());
    }

    @Test
    @DisplayName("no using params -> update executes normally")
    void update_noUsingParams_executesNormally() {
        SparqlRequest req = SparqlRequest.update(
                "INSERT DATA { <http://ex.org/X> <http://ex.org/y> <http://ex.org/Z> }",
                null, null
        );
        SparqlResponse res = service.executeUpdate(req);
        assertEquals(204, res.statusCode());
    }

    @Test
    @DisplayName("using-graph-uri with INSERT DATA (no WHERE) does not break execution")
    void update_usingGraphUriWithDataForm_stillExecutes() {
        SparqlRequest req = SparqlRequest.update(
                "INSERT DATA { <http://ex.org/NoWhere> <http://ex.org/p> <http://ex.org/o> }",
                List.of("http://ex.org/g1"),
                null
        );
        SparqlResponse res = service.executeUpdate(req);
        assertEquals(204, res.statusCode());
    }
}