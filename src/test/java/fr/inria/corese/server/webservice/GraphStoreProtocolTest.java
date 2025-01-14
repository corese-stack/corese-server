package fr.inria.corese.server.webservice;

import static fr.inria.corese.core.print.ResultFormat.TURTLE_TEXT;
import static fr.inria.corese.core.api.Loader.TURTLE_FORMAT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
    
/**
 * Test of the behavior of the corese server against graph store protocol requests.
 * 
 * @author Pierre Maillot, P16 Wimmics INRIA I3S, 2024
 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-http-rdf-update-20130321/">https://www.w3.org/TR/2013/REC-sparql11-http-rdf-update-20130321/</a>
 
 */
public class GraphStoreProtocolTest {
    

    private static final Logger logger = LogManager.getLogger(GraphStoreProtocolTest.class);

    private static Process server;

    private static final String SERVER_URL = "http://localhost:8080/";
    private static final String GRAPH_STORE_ENDPOINT = SERVER_URL + "rdf-graph-store";

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
               "-javaagent:" + jacocoAgentPath + "=destfile=" + startDirectory+"/jacoco/server_graphstore.exec,includes=fr.inria.corese.*",
                 "-jar", jar_file.getAbsolutePath(),
                "-lh",
                "-su",
                "-l", trigFileAbsolutePath,
                "-l", turtleFileAbsolutePath).start();
        Thread.sleep(7000);
    }

    @AfterClass
    public static void shutdown() {
        server.destroy();
    }

    @Test
    public void getGraphStoreProtocolWithGraph() throws Exception {

        List<List<String>> headers = new LinkedList<>();
        List<String> acceptHeader = new LinkedList<>();
        acceptHeader.add("Accept");
        acceptHeader.add(TURTLE_TEXT);
        headers.add(acceptHeader);

        String urlQuery = GRAPH_STORE_ENDPOINT + "?" + SPARQLTestUtils.generateGraphStoreParameters("http://example.com/A");
        HttpURLConnection con = HTTPConnectionUtils.getConnection(urlQuery, headers);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        int status = con.getResponseCode();

        con.disconnect();
        Graph describeGraph = new Graph();
        Load load = Load.create(describeGraph);
        InputStream inputStream = new ByteArrayInputStream(content.toString().getBytes());
        load.parse(inputStream, TURTLE_FORMAT);

        // FIXME: fix test
        // assertEquals(200, status);
        // assertEquals(1, describeGraph.size());
    }

    @Test
    public void getGraphStoreProtocolWithDefault() throws Exception{

        List<List<String>> headers = new LinkedList<>();
        List<String> acceptHeader = new LinkedList<>();
        acceptHeader.add("Accept");
        acceptHeader.add(TURTLE_TEXT);
        headers.add(acceptHeader);

        String urlQuery = GRAPH_STORE_ENDPOINT + "?" + SPARQLTestUtils.generateGraphStoreParameters("default");
        HttpURLConnection con = HTTPConnectionUtils.getConnection(urlQuery, headers);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        int status = con.getResponseCode();

        con.disconnect();
        Graph describeGraph = new Graph();
        Load load = Load.create(describeGraph);
        InputStream inputStream = new ByteArrayInputStream(content.toString().getBytes());
        load.parse(inputStream, TURTLE_FORMAT);

        // FIXME: fix test
        // assertEquals(200, status);
        // assertEquals(171, describeGraph.size());
    }

    @Test
    public void getGraphStoreProtocolWithUnknownGraph() throws Exception{
        
        boolean absenceTest = ! SPARQLTestUtils.sendSPARQLAsk("ASK { GRAPH <http://example.com/Z> { ?x ?y ?z } }");

        List<List<String>> headers = new LinkedList<>();
        List<String> acceptHeader = new LinkedList<>();
        acceptHeader.add("Accept");
        acceptHeader.add(TURTLE_TEXT);
        headers.add(acceptHeader);

        String urlQuery = GRAPH_STORE_ENDPOINT + "?" + SPARQLTestUtils.generateGraphStoreParameters("http://example.com/Z");
        HttpURLConnection con = HTTPConnectionUtils.getConnection(urlQuery, headers);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        int status = con.getResponseCode();

        con.disconnect();
        Graph describeGraph = new Graph();
        Load load = Load.create(describeGraph);
        InputStream inputStream = new ByteArrayInputStream(content.toString().getBytes());
        load.parse(inputStream, TURTLE_FORMAT);

        //FIXME: fix test
        // assertTrue(absenceTest);
        // assertEquals(404, status);
        // assertEquals(0, describeGraph.size());
    }

    @Test 
    public void putGraphStoreProtocolNewGraph() throws Exception {
        
        boolean absenceTest = SPARQLTestUtils.sendSPARQLAsk("ASK { GRAPH <http://example.com/C> { <http://example.com/C> a <http://example.com/Thing> } }");

        List<List<String>> headers = new LinkedList<>();
        List<String> acceptHeader = new LinkedList<>();
        acceptHeader.add("Content-Type");
        acceptHeader.add(TURTLE_TEXT);
        headers.add(acceptHeader);

        String urlQuery = GRAPH_STORE_ENDPOINT + "?" + SPARQLTestUtils.generateGraphStoreParameters("http://example.com/C");

        String rdfPayload = "@prefix ex: <http://example.com/> . ex:C a ex:Thing .";

        HttpURLConnection con = HTTPConnectionUtils.putConnection(urlQuery, headers, rdfPayload);

        int status = con.getResponseCode();

        con.disconnect();
        
        boolean presenceTest = SPARQLTestUtils.sendSPARQLAsk("ASK { GRAPH <http://example.com/C> { <http://example.com/C> a <http://example.com/Thing> } }");

        // FIXME: fix test
        // assertFalse(absenceTest);
        // assertEquals(201, status);
        // assertTrue(presenceTest);
    }

    @Test 
    public void putGraphStoreProtocolExistingGraph() throws Exception {
        
        boolean absenceTest = ! SPARQLTestUtils.sendSPARQLAsk("ASK { GRAPH <http://example.com/A> { <http://example.com/C> a <http://example.com/Thing> } }");

        List<List<String>> headers = new LinkedList<>();
        List<String> acceptHeader = new LinkedList<>();
        acceptHeader.add("Content-Type");
        acceptHeader.add(TURTLE_TEXT);
        headers.add(acceptHeader);

        String urlQuery = GRAPH_STORE_ENDPOINT + "?" + SPARQLTestUtils.generateGraphStoreParameters("http://example.com/A");

        String rdfPayload = "@prefix ex: <http://example.com/> . ex:C a ex:Thing .";

        HttpURLConnection con = HTTPConnectionUtils.putConnection(urlQuery, headers, rdfPayload);

        int status = con.getResponseCode();

        con.disconnect();
        
        boolean presenceTest = SPARQLTestUtils.sendSPARQLAsk("ASK { GRAPH <http://example.com/A> { <http://example.com/C> a <http://example.com/Thing> } }");

        // FIXME: fix test
        // assertTrue(status == 200 || status == 204);
        // assertTrue(presenceTest);
        // assertTrue(absenceTest);
    }

    @Test
    public void deleteGraphStoreProtocol() throws Exception {

        boolean presenceTest = SPARQLTestUtils.sendSPARQLAsk("ASK { GRAPH <http://example.com/B> { ?s ?p ?o } }");

        String urlQuery = GRAPH_STORE_ENDPOINT + "?" + SPARQLTestUtils.generateGraphStoreParameters("http://example.com/B");
        HttpURLConnection deleteCon = HTTPConnectionUtils.deleteConnection(urlQuery);

        int status = deleteCon.getResponseCode();

        deleteCon.disconnect();

        boolean absenceTest = ! SPARQLTestUtils.sendSPARQLAsk("ASK { GRAPH <http://example.com/B> { ?s ?p ?o } }");

        // FIXME: fix test
        // assertTrue(status == 200 || status == 204);
        // assertTrue(presenceTest);
        // assertTrue(absenceTest);
    }

    @Test
    public void deleteGraphStoreProtocolWithUnknownGraph() throws Exception {

        boolean presenceTest = ! SPARQLTestUtils.sendSPARQLAsk("ASK { GRAPH <http://example.com/Z> { ?s ?p ?o } }");

        String urlQuery = GRAPH_STORE_ENDPOINT + "?" + SPARQLTestUtils.generateGraphStoreParameters("http://example.com/Z");
        HttpURLConnection deleteCon = HTTPConnectionUtils.deleteConnection(urlQuery);

        int status = deleteCon.getResponseCode();

        deleteCon.disconnect();

        boolean absenceTest = ! SPARQLTestUtils.sendSPARQLAsk("ASK { GRAPH <http://example.com/Z> { ?s ?p ?o } }");

        // FIXME: fix test
        // assertEquals(404, status);
        // assertTrue(presenceTest);
        // assertTrue(absenceTest);
    }

    @Test 
    public void postGraphStoreProtocolNewGraph() throws Exception {
        
        boolean absenceTest = ! SPARQLTestUtils.sendSPARQLAsk("ASK { GRAPH <http://example.com/C> { <http://example.com/C> a <http://example.com/Thing> } }");

        List<List<String>> headers = new LinkedList<>();
        List<String> acceptHeader = new LinkedList<>();
        acceptHeader.add("Content-Type");
        acceptHeader.add(TURTLE_TEXT);
        headers.add(acceptHeader);

        String urlQuery = GRAPH_STORE_ENDPOINT + "?" + SPARQLTestUtils.generateGraphStoreParameters("http://example.com/C");

        String rdfPayload = "@prefix ex: <http://example.com/> . ex:C a ex:Thing .";

        HttpURLConnection con = HTTPConnectionUtils.postConnection(urlQuery, headers, rdfPayload);

        int status = con.getResponseCode();

        con.disconnect();
        
        boolean presenceTest = SPARQLTestUtils.sendSPARQLAsk("ASK { GRAPH <http://example.com/C> { <http://example.com/C> a <http://example.com/Thing> } }");

        // FIXME: fix test
        // assertEquals(201, status);
        // assertTrue(presenceTest);
        // assertTrue(absenceTest);
    }

    @Test 
    public void postGraphStoreProtocolExistingGraph() throws Exception {
        
        boolean absenceTest = ! SPARQLTestUtils.sendSPARQLAsk("ASK { GRAPH <http://example.com/A> { <http://example.com/C> a <http://example.com/Thing> } }");

        List<List<String>> headers = new LinkedList<>();
        List<String> acceptHeader = new LinkedList<>();
        acceptHeader.add("Content-Type");
        acceptHeader.add(TURTLE_TEXT);
        headers.add(acceptHeader);

        String urlQuery = GRAPH_STORE_ENDPOINT + "?" + SPARQLTestUtils.generateGraphStoreParameters("http://example.com/A");

        String rdfPayload = "@prefix ex: <http://example.com/> . ex:C a ex:Thing .";

        HttpURLConnection con = HTTPConnectionUtils.postConnection(urlQuery, headers, rdfPayload);

        int status = con.getResponseCode();

        con.disconnect();
        
        boolean presenceTest = SPARQLTestUtils.sendSPARQLAsk("ASK { GRAPH <http://example.com/A> { <http://example.com/C> a <http://example.com/Thing> } }");

        // FIXME: fix test
        // assertTrue(status == 200 || status == 204);
        // assertTrue(presenceTest);
        // assertTrue(absenceTest);
    }

    @Test
    public void headGraphStoreProtocolWithDefault() throws Exception{

        List<List<String>> headers = new LinkedList<>();
        List<String> acceptHeader = new LinkedList<>();
        acceptHeader.add("Accept");
        acceptHeader.add(TURTLE_TEXT);
        headers.add(acceptHeader);

        String urlQuery = GRAPH_STORE_ENDPOINT + "?" + SPARQLTestUtils.generateGraphStoreParameters("default");
        HttpURLConnection con = HTTPConnectionUtils.headConnection(urlQuery);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        int status = con.getResponseCode();

        con.disconnect();

        assertEquals(200, status);
        assertEquals(0, content.toString().length());
    }

    @Test
    public void headGraphStoreProtocolWithGraph() throws Exception{
        
        boolean presenceTest = SPARQLTestUtils.sendSPARQLAsk("ASK { GRAPH <http://example.com/A> { ?x ?y ?z } }");

        String urlQuery = GRAPH_STORE_ENDPOINT + "?" + SPARQLTestUtils.generateGraphStoreParameters("http://example.com/A");
        HttpURLConnection con = HTTPConnectionUtils.headConnection(urlQuery);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        int status = con.getResponseCode();

        con.disconnect();

        assertTrue(presenceTest);
        assertEquals(200, status);
        assertEquals(0, content.toString().length());
    }

    @Test
    public void headGraphStoreProtocolWithUnknownGraph() throws Exception{
        
        boolean absenceTest = ! SPARQLTestUtils.sendSPARQLAsk("ASK { GRAPH <http://example.com/Z> { ?x ?y ?z } }");

        String urlQuery = GRAPH_STORE_ENDPOINT + "?" + SPARQLTestUtils.generateGraphStoreParameters("http://example.com/Z");
        HttpURLConnection con = HTTPConnectionUtils.headConnection(urlQuery);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        int status = con.getResponseCode();

        con.disconnect();

        // FIXME: fix test
        // assertTrue(absenceTest);
        // assertEquals(404, status);
        // assertEquals(0, content.toString().length());
    }

}
