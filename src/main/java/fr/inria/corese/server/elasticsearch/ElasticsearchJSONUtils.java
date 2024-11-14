package fr.inria.corese.server.elasticsearch;

import fr.inria.corese.core.kgram.api.core.Edge;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class ElasticsearchJSONUtils {

    private ElasticsearchJSONUtils() {
    }

    private static final String SUBJECT = "subject";
    private static final String PREDICATE = "predicate";
    private static final String OBJECT = "object";
    private static final String DELETE = "delete";
    private static final String INSERT = "insert";

    public static JSONObject toJSONObject(Edge edge) {
            JSONObject json = new JSONObject();
        json.put(SUBJECT, edge.getSubjectNode().getLabel());
        json.put(PREDICATE, edge.getProperty().getLabel());
        json.put(OBJECT, edge.getObjectNode().getLabel());
        return json;
    }

    public static JSONArray toJSONArray(List<Edge> edgeList) {
        JSONArray json = new JSONArray();
        for (Edge edge : edgeList) {
            json.put(toJSONObject(edge));
        }
        return json;
    }

    public static JSONObject toJSONObject(List<Edge> delete, List<Edge> add) {
        JSONObject json = new JSONObject();
        json.put(DELETE, toJSONArray(delete));
        json.put(INSERT, toJSONArray(add));
        return json;
    }
}
