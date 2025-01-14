package fr.inria.corese.server.webservice;

import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.load.Service;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Test of the behavior of the corese server against HTTP requests.
 * 
 * Tests:
 * - Does the server answers to a simple HTTP GET request?
 * - Does the server returns HTTP headers with the appropriate values?
 * 
 * @author Pierre Maillot, P16 Wimmics INRIA I3S, 2024
 * @author Olivier Corby, Wimmics INRIA I3S, 2015
 */
public class HttpServerTest {

    private static final Logger logger = LogManager.getLogger(HttpServerTest.class);

    private static Process server;

    private static final String SERVER_URL = "http://localhost:8080/";
    private static final String SPARQL_ENDPOINT_URL = SERVER_URL + "sparql";

    /**
     * Get a connection to a server.
     * 
     * @param url     server URL
     * @param headers HTTP headers
     * @return
     * @throws MalformedURLException
     * @throws IOException
     * @throws ProtocolException
     */
    private HttpURLConnection getConnection(String url, Map<String, String> headers)
            throws MalformedURLException, IOException, ProtocolException {
        URL u = new URL(url);
        HttpURLConnection con = (HttpURLConnection) u.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setInstanceFollowRedirects(true);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            con.setRequestProperty(entry.getKey(), entry.getValue());
        }
        return con;
    }

    private HttpURLConnection postConnection(String url, Map<String, String> headers, String body)
            throws MalformedURLException, IOException, ProtocolException {
        URL u = new URL(url);
        HttpURLConnection con = (HttpURLConnection) u.openConnection();
        con.setRequestMethod("POST");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setInstanceFollowRedirects(true);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            con.setRequestProperty(entry.getKey(), entry.getValue());
        }
        con.setDoOutput(true);
        con.getOutputStream().write(body.getBytes());
        return con;
    }

    private String generateSPARQLQueryUrl(String query, Map<String, String> optionalParameters) {
        try {
        String result = SPARQL_ENDPOINT_URL + "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        if (!optionalParameters.isEmpty()) {
            result += "&" + optionalParameters.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("&"));
        }
            return result;
        } catch (UnsupportedEncodingException e) {
            logger.error(e);
            return null;
        }
    }

    private String generateSPARQLQueryUrl(String query) {
        return generateSPARQLQueryUrl(query, new HashMap<>());
    }


    /**
     * Start the server before running the tests.
     * Loads a part of the DBpedia dataset in the server.
     */
    @BeforeClass
    public static void init() throws InterruptedException, IOException {
        System.out.println("starting in " + System.getProperty("user.dir"));
        String startDirectory = "build";
        Pattern pattern = Pattern.compile("corese-server-(\\d+)\\.(\\d+)\\.(\\d+)-SNAPSHOT-app\\.jar");
        File jar_file = HTTPConnectionUtils.findFileRecursively(pattern, new File(startDirectory));

        Pattern jacoco_pattern = Pattern.compile("jacocoagent\\.jar");
        File jacoco_jar_file = HTTPConnectionUtils.findFileRecursively(jacoco_pattern, new File(startDirectory));
        String jacocoAgentPath = jacoco_jar_file.getAbsolutePath();

        server = new ProcessBuilder().inheritIO().command(
                "java",
                "-javaagent:" + jacocoAgentPath + "=destfile=" + startDirectory+"/jacoco/server_http.exec,includes=fr.inria.corese.*",
                "-jar", jar_file.getAbsolutePath(),
                "-lh",
                "-l", "./build/resources/main/webapp/data/dbpedia/dbpedia.ttl").start();
        Thread.sleep(7000);
    }

    @AfterClass
    public static void shutdown() {
        server.destroy();
    }

    @Test
    public void test() throws LoadException, EngineException {
        Service serv = new Service("http://localhost:8080/sparql");
        String q = "select * where {?x ?p ?y} limit 10";
        Mappings map = serv.select(q);
        for (Mapping m : map) {
            System.out.println(map);
        }
        assertEquals(10, map.size());
    }

    // @Test
    // public void test2() {
    //     String service = "http://localhost:8080/template";
    //     Client client = ClientBuilder.newClient();
    //     WebTarget target = client.target(service);
    //     String res = target.queryParam("profile", "st:dbedit").request().get(String.class);
    //     assertEquals(true, res.length() > 17000);
    //     assertEquals(true, res.contains("Front yougoslave de la Seconde Guerre mondiale"));
    //     System.out.println(res.length());
    // }

    // @Test
    // public void test3() {
    //     String service = "http://localhost:8080/template";
    //     Client client = ClientBuilder.newClient();
    //     WebTarget target = client.target(service);
    //     String res = target.queryParam("profile", "st:dbpedia")
    //             .queryParam("uri", "http://fr.dbpedia.org/resource/Jimmy_Page")
    //             .request()
    //             .get(String.class);
    //     assertEquals(true, res.contains("Led Zeppelin"));
    // }

    // @Test
    // public void test4() {
    //     String service = "http://localhost:8080/tutorial/cdn";
    //     Client client = ClientBuilder.newClient();
    //     WebTarget target = client.target(service);
    //     String res = target.request().get(String.class);
    //     assertEquals(true, res.contains("Siècle"));
    // }

    // @Test
    // public void test5() {
    //     String service = "http://localhost:8080/process/owlrl";
    //     Client client = ClientBuilder.newClient();
    //     WebTarget target = client.target(service);
    //     String res = target.queryParam("uri", "/data/primer.owl").request().get(String.class);
    //     assertEquals(true, res.contains("Statement not supported in an Equivalent Class Expression"));
    // }
}
