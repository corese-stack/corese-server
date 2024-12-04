package fr.inria.corese.server.elasticsearch.model;

import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.server.webservice.endpoint.SPARQLRestAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * In charge of managing the mapping of instances of Model to Elasticsearch JSON objects.
 */
public class ESMappingManager {
    private static final Logger logger = LoggerFactory.getLogger(ESMappingManager.class);

    private static ESMappingManager instance = null;

    private ESMappingManager() {
    }

    public static ESMappingManager getInstance() {
        if (instance == null) {
            instance = new ESMappingManager();
        }
        return instance;
    }

    /**
     * Retrieves all the mappings for all models.
     * @return The mappings in a JSON array.
     */
    public Map<String, JSONArray> getAllMappings() {
        Map<String, JSONArray> mappings = new HashMap<>();
        for(IndexingModel model : IndexingManager.getInstance().getModels()) {
            mappings.put(model.getIndexName(), retrieveModelMappings(model));
        }
        return mappings;
    }

    /**
     * Retrieves the mappings for a given class URI.
     * @param classUri The URI of the class to retrieve the mapping for.
     * @return The mappings in a JSON array.
     */
    public JSONArray getMappings(String classUri) {
        IndexingModel model = IndexingManager.getInstance().getModel(classUri);
        return retrieveModelMappings(model);
    }

    /**
     * Retrieve the mappings of the instances corresponding to a given model
     * @param model
     * @return
     */
    private JSONArray retrieveModelMappings(IndexingModel model) {
        JSONArray mappings = new JSONArray();

        ArrayList<String> instanceList = new ArrayList<>();
        try {
            Mappings instancesMappings = SPARQLRestAPI.getQueryProcess().query(model.generateInstanceListQuery());
            for(Mapping m : instancesMappings) {
                instanceList.add(m.getValue("?instance").stringValue());
            }
        } catch (EngineException e) {
            logger.error("Error while retrieving instances of class {} for mapping", model.getClassUri(), e);
        }

        for(String instanceUri : instanceList) {
            JSONArray instanceMappings = retrieveIndividualMapping(instanceUri, model);
            if(instanceMappings.length() > 0) {
                mappings.putAll(instanceMappings);
            }
        }

        return mappings;
    }

    /**
     * Retrieves the mappings for a given individual.
     * Will return an empty object if there are no model for this individual
     * @param individualUri The URI of the individual to retrieve the mapping for.
     * @return The mappings in a JSON array, if several models correspond to the individual, a mapping for each model is generated.
     */
    public Map<String, JSONObject> retrieveIndividualMapping(String individualUri) {
        Collection<IndexingModel> models = getModelsOfInstance(individualUri);
        HashMap<String, JSONObject> instanceMappings = new HashMap<>();
        for(IndexingModel model : models) {
            String instanceQuery = model.generateInstanceQuery(individualUri);
            logger.debug("Extraction info for instance: {} with {}", individualUri, instanceQuery);
            try {
                Mappings instanceFieldQueryMappings = SPARQLRestAPI.getQueryProcess().query(instanceQuery);
                JSONObject instanceJSON = jsonFromFieldList(model.getFields().values(), instanceFieldQueryMappings);
                instanceMappings.put(model.getIndexName(), instanceJSON);
            } catch (EngineException e) {
                logger.error("Error while retrieving instance {} for mapping using {}", individualUri, instanceQuery, e);
            }
        }
        return instanceMappings;
    }

    /**
     * Retrieves the mappings for a given model.
     * @param individualUri The URI of the individual to retrieve the mapping for.
     * @return The mappings in a JSON array.
     */
    public JSONArray retrieveIndividualMapping(String individualUri, IndexingModel model) {
        JSONArray instanceMappings = new JSONArray();
            String instanceQuery = model.generateInstanceQuery(individualUri);
            logger.debug("Extraction info for instance: {} with {}", individualUri, instanceQuery);
            try {
                Mappings instanceFieldQueryMappings = SPARQLRestAPI.getQueryProcess().query(instanceQuery);
                JSONObject instanceJSON = jsonFromFieldList(model.getFields().values(), instanceFieldQueryMappings);
                instanceMappings.put(instanceJSON);
            } catch (EngineException e) {
                logger.error("Error while retrieving instance {} for mapping using {}", individualUri, instanceQuery, e);
            }
        return instanceMappings;
    }

    private JSONObject jsonFromFieldList(Collection<IndexingField> fieldList, Mappings instanceMappings) {
        JSONObject json = new JSONObject();

        for(IndexingField field : fieldList) {
            if(field.isMultivalued()) {
                JSONArray retrievedValue = jsonFieldValueFromMultivaluedField(field, instanceMappings);
                if(! retrievedValue.isEmpty()) {
                    json.put(field.getLabel(), retrievedValue);
                }
            } else {
                String retrievedValue = jsonFieldValueFromField(field, instanceMappings);
                if(retrievedValue != null) {
                    json.put(field.getLabel(), retrievedValue);
                }
            }
        }

        return json;
    }

    private String jsonFieldValueFromField(IndexingField field, Mappings instanceMappings) {
        if(field.isMultivalued()) {
            throw new IllegalArgumentException("Field " + field.getLabel() + " is multivalued, use jsonFieldValueFromMultivaluedField instead");
        }
        if(! instanceMappings.isEmpty()
                && instanceMappings.get(0).getValue("?" + field.getLabel()) != null) {
            if(field.hasSubfields()) {
                return jsonFromFieldList(field.getSubfields().values(), instanceMappings).toString();
            } else {
                return instanceMappings.get(0).getValue("?" + field.getLabel()).stringValue();
            }
        }
        return null;
    }

    private JSONArray jsonFieldValueFromMultivaluedField(IndexingField field, Mappings instanceMappings) {
        JSONArray json = new JSONArray();
        logger.debug("Retrieving multivalued field {}", field.getLabel());
        for(Mapping m : instanceMappings) {
            if(m.getValue("?" + field.getLabel()) != null) {
                if(field.hasSubfields()) {
                    JSONObject value = jsonFromFieldList(field.getSubfields().values(), instanceMappings);
                    if(! json.toString().contains(value.toString()) ) {
                        json.put(value);
                    }
                } else {
                    String value = m.getValue("?" + field.getLabel()).stringValue();
                    if(value != null && ! value.isEmpty() && ! json.toList().contains(value)) {
                        json.put(value);
                    }
                }
            }
        }

        return json;
    }

    /**
     * generates a SPARQL query to retrieve the types of an instance
     * @param instanceUri must be an IRI
     * @return the SPARQL query with a "?type" variable
     */
    private static String generateInstanceTypeQuery(String instanceUri) {
        return "SELECT ?type WHERE { <" + instanceUri + "> a ?type . FILTER(IsIRI(?type)) }";
    }

    /**
     * Retrieves the models corresponding to the types of an instance
     * @param instanceUri The URI of the instance to retrieve the models for, must be an IRI.
     * @return The models of the instance.
     */
    private static Collection<IndexingModel> getModelsOfInstance(String instanceUri) {
        HashSet<IndexingModel> models = new HashSet<>();
        try {
            Mappings typeMappings = SPARQLRestAPI.getQueryProcess().query(generateInstanceTypeQuery(instanceUri));
            if(! typeMappings.isEmpty()) {
                for(Mapping m : typeMappings) {
                    IndexingModel model = IndexingManager.getInstance().getModel(m.getValue("?type").stringValue());
                    if(model != null) {
                        models.add(model);
                    }
                }
            }
        } catch (EngineException e) {
            logger.error("Error while retrieving type of instance {}", instanceUri, e);
        }
        return models;
    }
}
