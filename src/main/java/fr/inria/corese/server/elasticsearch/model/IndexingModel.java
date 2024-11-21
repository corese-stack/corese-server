package fr.inria.corese.server.elasticsearch.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexingModel {
    private static final Logger logger = LoggerFactory.getLogger(IndexingModel.class);

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

    /**
     * Contains the instance uri in the variable "?instance" along with all the fields of the instance
     * @param instanceUri the uri of the instance to retrieve
     * @return a SPARQL SELECT query to retrieve the instance with the "?instance" variable
     */
    public String generateInstanceQuery(String instanceUri) {
        logger.info("Generating instance query for class {} and instance {} with {} prefixes and {} fields", classUri, instanceUri, prefixes.size(), fields.size());
        StringBuilder sb = new StringBuilder();

        for(Map.Entry<String, String> prefixEntry : prefixes.entrySet()) {
            sb.append("PREFIX ").append(prefixEntry.getKey()).append(": <").append(prefixEntry.getValue()).append(">\n");
        }

        sb.append("SELECT * WHERE {\n");
        sb.append("    FILTER(?instance = <").append(instanceUri).append(">)\n");
        sb.append("    ?instance a <").append(classUri).append("> .\n");
        for(IndexingField field : fields.values()) {
            sb.append(field.getQueryStatement(instanceUri)).append("\n");
        }

        sb.append("}\n");

        return sb.toString();
    }

    /**
     *
     * @return a SPARQL SELECT query to retrieve all instances of the class with the "?instance" variable
     */
    public String generateInstanceListQuery() {
        StringBuilder sb = new StringBuilder();

        for(Map.Entry<String, String> prefixEntry : prefixes.entrySet()) {
            sb.append("PREFIX ").append(prefixEntry.getKey()).append(": <").append(prefixEntry.getValue()).append(">\n");
        }
        sb.append("SELECT ?instance\n");
        sb.append("WHERE {\n");
        sb.append("    ?instance a <").append(classUri).append("> .\n");
        sb.append("}\n");

        return sb.toString();
    }

    public void refresh() {
        // Refresh the model map according to the declaration in the server
    }

    public IndexingField getField(String fieldLabel) {
        return fields.get(fieldLabel);
    }
}
