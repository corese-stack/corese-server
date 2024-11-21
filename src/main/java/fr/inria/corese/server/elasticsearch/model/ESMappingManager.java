package fr.inria.corese.server.elasticsearch.model;

import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.server.webservice.SPARQLRestAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    public JSONArray getAllMappings() {
        JSONArray mappings = new JSONArray();
        for(IndexingModel model : IndexingManager.getInstance().getModels()) {
            mappings.put(retrieveMappings(model));
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
        return retrieveMappings(model);
    }

    private JSONArray retrieveMappings(IndexingModel model) {
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
            String instanceQuery = model.generateInstanceQuery(instanceUri);
            logger.info("Extraction info for instance: {} with {}", instanceUri, instanceQuery);
            try {
                Mappings instanceMappings = SPARQLRestAPI.getQueryProcess().query(instanceQuery);
                JSONObject instanceJSON = jsonFromFieldList(model.getFields().values(), instanceMappings);
                mappings.put(instanceJSON);
            } catch (EngineException e) {
                logger.error("Error while retrieving instance {} for mapping using {}", instanceUri, instanceQuery, e);
            }
        }

        return mappings;
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
        logger.info("Retrieving multivalued field {}", field.getLabel());
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
}
