package fr.inria.corese.server.elasticsearch.model;

import java.util.HashMap;
import java.util.Map;

public class IndexingModel {

    private String classUri;
    private Map<String, String> prefixes;
    private Map<String, IndexingField> fields;

    public IndexingModel(String classUri) {
        this.classUri = classUri;
        this.prefixes = new HashMap<>();
        this.fields = new HashMap<>();
    }

    public void addPrefix(String prefix, String uri) {
        prefixes.put(prefix, uri);
    }

    public void addField(String fieldName, IndexingField field) {
        fields.put(fieldName, field);
    }

    public String getClassUri() {
        return classUri;
    }

    public Map<String, String> getPrefixes() {
        return prefixes;
    }

    public Map<String, IndexingField> getFields() {
        return fields;
    }

    private String generateInstanceQuery() {
        StringBuilder sb = new StringBuilder();

        return sb.toString();
    }

    public void refresh() {
        // Refresh the model according to the declaration in the server
    }

    public IndexingField getField(String fieldLabel) {
        return fields.get(fieldLabel);
    }
}
