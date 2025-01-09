package fr.inria.corese.server.elasticsearch.model;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.server.webservice.endpoint.SPARQLRestAPI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * In charge of managing the mapping of instances of Model to Elasticsearch JSON objects.
 */
public class ESMappingManager {
    private static final Logger logger = LoggerFactory.getLogger(ESMappingManager.class);

    private static ESMappingManager instance = null;
    private Map<String, Set<Node>> classInstances;

    /**
     * Inverse dependency of mapped instances to sub-resources because of a subfield, i.e a document is linked to its author to get their first and last name
     * The key is the sub-resource and the value is the set of resources that depend on it.
     */
    private Map<Node, Set<Node>> inverseInstanceDependencies;

    private ESMappingManager() {
        classInstances = new HashMap<>();
        inverseInstanceDependencies = new HashMap<>();
    }

    public static ESMappingManager getInstance() {
        if (instance == null) {
            instance = new ESMappingManager();
        }
        return instance;
    }

    /**
     * generates a SPARQL query to retrieve the types of an instance
     *
     * @param instanceNode must be an IRI
     * @return the SPARQL query with a "?type" variable
     */
    private static String generateInstanceTypeQuery(Node instanceNode) {
        return "SELECT ?type WHERE { " + instanceNode.getDatatypeValue().toSparql() + " a ?type . FILTER(IsIRI(?type)) }";
    }

    /**
     * Retrieves the models corresponding to the types of an instance
     *
     * @param instanceNode The URI of the instance to retrieve the models for, must be an IRI.
     * @return The models of the instance.
     */
    private static Collection<IndexingModel> getModelsOfInstance(Node instanceNode) {
        HashSet<IndexingModel> models = new HashSet<>();
        try {
            Mappings typeMappings = SPARQLRestAPI.getQueryProcess().query(generateInstanceTypeQuery(instanceNode));
            if (!typeMappings.isEmpty()) {
                for (Mapping m : typeMappings) {
                    IndexingModel model = IndexingManager.getInstance().getModel(m.getValue("?type").stringValue());
                    if (model != null) {
                        models.add(model);
                    }
                }
            }
        } catch (EngineException e) {
            logger.error("Error while retrieving type of instance {}", instanceNode, e);
        }
        return models;
    }

    /**
     * Replace all special characters in a URI to generate a valid Elasticsearch document ID.
     */
    private static String generateDocIdFromUri(String uri) throws UnsupportedEncodingException {
        String replacedURI = uri.replaceAll("<", "").replaceAll(">", "").replaceAll(":", "").replaceAll("/", "").replaceAll("#", "").replaceAll("\\.", "").replaceAll("\\?", "").replaceAll("&", "").replaceAll("=", "").replaceAll(";", "").replaceAll(",", "").replaceAll("\\+", "").replaceAll("\\*", "").replaceAll("\\(", "").replaceAll("\\)", "").replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("\\|", "").replaceAll("\"", "").replaceAll("'", "").replaceAll("`", "").replaceAll(" ", "_");
        return URLEncoder.encode(replacedURI, StandardCharsets.UTF_8.toString());
    }

    public void addClassInstanceUri(String classUri, Node instance) {
        if (!classInstances.containsKey(classUri)) {
            classInstances.put(classUri, new HashSet<>());
        }
        classInstances.get(classUri).add(instance);
    }

    public Set<Node> getClassInstancesUris(String classUri) {
        return classInstances.get(classUri);
    }

    public boolean isInstanceOfModelClass(IndexingModel model, Node node) {
        return classInstances.containsKey(model.getClassUri()) && classInstances.get(model.getClassUri()).contains(node);
    }

    public boolean isInstanceOfAModelClass(Node node) {
        return classInstances.values().stream().anyMatch(s -> s.contains(node));
    }

    public void addInverseDependency(Node subResource, Node resource) {
        if (inverseInstanceDependencies == null) {
            inverseInstanceDependencies = new HashMap<>();
        }
        if (!inverseInstanceDependencies.containsKey(subResource)) {
            inverseInstanceDependencies.put(subResource, new HashSet<>());
        }
        if (
                (subResource.getDatatypeValue().isBlank()
                        || subResource.getDatatypeValue().isURI())
                        && (resource.getDatatypeValue().isBlank()
                        || resource.getDatatypeValue().isURI())) {
            logger.debug("Adding inverse dependency from {} to {}", subResource.getDatatypeValue().toSparql(), resource.getDatatypeValue().toSparql());
            inverseInstanceDependencies.get(subResource).add(resource);
        }
    }

    public void addInverseDependencies(Node subResource, Set<Node> resources) {
        if (inverseInstanceDependencies == null) {
            inverseInstanceDependencies = new HashMap<>();
        }
        if (!inverseInstanceDependencies.containsKey(subResource)) {
            inverseInstanceDependencies.put(subResource, new HashSet<>());
        }
        inverseInstanceDependencies.get(subResource).addAll(resources);
    }

    public Set<Node> getInverseDependencies(Node subResource) {
        return inverseInstanceDependencies.get(subResource);
    }

