package fr.inria.corese.server.elasticsearch;

import fr.inria.corese.core.kgram.api.core.Edge;
import org.json.JSONObject;

import java.util.List;

public class ElasticsearchJSONUtils {

    private ElasticsearchJSONUtils() {
    }

    private static final String SUBJECT = "subject";
    private static final String PREDICATE = "predicate";
    private static final String OBJECT = "object";
    private static final String DELETE = "delete";
    private static final String ADD = "add";

    public static JSONObject toJSONObject(Edge edge) {
            JSONObject json = new JSONObject();
        json.put(SUBJECT, edge.getSubjectNode().getLabel());
        json.put(PREDICATE, edge.getEdgeNode().getLabel());
        json.put(OBJECT, edge.getObjectNode().getLabel());
        return json;
    }

    public static JSONObject toJSONObject(List<Edge> edgeList) {
        JSONObject json = new JSONObject();
        for (Edge edge : edgeList) {
            json.put(edge.getEdgeNode().getLabel(), toJSONObject(edge));
        }
        return json;
    }

    public static JSONObject toJSONObject(List<Edge> delete, List<Edge> add) {
        JSONObject json = new JSONObject();
        json.put(DELETE, toJSONObject(delete));
        json.put(ADD, toJSONObject(add));
        return json;
    }
}
