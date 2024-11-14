package fr.inria.corese.server.webservice;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.elasticsearch.EdgeChangeListener;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class ElasticsearchTest {

    private static Process server;

    private static class TestEdgeChangeListener extends EdgeChangeListener {
        public int nbTripleInserted = 0;
        public int nbTripleDeleted = 0;
        public List<Edge> edgesDeleted = new ArrayList<>();
        public List<Edge> edgesInserted = new ArrayList<>();

        @Override
        public void onBulkEdgeChange(List<Edge> delete, List<Edge> insert) {
            nbTripleInserted += insert.size();
            nbTripleDeleted += delete.size();
            edgesDeleted.addAll(delete);
            edgesInserted.addAll(insert);
            System.out.println("Bulk edge change: " + delete + " " + insert);
        }
    }

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

        System.out.println("starting in " + System.getProperty("user.dir"));
        server = new ProcessBuilder().inheritIO().command(
                "java",
//                "-javaagent:" + jacocoAgentPath + "=destfile=" + startDirectory+"/jacoco/server_sparql_ep_query.exec,includes=fr.inria.corese.*",
                "-jar", jar_file.getAbsolutePath(),
                "-lh",
                "-pp", "src/test/resources/emptyProfile.ttl",
                "-l", turtleFileAbsolutePath,
                "-l", trigFileAbsolutePath).start();
        Thread.sleep(7000);
    }

    @AfterClass
    public static void shutdown() {
        server.destroy();
    }

    @Test
    public void insertListenerTest() throws EngineException {
        Graph testGraph = Graph.create();
        TestEdgeChangeListener listener = new TestEdgeChangeListener();
        testGraph.addEdgeChangeListener(listener);
        QueryProcess queryProc = QueryProcess.create(testGraph);
        queryProc.query("INSERT DATA { <http://example.com/subject> <http://example.com/property> <http://example.com/object> }");

        assertTrue(listener.edgesInserted.stream().anyMatch(edge -> edge.getSubjectNode().getLabel().equals("http://example.com/subject") && edge.getPropertyNode().getLabel().equals("http://example.com/property") && edge.getObjectNode().getLabel().equals("http://example.com/object")));
        assertEquals(0, listener.nbTripleDeleted);
        assertEquals(1, listener.nbTripleInserted);
    }

    @Test
    public void deleteListenerTest() throws EngineException {
        Graph testGraph = Graph.create();
        TestEdgeChangeListener listener = new TestEdgeChangeListener();
        testGraph.addEdgeChangeListener(listener);
        QueryProcess queryProc = QueryProcess.create(testGraph);
        queryProc.query("INSERT DATA { <http://example.com/subject1> <http://example.com/property1> <http://example.com/object1> . <http://example.com/subject2> <http://example.com/property2> <http://example.com/object2> }");
        assertEquals(2, listener.nbTripleInserted);
        queryProc.query("DELETE DATA { <http://example.com/subject> <http://example.com/property> <http://example.com/object> }");
        assertEquals(0, listener.nbTripleDeleted);
        queryProc.query("DELETE DATA { <http://example.com/subject1> <http://example.com/property1> <http://example.com/object1> }");
        assertEquals(1, listener.nbTripleDeleted);
        assertFalse(listener.edgesDeleted.stream().anyMatch(edge -> edge.getSubjectNode().getLabel().equals("http://example.com/subject") && edge.getPropertyNode().getLabel().equals("http://example.com/property") && edge.getObjectNode().getLabel().equals("http://example.com/object")));
        assertTrue(listener.edgesDeleted.stream().anyMatch(edge -> edge.getSubjectNode().getLabel().equals("http://example.com/subject1") && edge.getPropertyNode().getLabel().equals("http://example.com/property1") && edge.getObjectNode().getLabel().equals("http://example.com/object1")));
    }

    @Test
    public void fileLoadListenerTest() throws LoadException {
        Graph testGraph = Graph.create();
        TestEdgeChangeListener listener = new TestEdgeChangeListener();
        testGraph.addEdgeChangeListener(listener);
        Load fileLoader = Load.create(testGraph);
        fileLoader.parse("src/test/resources/elasticsearch/data.ttl");

        assertEquals(0, listener.nbTripleDeleted);
        assertEquals(2, listener.nbTripleInserted);
        assertTrue(listener.edgesInserted.stream().anyMatch(edge -> edge.getSubjectNode().getLabel().equals("http://example.com/subject") && edge.getPropertyNode().getLabel().equals("http://example.com/property") && edge.getObjectNode().getLabel().equals("http://example.com/object2")));
        assertTrue(listener.edgesInserted.stream().anyMatch(edge -> edge.getSubjectNode().getLabel().equals("http://example.com/subject") && edge.getPropertyNode().getLabel().equals("http://example.com/property") && edge.getObjectNode().getLabel().equals("object1")));
    }

    @Test
    public void loadListenerTest() throws LoadException, EngineException {
        Graph testGraph = Graph.create();
        TestEdgeChangeListener listener = new TestEdgeChangeListener();
        testGraph.addEdgeChangeListener(listener);

        QueryProcess queryProc = QueryProcess.create(testGraph);
        queryProc.query("LOAD <src/test/resources/elasticsearch/data.ttl>");

        assertEquals(0, listener.nbTripleDeleted);
        assertEquals(2, listener.nbTripleInserted);
        assertTrue(listener.edgesInserted.stream().anyMatch(edge -> edge.getSubjectNode().getLabel().equals("http://example.com/subject") && edge.getPropertyNode().getLabel().equals("http://example.com/property") && edge.getObjectNode().getLabel().equals("http://example.com/object2")));
        assertTrue(listener.edgesInserted.stream().anyMatch(edge -> edge.getSubjectNode().getLabel().equals("http://example.com/subject") && edge.getPropertyNode().getLabel().equals("http://example.com/property") && edge.getObjectNode().getLabel().equals("object1")));
    }
}
