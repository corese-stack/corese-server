package fr.inria.corese.server.http.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SparqlRequest}.
 */
class SparqlRequestTest {


    @Test
    void queryFactory_setsType() {
        SparqlRequest req = SparqlRequest.query("SELECT * WHERE { ?s ?p ?o }", null, null, null);
        assertTrue(req.isQuery());
        assertFalse(req.isUpdate());
        assertEquals(SparqlRequest.OperationType.QUERY, req.operationType());
    }

    @Test
    void queryFactory_setsQueryString() {
        String sparql = "SELECT * WHERE { ?s ?p ?o }";
        SparqlRequest req = SparqlRequest.query(sparql, null, null, null);
        assertEquals(sparql, req.queryString());
        assertTrue(req.hasQuery());
    }

    @Test
    void queryFactory_nullListsBecomesEmpty() {
        SparqlRequest req = SparqlRequest.query("SELECT * WHERE {}", null, null, null);
        assertNotNull(req.defaultGraphUris());
        assertNotNull(req.namedGraphUris());
        assertTrue(req.defaultGraphUris().isEmpty());
        assertTrue(req.namedGraphUris().isEmpty());
        assertFalse(req.hasDataset());
    }

    @Test
    void queryFactory_withDataset() {
        SparqlRequest req = SparqlRequest.query(
                "SELECT * WHERE {}",
                List.of("http://example.org/g1"),
                List.of("http://example.org/g2"),
                "application/sparql-results+json"
        );
        assertTrue(req.hasDataset());
        assertEquals(1, req.defaultGraphUris().size());
        assertEquals(1, req.namedGraphUris().size());
        assertEquals("application/sparql-results+json", req.acceptHeader());
    }

    @Test
    void queryFactory_emptyQueryString_hasQueryFalse() {
        SparqlRequest req = SparqlRequest.query("", null, null, null);
        assertFalse(req.hasQuery());
    }

    @Test
    void queryFactory_blankQueryString_hasQueryFalse() {
        SparqlRequest req = SparqlRequest.query("   ", null, null, null);
        assertFalse(req.hasQuery());
    }

    // Factory: update

    @Test
    void updateFactory_setsType() {
        SparqlRequest req = SparqlRequest.update(
                "INSERT DATA { <s> <p> <o> }", null, null);
        assertTrue(req.isUpdate());
        assertFalse(req.isQuery());
    }

    @Test
    void updateFactory_setsUpdateString() {
        String sparql = "INSERT DATA { <s> <p> <o> }";
        SparqlRequest req = SparqlRequest.update(sparql, null, null);
        assertEquals(sparql, req.updateString());
        assertTrue(req.hasUpdate());
    }

    @Test
    void updateFactory_emptyUpdate_hasUpdateFalse() {
        SparqlRequest req = SparqlRequest.update("", null, null);
        assertFalse(req.hasUpdate());
    }
}
