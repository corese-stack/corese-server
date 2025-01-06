package fr.inria.corese.server.elasticsearch.model;

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
import java.util.stream.Collectors;

/**
 * In charge of managing the mapping of instances of Model to Elasticsearch JSON objects.
 */
public class ESMappingManager {
    private static final Logger logger = LoggerFactory.getLogger(ESMappingManager.class);

    private static ESMappingManager instance = null;
    private Map<String, Set<String>> classInstancesURIs;

    private ESMappingManager() {
        classInstancesURIs = new HashMap<>();
    }

    public static ESMappingManager getInstance() {
        if (instance == null) {
            instance = new ESMappingManager();
        }
        return instance;
    }

    public void addClassInstanceUri(String classUri, String instanceUri) {
        if (!classInstancesURIs.containsKey(classUri)) {
            classInstancesURIs.put(classUri, new HashSet<>());
        }
        classInstancesURIs.get(classUri).add(instanceUri);
    }

    public Set<String> getClassInstancesUris(String classUri) {
        return classInstancesURIs.get(classUri);
    }

    /**
     * generates a SPARQL query to retrieve the types of an instance
     *
     * @param instanceUri must be an IRI
     * @return the SPARQL query with a "?type" variable
     */
    private static String generateInstanceTypeQuery(String instanceUri) {
        return "SELECT ?type WHERE { <" + instanceUri + "> a ?type . FILTER(IsIRI(?type)) }";
    }

    /**
     * Retrieves the models corresponding to the types of an instance
     *
     * @param instanceUri The URI of the instance to retrieve the models for, must be an IRI.
     * @return The models of the instance.
     */
    private static Collection<IndexingModel> getModelsOfInstance(String instanceUri) {
        HashSet<IndexingModel> models = new HashSet<>();
        try {
            Mappings typeMappings = SPARQLRestAPI.getQueryProcess().query(generateInstanceTypeQuery(instanceUri));
            if (!typeMappings.isEmpty()) {
                for (Mapping m : typeMappings) {
                    IndexingModel model = IndexingManager.getInstance().getModel(m.getValue("?type").stringValue());
                    if (model != null) {
                        models.add(model);
                    }
                }
            }
        } catch (EngineException e) {
            logger.error("Error while retrieving type of instance {}", instanceUri, e);
        }
        return models;
    }

    /**
     * Replace all special characters in a URI to generate a valid Elasticsearch document ID.
     */
    private static String generateDocIdFromUri(String uri) throws UnsupportedEncodingException {
        String replacedURI = uri.replaceAll(":", "").replaceAll("/", "").replaceAll("#", "").replaceAll("\\.", "").replaceAll("\\?", "").replaceAll("&", "").replaceAll("=", "").replaceAll(";", "").replaceAll(",", "").replaceAll("\\+", "").replaceAll("\\*", "").replaceAll("\\(", "").replaceAll("\\)", "").replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("\\|", "").replaceAll("\"", "").replaceAll("'", "").replaceAll("`", "").replaceAll(" ", "_");
        return URLEncoder.encode(replacedURI, StandardCharsets.UTF_8.toString());
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
     * @param model
     * @return
     */
    private JSONArray retrieveModelMappings(IndexingModel model) {
        JSONArray mappings = new JSONArray();

        ArrayList<String> instanceList = new ArrayList<>();
        try {
            Mappings instancesMappings = SPARQLRestAPI.getQueryProcess().query(model.generateInstanceListQuery());
            for (Mapping m : instancesMappings) {
                instanceList.add(m.getValue("?instance").stringValue());
            }
        } catch (EngineException e) {
            logger.error("Error while retrieving instances of class {} for mapping", model.getClassUri(), e);
        }

        for (String instanceUri : instanceList) {
            JSONArray instanceMappings = retrieveIndividualMapping(instanceUri, model);
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
     * @param individualUri The URI of the individual to retrieve the mapping for.
     * @return The mappings for each model in a JSON array, if several models correspond to the individual
     */
    public Map<String, JSONArray> retrieveIndividualMapping(String individualUri) {
        Collection<IndexingModel> models = getModelsOfInstance(individualUri);
        HashMap<String, JSONArray> instanceMappings = new HashMap<>();
        for (IndexingModel model : models) {
            instanceMappings.put(model.getIndexName(), retrieveIndividualMapping(individualUri, model));
        }
        return instanceMappings;
    }

    /**
     * Retrieves the mappings for an individual for a given model.
     *
     * @param individualUri The URI of the individual to retrieve the mapping for.
     * @return The mappings in a JSON array.
     */
    public JSONArray retrieveIndividualMapping(String individualUri, IndexingModel model) {
        JSONArray instanceMappings = new JSONArray();
        String instanceQuery = model.generateInstanceDescriptionQuery(individualUri);
        try {
            Mappings instanceFieldQueryMappings = SPARQLRestAPI.getQueryProcess().query(instanceQuery);
            logger.debug("Mappings for instance {} : {}", individualUri, instanceFieldQueryMappings.size());
            JSONObject instanceJSON = jsonFromFieldList(model.getFields().values(), instanceFieldQueryMappings);
            instanceJSON.put("uri", generateDocIdFromUri(individualUri));
            instanceMappings.put(instanceJSON);
        } catch (EngineException | UnsupportedEncodingException e) {
            logger.error("Error while retrieving instance {} for mapping using {}", individualUri, instanceQuery, e);
        }
        addClassInstanceUri(model.getClassUri(), individualUri);
        return instanceMappings;
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