    public void removeInverseDependencies(Node subResource) {
        inverseInstanceDependencies.remove(subResource);
    }

    public void removeInverseDependency(Node subResource, Node resource) {
        if (inverseInstanceDependencies.containsKey(subResource)) {
            inverseInstanceDependencies.get(subResource).remove(resource);
        }
    }

    public void removeInverseDependency(Node subResource, Set<Node> resources) {
        if (inverseInstanceDependencies.containsKey(subResource)) {
            inverseInstanceDependencies.get(subResource).removeAll(resources);
        }
    }

    public boolean hasInverseDependencies(Node subResource) {
        return inverseInstanceDependencies.containsKey(subResource);
    }

    public void clearInverseDependencies() {
        inverseInstanceDependencies.clear();
    }

    public void clearClassInstancesURIs() {
        classInstances.clear();
    }

    /**
     * Retrieves all the mappings for all models.
     *
     * @return The mappings in a JSON array.
     */
    public Map<String, JSONArray> getAllMappings() {
        Map<String, JSONArray> mappings = new HashMap<>();
        for (IndexingModel model : IndexingManager.getInstance().getModels()) {
            mappings.put(model.getIndexName(), retrieveModelMappings(model));
        }
        return mappings;
    }

    /**
     * Retrieves the mappings for a given class URI.
     *
     * @param classUri The URI of the class to retrieve the mapping for.
     * @return The mappings in a JSON array.
     */
    public JSONArray getMappings(String classUri) {
        IndexingModel model = IndexingManager.getInstance().getModel(classUri);
        return retrieveModelMappings(model);
    }

    /**
     * Retrieve the mappings of the instances corresponding to a given model
     *
     * @return
     */
    private JSONArray retrieveModelMappings(IndexingModel model) {
        JSONArray mappings = new JSONArray();

        ArrayList<Node> instanceList = new ArrayList<>();
        try {
            Mappings instancesMappings = SPARQLRestAPI.getQueryProcess().query(model.generateInstanceListQuery());
            for (Mapping m : instancesMappings) {
                instanceList.add(m.getValue("?instance"));
                addClassInstanceUri(model.getClassUri(), m.getValue("?instance"));
            }
        } catch (EngineException e) {
            logger.error("Error while retrieving instances of class {} for mapping", model.getClassUri(), e);
        }

        for (Node instanceNode : instanceList) {
            JSONArray instanceMappings = retrieveIndividualMapping(instanceNode, model);
            if (instanceMappings.length() > 0) {
                mappings.putAll(instanceMappings);
            }
        }

        return mappings;
    }

    /**
     * Retrieves the mappings for a given individual.
     * Will return an empty map if there are no model for this individual
     *
     * @param individualNode The URI of the individual to retrieve the mapping for.
     * @return The mappings for each model in a JSON array, if several models correspond to the individual
     */
    public Map<String, JSONArray> retrieveIndividualMapping(Node individualNode) {
        logger.debug("Retrieving mappings for individual {}", individualNode.getDatatypeValue().toSparql());
        Collection<IndexingModel> models = getModelsOfInstance(individualNode);
        HashMap<String, JSONArray> instanceMappings = new HashMap<>();
        for (IndexingModel model : models) {
            instanceMappings.put(model.getIndexName(), retrieveIndividualMapping(individualNode, model));
        }
        return instanceMappings;
    }

    /**
     * Retrieves the mappings for an individual for a given model.
     *
     * @param individualNode The URI of the individual to retrieve the mapping for.
     * @return The mappings in a JSON array.
     */
    public JSONArray retrieveIndividualMapping(Node individualNode, IndexingModel model) {
        // Building dependencies graph
        try {
            Mappings dependenciesMappings = SPARQLRestAPI.getQueryProcess().query(generateDependenciesQuery(individualNode, model));
            for (Mapping m : dependenciesMappings) {
                addInverseDependency(m.getNode("?subResource"), m.getNode("?resource"));
            }
        } catch (EngineException e) {
            logger.error("Error while retrieving dependencies for individual {} for mapping", individualNode, e);
        }

        // retrieving individual mappings
        JSONArray instanceMappings = new JSONArray();
        String instanceQuery = model.generateInstanceDescriptionQuery(individualNode);
        try {
            Mappings instanceFieldQueryMappings = SPARQLRestAPI.getQueryProcess().query(instanceQuery);
            logger.debug("Mappings for instance {} : {}", individualNode, instanceFieldQueryMappings.size());
            JSONObject instanceJSON = jsonFromFieldList(model.getFields().values(), instanceFieldQueryMappings);
            instanceJSON.put("uri", generateDocIdFromUri(individualNode.getDatatypeValue().toSparql()));
            instanceMappings.put(instanceJSON);
        } catch (EngineException | UnsupportedEncodingException e) {
            logger.error("Error while retrieving instance {} for mapping using {}", individualNode, instanceQuery, e);
        }
        addClassInstanceUri(model.getClassUri(), individualNode);
        return instanceMappings;
    }

