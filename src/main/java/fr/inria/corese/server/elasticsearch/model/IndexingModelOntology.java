package fr.inria.corese.server.elasticsearch.model;

/**
 * Based on the ontology of the indexing model in https://gitlab.com/mnemotix/synaptix/mnx-models/-/blob/aa8134f95b1db258b1678aab1030e70e6763925f/indexing-model/indexing-model.owl
 *
 * @author Pierre Maillot
 * @since 2024-11-26
 */
public class IndexingModelOntology {

    public enum objectProperty implements Property {
        FIELD("field"),
        INDEXINGMODELOF("indexingModelOf"),
        PREFIX("prefix"),
        SUBFIELD("subfield");

        private String uri;

        objectProperty(String name) {
            this.uri = BASE_URI + name;
        }

        public String uri() {
            return uri;
        }
    }

    public enum datatypeProperty implements Property {
        ANALYZED("analyzed"),
        ANALYZER("analyzer"),
        DATAPATH("dataPath"),
        FILTERDELETED("filterDeleted"),
        IGNOREABOVE("ignore_above"),
        MULTIVALUED("multivalued"),
        OPTIONAL("optional"),
        VALUE("value");

        private String uri;

        datatypeProperty(String name) {
            this.uri = BASE_URI + name;
        }

        public String uri() {
            return uri;
        }
    }

    interface Property {

        String BASE_URI = "http://ns.mnemotix.com/ontologies/2019/1/indexing-model#";
        String uri();
    }

    public static boolean isObjectProperty(String uri) {
        for (objectProperty p : objectProperty.values()) {
            if (p.uri().equals(uri)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDatatypeProperty(String uri) {
        for (datatypeProperty p : datatypeProperty.values()) {
            if (p.uri().equals(uri)) {
                return true;
            }
        }
        return false;
    }

}