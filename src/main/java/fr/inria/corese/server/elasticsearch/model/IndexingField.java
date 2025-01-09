package fr.inria.corese.server.elasticsearch.model;

import java.util.HashMap;
import java.util.Map;

public class IndexingField {

    private String label;
    private String datatype;
    private String path;
    private boolean multivalued = true;
    private boolean analyzed = true;
    private boolean optional = true;
    private String analyzer = null;
    private Integer ignoreAbove = null;
    private boolean filterDeleted = true;
    private Map<String, IndexingField> subfields;

    public IndexingField(String label, String datatype, String path) {
        this.label = label;
        this.datatype = datatype;
        this.path = path;
        this.subfields = new HashMap<>();
    }

    public void addSubfield(String fieldName, IndexingField field) {
        subfields.put(fieldName, field);
    }

    public String getLabel() {
        return label;
    }

    public String getDatatype() {
        return datatype;
    }

    public String getPath() {
        return path;
    }

    public boolean isMultivalued() {
        return multivalued;
    }

    public boolean isAnalyzed() {
        return analyzed;
    }

    public boolean isOptional() {
        return optional;
    }

    public String getAnalyzer() {
        return analyzer;
    }

    /**
     * @return null if not defined
     */
    public Integer getIgnoreAbove() {
        return ignoreAbove;
    }

    public boolean isFilterDeleted() {
        return filterDeleted;
    }

    public Map<String, IndexingField> getSubfields() {
        return subfields;
    }

    public void setMultivalued(boolean multivalued) {
        this.multivalued = multivalued;
    }

    public void setAnalyzed(boolean analyzed) {
        this.analyzed = analyzed;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public void setAnalyzer(String analyzer) {
        this.analyzer = analyzer;
    }

    public void setIgnoreAbove(Integer ignoreAbove) {
        this.ignoreAbove = ignoreAbove;
    }

    public void setFilterDeleted(boolean filterDeleted) {
        this.filterDeleted = filterDeleted;
    }

    public IndexingField getSubfield(String subfieldLabel) {
        return subfields.get(subfieldLabel);
    }

    public boolean hasSubfields() {
        return !subfields.isEmpty();
    }

    public String getQueryStatement(String uri) {
        StringBuilder sb = new StringBuilder();

        if(isOptional()) {
            sb.append("OPTIONAL {\n");
        }

        if(uri.startsWith("?")) {
            sb.append("    ").append(uri).append(" ").append(getPath()).append(" ?").append(getLabel()).append(" .\n");
        } else {
            sb.append("    ").append(uri).append(" ").append(getPath()).append(" ?").append(getLabel()).append(" .\n");
        }

        for(Map.Entry<String, IndexingField> subfieldEntry : subfields.entrySet()) {
            sb.append("    ").append(subfieldEntry.getValue().getQueryStatement("?"+getLabel()));
        }

        if(isFilterDeleted()) {
            sb.append("    FILTER NOT EXISTS { ?").append(getLabel()).append(" mnx:hasDeletion/rdf:type mnx:Deletion }\n");
        }

        if(isOptional()) {
            sb.append("}\n");
        }

        return sb.toString();
    }
}
