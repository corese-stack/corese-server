package fr.inria.corese.server.http.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SparqlResponse}.
 */
class SparqlResponseTest {

    @Test
    void ok_string_returns200() {
        SparqlResponse r = SparqlResponse.ok("application/sparql-results+xml", "<sparql/>");
        assertEquals(200, r.statusCode());
        assertEquals("application/sparql-results+xml", r.contentType());
        assertTrue(r.hasBody());
        assertTrue(r.isSuccess());
        assertEquals("<sparql/>", r.bodyAsString());
    }

    @Test
    void ok_bytes_returns200() {
        byte[] body = "hello".getBytes();
        SparqlResponse r = SparqlResponse.ok("text/plain", body);
        assertEquals(200, r.statusCode());
        assertArrayEquals(body, r.body());
    }

    @Test
    void noContent_returns204() {
        SparqlResponse r = SparqlResponse.noContent();
        assertEquals(204, r.statusCode());
        assertNull(r.contentType());
        assertFalse(r.hasBody());
        assertTrue(r.isSuccess());
    }

    @Test
    void badRequest_returns400() {
        SparqlResponse r = SparqlResponse.badRequest("Missing query");
        assertEquals(400, r.statusCode());
        assertFalse(r.isSuccess());
        assertTrue(r.bodyAsString().contains("Missing query"));
    }

    @Test
    void serverError_returns500() {
        SparqlResponse r = SparqlResponse.serverError("Internal error");
        assertEquals(500, r.statusCode());
        assertFalse(r.isSuccess());
        assertTrue(r.bodyAsString().contains("Internal error"));
    }
}
