package fr.inria.corese.server.service;

import fr.inria.corese.core.sparql.api.ResultFormatDef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link ContentNegotiator}.
 */
class ContentNegotiatorTest {

    @Test
    void nullAccept_defaultsToXML() {
        assertEquals(ContentNegotiator.SPARQL_RESULTS_XML,
                ContentNegotiator.resolveForResultSet(null));
    }

    @Test
    void blankAccept_defaultsToXML() {
        assertEquals(ContentNegotiator.SPARQL_RESULTS_XML,
                ContentNegotiator.resolveForResultSet("   "));
    }

    @Test
    void jsonAccept_returnsJSON() {
        assertEquals(ContentNegotiator.SPARQL_RESULTS_JSON,
                ContentNegotiator.resolveForResultSet("application/sparql-results+json"));
    }

    @Test
    void applicationJsonAccept_returnsJSON() {
        assertEquals(ContentNegotiator.SPARQL_RESULTS_JSON,
                ContentNegotiator.resolveForResultSet("application/json"));
    }

    @Test
    void xmlAccept_returnsXML() {
        assertEquals(ContentNegotiator.SPARQL_RESULTS_XML,
                ContentNegotiator.resolveForResultSet("application/sparql-results+xml"));
    }

    @Test
    void csvAccept_returnsCSV() {
        assertEquals(ContentNegotiator.SPARQL_RESULTS_CSV,
                ContentNegotiator.resolveForResultSet("text/csv"));
    }

    @Test
    void tsvAccept_returnsTSV() {
        assertEquals(ContentNegotiator.SPARQL_RESULTS_TSV,
                ContentNegotiator.resolveForResultSet("text/tab-separated-values"));
    }

    @Test
    void unknownAccept_defaultsToXML() {
        assertEquals(ContentNegotiator.SPARQL_RESULTS_XML,
                ContentNegotiator.resolveForResultSet("text/html"));
    }


    @Test
    void nullAccept_graphDefaultsTurtle() {
        assertEquals(ContentNegotiator.TURTLE,
                ContentNegotiator.resolveForGraph(null));
    }

    @Test
    void turtleAccept_returnsTurtle() {
        assertEquals(ContentNegotiator.TURTLE,
                ContentNegotiator.resolveForGraph("text/turtle"));
    }

    @Test
    void rdfXmlAccept_returnsRdfXml() {
        assertEquals(ContentNegotiator.RDF_XML,
                ContentNegotiator.resolveForGraph("application/rdf+xml"));
    }

    @Test
    void nTriplesAccept_returnsNTriples() {
        assertEquals(ContentNegotiator.N_TRIPLES,
                ContentNegotiator.resolveForGraph("application/n-triples"));
    }

    @Test
    void jsonLdAccept_returnsJsonLd() {
        assertEquals(ContentNegotiator.JSON_LD,
                ContentNegotiator.resolveForGraph("application/ld+json"));
    }

    @Test
    void trigAccept_returnsTrig() {
        assertEquals(ContentNegotiator.TRIG,
                ContentNegotiator.resolveForGraph("application/trig"));
    }


    @Test
    void toFormat_json_mapsCorrectly() {
        assertEquals(ResultFormatDef.format.JSON_FORMAT,
                ContentNegotiator.toFormat(ContentNegotiator.SPARQL_RESULTS_JSON));
    }

    @Test
    void toFormat_xml_mapsCorrectly() {
        assertEquals(ResultFormatDef.format.XML_FORMAT,
                ContentNegotiator.toFormat(ContentNegotiator.SPARQL_RESULTS_XML));
    }

    @Test
    void toFormat_csv_mapsCorrectly() {
        assertEquals(ResultFormatDef.format.CSV_FORMAT,
                ContentNegotiator.toFormat(ContentNegotiator.SPARQL_RESULTS_CSV));
    }

    @Test
    void toFormat_turtle_mapsCorrectly() {
        assertEquals(ResultFormatDef.format.TURTLE_FORMAT,
                ContentNegotiator.toFormat(ContentNegotiator.TURTLE));
    }

    @Test
    void toFormat_unknown_defaultsToXML() {
        assertEquals(ResultFormatDef.format.XML_FORMAT,
                ContentNegotiator.toFormat("unknown/type"));
    }
}
