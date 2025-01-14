package fr.inria.corese.server.webservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test of the behavior of the corese server against SPARQL Updates.
 * 
 * @author Pierre Maillot, P16 Wimmics INRIA I3S, 2024
 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-protocol-20130321/#update-operation">https://www.w3.org/TR/2013/REC-sparql11-protocol-20130321/#update-operation</a>
 
 */
public class SPARQLEndpointUpdateTest {
    

    private static final Logger logger = LogManager.getLogger(SPARQLEndpointUpdateTest.class);

    private static Process server;

    private static final String SERVER_URL = "http://localhost:8080/";
    private static final String SPARQL_ENDPOINT_URL = SERVER_URL + "sparql";

    /**
     * Start the server before running the tests.
     * Loads a part of the DBpedia dataset in the server.
     */
    @BeforeClass
    public static void init() throws InterruptedException, IOException {
        File turtleFile = new File("src/test/resources/data.ttl");
        String turtleFileAbsolutePath = turtleFile.getAbsolutePath();

        File trigFile = new File("src/test/resources/data.trig");
        String trigFileAbsolutePath = trigFile.getAbsolutePath();

        String startDirectory = "build";
        Pattern pattern = Pattern.compile("corese-server-(\\d+)\\.(\\d+)\\.(\\d+)-SNAPSHOT-app\\.jar");
        File jar_file = HTTPConnectionUtils.findFileRecursively(pattern, new File(startDirectory));

        Pattern jacoco_pattern = Pattern.compile("jacocoagent\\.jar");
        File jacoco_jar_file = HTTPConnectionUtils.findFileRecursively(jacoco_pattern, new File(startDirectory));

        String jacocoAgentPath = jacoco_jar_file.getAbsolutePath();

        logger.info("starting in " + System.getProperty("user.dir"));
        server = new ProcessBuilder().inheritIO().command(
                "java",
                "-javaagent:" + jacocoAgentPath + "=destfile=" + startDirectory+"/jacoco/server_sparqlep.exec,includes=fr.inria.corese.*",
                "-jar", jar_file.getAbsolutePath(),
                "-lh",
                "-l", turtleFileAbsolutePath,
                "-l", trigFileAbsolutePath,
                "-su").start();
        Thread.sleep(7000);
    }

    @AfterClass
    public static void shutdown() {
        server.destroy();
    }

    /**
     * Test the insertion of a triple in the server using a POST request with a URL-encoded body.
     * @throws Exception
     */
    @Test
    public void postUrlencodedAcceptSPARQLXmlUpdateTest() throws Exception {
        String query = "INSERT DATA { <http://example.org/s> <http://example.org/p> <http://example.org/o> }";
        String body = "update=" + URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        List<List<String>> headers = new LinkedList<>();
        List<String> contentTypeHeader = new LinkedList<>();
        contentTypeHeader.add("Content-Type");
        contentTypeHeader.add("application/x-www-form-urlencoded");
        List<String> acceptHeader = new LinkedList<>();
        acceptHeader.add("Accept");
        acceptHeader.add("application/sparql-results+xml");
        headers.add(contentTypeHeader);
        headers.add(acceptHeader);
        HttpURLConnection con = HTTPConnectionUtils.postConnection(SPARQL_ENDPOINT_URL, headers, body);
        int responseCode = con.getResponseCode();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();

        assertEquals(200, responseCode);
        assertEquals("<?xml version=\"1.0\" ?><sparql xmlns='http://www.w3.org/2005/sparql-results#'><head></head><results><result></result></results></sparql>", content.toString());
    }

    /**
     * Test the insertion of a triple in the server using a POST request with a URL-encoded body.
     * @throws Exception
     */
    @Test
    public void postUrlencodedAcceptCSVUpdateTest() throws Exception {
        String query = "INSERT DATA { <http://example.org/s> <http://example.org/p> <http://example.org/o> }";
        String body = "update=" + URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        List<List<String>> headers = new LinkedList<>();
        List<String> contentTypeHeader = new LinkedList<>();
        contentTypeHeader.add("Content-Type");
        contentTypeHeader.add("application/x-www-form-urlencoded");
        List<String> acceptHeader = new LinkedList<>();
        acceptHeader.add("Accept");
        acceptHeader.add("text/csv");
        headers.add(contentTypeHeader);
        headers.add(acceptHeader);
        HttpURLConnection con = HTTPConnectionUtils.postConnection(SPARQL_ENDPOINT_URL, headers, body);
        int responseCode = con.getResponseCode();
        
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();

        assertEquals(200, responseCode);
        assertEquals("", content.toString());
    }