    private String generateDependenciesQuery(Node individualNode, IndexingModel model) {
        StringBuilder sb = new StringBuilder();

        model.getPrefixes().forEach((prefix, uri) -> sb.append("PREFIX ").append(prefix).append(": <").append(uri).append(">\n"));
        sb.append("SELECT DISTINCT ?subResource ?resource WHERE {\n");
        sb.append("    ?resource ?p ?subResource .\n");
        sb.append("    VALUES ?p { ");
        for (IndexingField field : model.getFields().values()) {
            sb.append(" ").append(field.getPath()).append(" ");
        }
        sb.append("    }\n");
        sb.append("FILTER(?resource = ").append(individualNode.getDatatypeValue().toSparql()).append(")\n");
        sb.append("    FILTER(isIRI(?subResource) || isBlank(?subResource))\n");

        sb.append("}\n");


        return sb.toString();
    }

    private JSONObject jsonFromFieldList(Collection<IndexingField> fieldList, Mappings instanceMappings) {
        JSONObject json = new JSONObject();

        for (IndexingField field : fieldList) {
            if (field.isMultivalued()) {
                JSONArray retrievedValue = jsonFieldValueFromMultivaluedField(field, instanceMappings);
                if (!retrievedValue.isEmpty()) {
                    try { // If the field already exists, we append the values to the existing array
                        JSONArray existingValues = json.getJSONArray(field.getLabel());
                        for (int i = 0; i < retrievedValue.length(); i++) {
                            existingValues.put(retrievedValue.get(i));
                        }
                        json.put(field.getLabel(), existingValues);
                    } catch (JSONException e) { // If the field does not exist, we create it
                        json.put(field.getLabel(), retrievedValue);
                    }
                }
            } else {
                String retrievedValue = jsonFieldValueFromFieldMappings(field, instanceMappings);
                if (retrievedValue != null) {
                    json.put(field.getLabel(), retrievedValue);
                }
            }
        }

        return json;
    }

    private JSONObject jsonFromSubfieldList(Collection<IndexingField> fieldList, Mapping instanceMapping) {
        JSONObject json = new JSONObject();

        for (IndexingField field : fieldList) {
            Node subfieldNode = instanceMapping.getNode("?" + field.getLabel());
            Node instanceNode = instanceMapping.getNode("?instance");
            addInverseDependency(subfieldNode, instanceNode);
            String retrievedValue = jsonFieldValueFromFieldMapping(field, instanceMapping);
            if (retrievedValue != null) {
                json.put(field.getLabel(), retrievedValue);
            }
        }

        return json;
    }

    private String jsonFieldValueFromFieldMappings(IndexingField field, Mappings instanceMappings) {
        if (field.isMultivalued()) {
            throw new IllegalArgumentException("Field " + field.getLabel() + " is multivalued, use jsonFieldValueFromMultivaluedField instead");
        }
        if (!instanceMappings.isEmpty()
                && instanceMappings.get(0).getValue("?" + field.getLabel()) != null) {
            if (field.hasSubfields()) {
                instanceMappings.forEach(mapping -> {
                    Node subfieldNode = mapping.getNode("?" + field.getLabel());
                    Node instanceNode = mapping.getNode("?instance");
                    addInverseDependency(subfieldNode, instanceNode);
                });
                return jsonFromFieldList(field.getSubfields().values(), instanceMappings).toString();
            } else {
                return instanceMappings.get(0).getValue("?" + field.getLabel()).stringValue();
            }
        }
        return null;
    }

    private String jsonFieldValueFromFieldMapping(IndexingField field, Mapping instanceMapping) {
        if (field.isMultivalued()) {
            throw new IllegalArgumentException("Field " + field.getLabel() + " is multivalued, use jsonFieldValueFromMultivaluedField instead");
        }
        if (instanceMapping.getValue("?" + field.getLabel()) != null) {
            return instanceMapping.getValue("?" + field.getLabel()).stringValue();
        }
        return null;
    }

    private JSONArray jsonFieldValueFromMultivaluedField(IndexingField field, Mappings instanceMappings) {
        JSONArray json = new JSONArray();
        HashSet<String> jsonObjectValues = new HashSet<>();
        HashSet<String> stringValues = new HashSet<>();
        for (Mapping m : instanceMappings) {
            if (m.getValue("?" + field.getLabel()) != null) {
                if (field.hasSubfields()) { // if the field has subfields, we need to retrieve the subfields as JSON Objects
                    JSONObject value = jsonFromSubfieldList(field.getSubfields().values(), m);
                    jsonObjectValues.add(value.toString());
                } else { // if the field has no subfields, we can retrieve the value as a string
                    String value = m.getValue("?" + field.getLabel()).stringValue();
                    if (value != null && !value.isEmpty()) {
                        stringValues.add(value);
                    }
                }
            }
        }

        ArrayList<JSONObject> jsonObjects = new ArrayList<>();
        jsonObjectValues.forEach(str -> jsonObjects.add(new JSONObject(str))); // We convert the set of JSON strings to a set of JSON objects because HashSet  allows duplicates JSONObjects
        json.putAll(jsonObjects);
        json.putAll(stringValues);
        return json;
    }
}
