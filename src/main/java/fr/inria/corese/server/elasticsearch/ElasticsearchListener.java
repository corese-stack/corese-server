package fr.inria.corese.server.elasticsearch;

import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import fr.inria.corese.core.elasticsearch.EdgeChangeListener;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.server.elasticsearch.model.ESMappingManager;
import fr.inria.corese.server.elasticsearch.model.IndexingManager;
import fr.inria.corese.server.elasticsearch.model.IndexingModelOntology;
import fr.inria.corese.server.webservice.endpoint.SPARQLRestAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Listens to changes in the graph and sends the modifications to Elasticsearch.
 * The modifications are checks to see if they modify models or their instances and if so, the instances of the models are sent to Elasticsearch.
 */
public class ElasticsearchListener extends EdgeChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchListener.class);

    private final ElasticsearchConnexion connexion;

    public ElasticsearchListener(String url, String key) throws MalformedURLException {
        this.connexion = ElasticsearchConnexion.create(url, key);
    }

    public ElasticsearchListener(ElasticsearchConnexion connexion) {
        this.connexion = connexion;
    }

    public ElasticsearchListener() {
        this(ElasticsearchConnexion.create());
    }

    @Override
    public void onBulkEdgeChange(List<Edge> delete, List<Edge> add) {
        HashSet<String> modifiedClassUris = new HashSet<>();
        HashSet<String> modifiedInstancesUris = new HashSet<>();
        // If edge modifies models, then we refresh the model objects and send their instances to Elasticsearch
        if(delete.isEmpty() && add.isEmpty()) {
            logger.debug("Bulk edge change with no edge to delete or add");
            return;
        }
        // Any edge modifies a model ?
        if(delete.stream().anyMatch(edge -> IndexingModelOntology.isDatatypeProperty(edge.getPropertyNode().getLabel()) || IndexingModelOntology.isObjectProperty(edge.getPropertyNode().getLabel()) )) {
            HashSet<Node> instances = new HashSet<>();
            for(Edge edge : delete) {
                if(IndexingModelOntology.isDatatypeProperty(edge.getPropertyNode().getLabel())
                        || IndexingModelOntology.isObjectProperty(edge.getPropertyNode().getLabel())) {
                    modifiedInstancesUris.add(edge.getSubjectNode().getLabel());
                    instances.add(edge.getSubjectNode());
                }
            }

            // What are the classes of the model modified ?
            for(Node instance : instances) {
                String instanceModelType = "PREFIX im: <http://ns.mnemotix.com/ontologies/indexing-model/> SELECT ?class WHERE { <" + instance.getLabel() + "> a im:IndexingModel ; im:indexingModelOf ?class . }";
                try {
                    Mappings results = SPARQLRestAPI.getQueryProcess().query(instanceModelType);
                    for(Mapping map : results) {
                        Node classNode = map.getNode("?class");
                        modifiedClassUris.add(classNode.getLabel());
                    }
                } catch (EngineException e) {
                    logger.error("Could not determine the type of the instance " + instance.getLabel(), e);
                }
            }
        }

        // If it modifies an instance of a model, we sent the description of the instance to Elasticsearch
        HashSet<String> candidateInstanceUri = new HashSet<>();
        for(Edge edge : add) {
            candidateInstanceUri.add(edge.getSubjectNode().getLabel());
        }
        for(Edge edge : delete) {
            candidateInstanceUri.add(edge.getSubjectNode().getLabel());
        }
        for(String instanceUri : candidateInstanceUri) {
            String instanceTypeQuery = "PREFIX im: <http://ns.mnemotix.com/ontologies/indexing-model/> SELECT ?class WHERE { <" + instanceUri + "> a ?class . ?model a im:IndexingModel ; im:indexingModelOf ?class . }";
            try {
                Mappings result = SPARQLRestAPI.getQueryProcess().query(instanceTypeQuery);
                if(result.size() > 0) {
                    modifiedInstancesUris.add(instanceUri);
                }
            } catch (EngineException e) {
                logger.error("Could not determine the type of the instance " + instanceUri, e);
            }
        }

        // Prepare the JSON to send to Elasticsearch
        // Update the models
        for(String classUri : modifiedClassUris) {
            IndexingManager.getInstance().extractModels(classUri);
        }
        JSONArray json = new JSONArray();
        for(String instanceUri : modifiedInstancesUris) {
            Map<String, JSONObject> modifiedInstanceMappings = ESMappingManager.getInstance().retrieveIndividualMapping(instanceUri);

            logger.debug("Sending JSON to Elasticsearch: " + json.toString());
            if(connexion.getElasticsearchUrl() != null) {
                try {
                    for(Map.Entry<String, JSONObject> instanceMapping : modifiedInstanceMappings.entrySet()) {
                        IndexResponse response = connexion.sendJSON(instanceMapping.getKey(), instanceMapping.getValue());

                        if(response != null && (response.result() == Result.Created || response.result() == Result.Updated)) {
                            logger.debug("JSON sent to Elasticsearch with status code: {}", response.toString());
                        } else {
                            logger.error("Error while sending JSON to Elasticsearch: {}", response.toString());
                        }
                    }
                } catch (IOException e) {
                    logger.error("Error while sending JSON to Elasticsearch", e);
                }
            }
        }
    }
}