    /**
     * Test the insertion of a triple in the server using a POST request with a URL-encoded body.
     * @throws Exception
     */
    @Test
    public void postUrlencodedAcceptJSONUpdateTest() throws Exception {
        String query = "INSERT DATA { <http://example.org/s> <http://example.org/p> <http://example.org/o> }";
        String body = "update=" + URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        List<List<String>> headers = new LinkedList<>();
        List<String> contentTypeHeader = new LinkedList<>();
        contentTypeHeader.add("Content-Type");
        contentTypeHeader.add("application/x-www-form-urlencoded");
        List<String> acceptHeader = new LinkedList<>();
        acceptHeader.add("Accept");
        acceptHeader.add("application/sparql-results+json");
        headers.add(contentTypeHeader);
        headers.add(acceptHeader);
        HttpURLConnection con = HTTPConnectionUtils.postConnection(SPARQL_ENDPOINT_URL, headers, body);
        int responseCode = con.getResponseCode();
        
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();

        assertEquals(200, responseCode);
        assertEquals("{\"head\": { \"vars\": []},\"results\": { \"bindings\": [{}] }}", content.toString());
    }

    /**
     * Test the insertion of a triple in the server using a POST request with a URL-encoded body.
     * @throws Exception
     */
    @Test
    public void postAcceptSPARQLXmlUpdateTest() throws Exception {
        String query = "INSERT DATA { <http://example.org/s> <http://example.org/p> <http://example.org/o> }";
        String body = query;
        List<List<String>> headers = new LinkedList<>();
        List<String> contentTypeHeader = new LinkedList<>();
        contentTypeHeader.add("Content-Type");
        contentTypeHeader.add("application/sparql-update");
        List<String> acceptHeader = new LinkedList<>();
        acceptHeader.add("Accept");
        acceptHeader.add("application/sparql-results+xml");
        headers.add(contentTypeHeader);
        headers.add(acceptHeader);
        HttpURLConnection con = HTTPConnectionUtils.postConnection(SPARQL_ENDPOINT_URL, headers, body);
        int responseCode = con.getResponseCode();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();

        assertEquals(200, responseCode);
        assertEquals("<?xml version=\"1.0\" ?><sparql xmlns='http://www.w3.org/2005/sparql-results#'><head></head><results><result></result></results></sparql>", content.toString());
    }

    /**
     * Test the insertion of a triple in the server using a POST request with a URL-encoded body.
     * @throws Exception
     */
    @Test
    public void postAcceptCSVUpdateTest() throws Exception {
        String query = "INSERT DATA { <http://example.org/s> <http://example.org/p> <http://example.org/o> }";
        String body = query;
        List<List<String>> headers = new LinkedList<>();
        List<String> contentTypeHeader = new LinkedList<>();
        contentTypeHeader.add("Content-Type");
        contentTypeHeader.add("application/sparql-update");
        List<String> acceptHeader = new LinkedList<>();
        acceptHeader.add("Accept");
        acceptHeader.add("text/csv");
        headers.add(contentTypeHeader);
        headers.add(acceptHeader);
        HttpURLConnection con = HTTPConnectionUtils.postConnection(SPARQL_ENDPOINT_URL, headers, body);
        int responseCode = con.getResponseCode();
        
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();

        assertEquals(200, responseCode);
        assertEquals("", content.toString());
    }

