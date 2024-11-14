package fr.inria.corese.server.elasticsearch.model;

public class Ontology {

    enum objectProperty implements Property {
        FIELD("field"),
        INDEXINGMODELOF("indexingModelOf"),
        PREFIX("prefix"),
        SUBFIELD("subfield");

        private String uri;

        objectProperty(String uri) {
            this.uri = uri;
        }

        public String uri() {
            return uri;
        }
    }

    enum datatypeProperty implements Property {
        ANALYZED("analyzed"),
        ANALYZER("analyzer"),
        DATAPATH("dataPath"),
        FILTERDELETED("filterDeleted"),
        IGNOREABOVE("ignore_above"),
        MULTIVALUED("multivalued"),
        OPTIONAL("optional"),
        VALUE("value");

        private String uri;

        datatypeProperty(String uri) {
            this.uri = uri;
        }

        public String uri() {
            return uri;
        }
    }

    interface Property {
        String uri();
    }
}
