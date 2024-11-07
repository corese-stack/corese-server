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
        public List<Node> subjectChanged = new ArrayList<>();
        public List<Node> propertyChanged = new ArrayList<>();
        public List<Node> objectChanged = new ArrayList<>();

        @Override
        public void onBulkEdgeChange(List<Edge> delete, List<Edge> insert) {
            System.out.println("Bulk edge change: " + delete + " " + insert);
            nbTripleInserted += insert.size();
            nbTripleDeleted += delete.size();
            for (Edge edge : delete) {
                subjectChanged.add(edge.getSubjectNode());
                propertyChanged.add(edge.getPropertyNode());
                objectChanged.add(edge.getObjectNode());
            }
            for (Edge edge : insert) {
                subjectChanged.add(edge.getSubjectNode());
                propertyChanged.add(edge.getPropertyNode());
                objectChanged.add(edge.getObjectNode());
            }
        }
    }

    @Test
    public void insertListenerTest() throws EngineException {
        Graph testGraph = Graph.create();
        TestEdgeChangeListener listener = new TestEdgeChangeListener();
        testGraph.addEdgeChangeListener(listener);
        QueryProcess queryProc = QueryProcess.create(testGraph);
        queryProc.query("INSERT DATA { <http://example.com/subject> <http://example.com/property> <http://example.com/object> }");

        assertTrue(listener.subjectChanged.stream().anyMatch(node -> node.getLabel().equals("http://example.com/subject")));
        assertTrue(listener.propertyChanged.stream().anyMatch(node -> node.getLabel().equals("http://example.com/property")));
        assertTrue(listener.objectChanged.stream().anyMatch(node -> node.getLabel().equals("http://example.com/object")));
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
        queryProc.query("DELETE DATA { <http://example.com/subject> <http://example.com/property> <http://example.com/object> }");

        assertEquals(1, listener.nbTripleDeleted);
        assertEquals(2, listener.nbTripleInserted);
        assertTrue(listener.subjectChanged.stream().anyMatch(node -> node.getLabel().equals("http://example.com/subject")));
        assertTrue(listener.propertyChanged.stream().anyMatch(node -> node.getLabel().equals("http://example.com/property")));
        assertTrue(listener.objectChanged.stream().anyMatch(node -> node.getLabel().equals("http://example.com/object")));
    }

    @Test
    public void loadListenerTest() throws LoadException {
        Graph testGraph = Graph.create();
        TestEdgeChangeListener listener = new TestEdgeChangeListener();
        testGraph.addEdgeChangeListener(listener);
        Load fileLoader = Load.create(testGraph);
        fileLoader.parse("src/test/resources/elasticsearch/data.ttl");

        System.out.println("Subject changed: " + listener.subjectChanged);
        System.out.println("Property changed: " + listener.propertyChanged);
        System.out.println("Object changed: " + listener.objectChanged);

        assertEquals(0, listener.nbTripleDeleted);
        assertEquals(2, listener.nbTripleInserted);
        assertTrue(listener.subjectChanged.stream().anyMatch(node -> node.getLabel().equals("http://example.com/subject")));
        assertTrue(listener.propertyChanged.stream().anyMatch(node -> node.getLabel().equals("http://example.com/property")));
        assertTrue(listener.objectChanged.stream().anyMatch(node -> node.getLabel().equals("object1")));
        assertTrue(listener.objectChanged.stream().anyMatch(node -> node.getLabel().equals("http://example.com/object2")));
    }
}