    /**
     * Test the insertion of a triple in the server using a POST request with a URL-encoded body.
     * @throws Exception
     */
    @Test
    public void postAcceptJSONUpdateTest() throws Exception {
        String query = "INSERT DATA { <http://example.org/s> <http://example.org/p> <http://example.org/o> }";
        String body = query;
        List<List<String>> headers = new LinkedList<>();
        List<String> contentTypeHeader = new LinkedList<>();
        contentTypeHeader.add("Content-Type");
        contentTypeHeader.add("application/sparql-update");
        List<String> acceptHeader = new LinkedList<>();
        acceptHeader.add("Accept");
        acceptHeader.add("application/sparql-results+json");
        headers.add(contentTypeHeader);
        headers.add(acceptHeader);
        HttpURLConnection con = HTTPConnectionUtils.postConnection(SPARQL_ENDPOINT_URL, headers, body);
        int responseCode = con.getResponseCode();
        
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();

        assertEquals(200, responseCode);
        assertEquals("{\"head\": { \"vars\": []},\"results\": { \"bindings\": [{}] }}", content.toString());
    }

    /**
     * Test the insertion of a triple in the server using a POST request with a SPARQL Update body.
     * @throws Exception
     */
    @Test
    public void postUpdateTest() throws Exception {
        String query = "INSERT DATA { <http://example.org/s> <http://example.org/p> <http://example.org/o> }";
        List<List<String>> headers = new LinkedList<>();
        List<String> contentTypeHeader = new LinkedList<>();
        contentTypeHeader.add("Content-Type");
        contentTypeHeader.add("application/sparql-update");
        headers.add(contentTypeHeader);
        HttpURLConnection con = HTTPConnectionUtils.postConnection(SPARQL_ENDPOINT_URL, headers, query);
        int responseCode = con.getResponseCode();
        con.disconnect();

        // Send a query to check if the instance was inserted
        String askQuery = "ASK { <http://example.org/s> <http://example.org/p> <http://example.org/o> }";
        boolean askResult = SPARQLTestUtils.sendSPARQLAsk(askQuery);

        assertTrue(responseCode >= 200 && responseCode < 400);
        assertTrue(askResult);
    }

    /**
     * Test the insertion of a triple in the server using a POST request with a URL-encoded body.
     * @throws Exception
     */
    @Test
    public void usingNamedGraphUpdateTest() throws Exception {
        // Insert a new instance in ex:A
        String updateQuery = "PREFIX owl: <http://www.w3.org/2002/07/owl#> INSERT { <http://example.com/Another> a owl:Thing } WHERE { <http://example.com/A> a owl:Thing }";
        List<List<String>> updateParameters = new LinkedList<>();
        List<String> graphParameter = new LinkedList<>();
        graphParameter.add("using-graph-uri");
        graphParameter.add("http://example.com/A");
        updateParameters.add(graphParameter);
        List<List<String>> updateHeaders = new LinkedList<>();
        List<String> contentTypeFormUrlEncodedHeader = new LinkedList<>();
        contentTypeFormUrlEncodedHeader.add("Content-Type");
        contentTypeFormUrlEncodedHeader.add("application/sparql-update");
        updateHeaders.add(contentTypeFormUrlEncodedHeader);
        HttpURLConnection updateCon = HTTPConnectionUtils.postConnection(SPARQL_ENDPOINT_URL, updateHeaders, updateQuery);
        int updateResponseCode = updateCon.getResponseCode();
        updateCon.disconnect();

        // Should be present in the dataset as it is loaded
        String askQueryABaseline = "PREFIX owl: <http://www.w3.org/2002/07/owl#> ASK { GRAPH <http://example.com/A> { <http://example.com/A> a owl:Thing } }";
        boolean askResultABaseline = SPARQLTestUtils.sendSPARQLAsk(askQueryABaseline);
        // Should have been created by the update
        String askQueryA = "PREFIX owl: <http://www.w3.org/2002/07/owl#> ASK { GRAPH <http://example.com/A> { <http://example.com/Another> a owl:Thing } }";
        boolean askResultA = SPARQLTestUtils.sendSPARQLAsk(askQueryA);
        // Should not be present in the dataset
        String askQueryB = "PREFIX owl: <http://www.w3.org/2002/07/owl#> ASK { GRAPH <http://example.com/B> { <http://example.com/Another> a owl:Thing } }";
        boolean askResultB = SPARQLTestUtils.sendSPARQLAsk(askQueryB);

        assertEquals(200, updateResponseCode);
        assertTrue(updateResponseCode >= 200 && updateResponseCode < 400);
        assertTrue(askResultABaseline);
        // FIXME: fix test
        // assertTrue(askResultA);
        assertFalse(askResultB);
    }
}
