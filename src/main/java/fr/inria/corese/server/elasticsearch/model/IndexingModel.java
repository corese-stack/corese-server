package fr.inria.corese.server.elasticsearch.model;

import fr.inria.corese.core.kgram.api.core.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IndexingModel {
    private static final Logger logger = LoggerFactory.getLogger(IndexingModel.class);

    private final String classUri;
    private String classLabel;
    private Map<String, String> prefixes;
    private Map<String, IndexingField> fields;
    private Set<String> fieldUris;

    public IndexingModel(String classUri) {
        this.classUri = classUri;
        this.prefixes = new HashMap<>();
        this.fields = new HashMap<>();
        this.fieldUris = new HashSet<>();
    }

    public void addPrefix(String prefix, String uri) {
        prefixes.put(prefix, uri);
        this.fieldUris = getFieldProperties();
    }

    public void addField(String fieldName, IndexingField field) {
        fields.put(fieldName, field);
        this.fieldUris = getFieldProperties();
    }

    public String getClassUri() {
        return classUri;
    }

    public String getClassLabel() {
        return classLabel;
    }

    public void setClassLabel(String classLabel) {
        this.classLabel = classLabel;
    }

    /**
     * Returns the name of the index to use in Elasticsearch
     * The index is supposed to be a normalized version in lowercase and with all forbidden characters removed of the class label
     * @return The end of the class URI
     */
    public String getIndexName() {
        String indexName = Normalizer.normalize(getClassLabel(), Normalizer.Form.NFKD)
                .toLowerCase()
                .strip()
                .replaceAll("[\\\\\\/\\*\\?\\\"\\<\\>\\| \\#']", "") // Cannot include \, /, *, ?, ", <, >, |, ` ` (space character), ,, #
                .replaceAll("^[\\-_\\+]+", ""); // Cannot start with -, _, +
        if(indexName.compareTo(".") == 0 || indexName.compareTo("..") == 0) { // Cannot be . or ..
            logger.error("The index name \".\" or \"..\" are illegal for class URI " + classUri);
            throw new IllegalArgumentException("The index name \".\" or \"..\" are illegal for class URI " + classUri);
        }
        return indexName;
    }

    public Map<String, String> getPrefixes() {
        return prefixes;
    }

    public Map<String, IndexingField> getFields() {
        return fields;
    }

    /**
     * Contains the instance uri in the variable "?instance" along with all the fields of the instance
     * @param instanceNode the node of the instance to retrieve
     * @return a SPARQL SELECT query to retrieve the instance with the "?instance" variable
     */
    public String generateInstanceDescriptionQuery(Node instanceNode) {
        StringBuilder sb = new StringBuilder();

        String instanceString = instanceNode.getDatatypeValue().toSparql();

        for(Map.Entry<String, String> prefixEntry : prefixes.entrySet()) {
            sb.append("PREFIX ").append(prefixEntry.getKey()).append(": <").append(prefixEntry.getValue()).append(">\n");
        }

        sb.append("SELECT DISTINCT * WHERE {\n");
        sb.append("    FILTER(?instance = ").append(instanceString).append(")\n");
        sb.append("    ?instance a <").append(classUri).append("> .\n");
        for(IndexingField field : fields.values()) {
            sb.append(field.getQueryStatement(instanceNode)).append("\n");
        }

        sb.append("}\n");

        return sb.toString();
    }

    /**
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
        sb.append("    FILTER(IsIRI(?instance)) .\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generate an ASK query that checks in the non-optional fields of an instance are present.
     */
    public String generateCheckInstanceIsCompleteQuery(Node instanceNode) {
        StringBuilder sb = new StringBuilder();

        for(Map.Entry<String, String> prefixEntry : prefixes.entrySet()) {
            sb.append("PREFIX ").append(prefixEntry.getKey()).append(": <").append(prefixEntry.getValue()).append(">\n");
        }

        sb.append("ASK {\n");
        sb.append("    ?instance a <").append(classUri).append("> .\n");
        for(IndexingField field : fields.values()) {
            if(!field.isOptional()) {
                sb.append(field.getQueryStatement("?instance")).append("\n");
            }
        }
        sb.append("FILTER(?instance = ").append(instanceNode.getDatatypeValue().toSparql()).append(")\n");
        sb.append("}\n");

        return sb.toString();
    }

    public IndexingField getField(String fieldLabel) {
        return fields.get(fieldLabel);
    }

    /**
     * Does the application of the prefixes to recover the actual URIs behind each field path
     */
    private Set<String> getFieldProperties() {
        Set<String> result = new HashSet<>();

        this.fields.values().forEach(field -> {
            String fieldPath = field.getPath();
            this.getPrefixes().forEach((prefix, uri) -> {
                if(fieldPath.startsWith(prefix + ":")) {
                    result.add(uri + fieldPath.substring(prefix.length() + 1));
                }
            });
        });

        return result;
    }

    public boolean usesProperty(String propertyUri) {
        boolean result = this.fieldUris.stream().anyMatch(uri -> uri.equals(propertyUri));
        for(Map.Entry<String, IndexingField> fieldEntry : this.getFields().entrySet()) {
            if(fieldEntry.getValue().hasSubfields()) {
                result = result || fieldEntry.getValue().getSubfields().values().stream().anyMatch(uri -> uri.equals(propertyUri));
            }
        }
        return result;
    }
}
