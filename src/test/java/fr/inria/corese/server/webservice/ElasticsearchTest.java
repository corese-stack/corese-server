package fr.inria.corese.server.webservice;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.elasticsearch.EdgeChangeListener;
import fr.inria.corese.core.elasticsearch.ElasticsearchVisitor;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ElasticsearchTest {

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

    @Test
    public void insertListenerTest() throws EngineException {
        Graph testGraph = Graph.create();
        TestEdgeChangeListener listener = new TestEdgeChangeListener();
        testGraph.addEdgeChangeListener(listener); // FIXME the edges sent using this method have no properties
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
        testGraph.addEdgeChangeListener(listener); // FIXME the edges sent using this method have no properties
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
