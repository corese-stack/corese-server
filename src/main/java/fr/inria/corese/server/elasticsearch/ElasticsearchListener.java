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
import java.util.Set;

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
        logger.info("Bulk edge change {} {}", delete, add);
        HashSet<String> modifiedClassUris = new HashSet<>();
        HashSet<String> modifiedInstancesUris = new HashSet<>();
        // If edge modifies models, then we refresh the model objects and send their instances to Elasticsearch
        if (delete.isEmpty() && add.isEmpty()) {
            logger.debug("Bulk edge change with no edge to delete or add");
            return;
        }
        // Any edge modifies a model ?
        modifiedClassUris.addAll(extractModifiedClassesUris(add));
        modifiedClassUris.addAll(extractModifiedClassesUris(delete));

        // If it modifies an instance of a model, we sent the description of the instance to Elasticsearch
        modifiedInstancesUris.addAll(extractModifiedInstanceUris(add));
        modifiedInstancesUris.addAll(extractModifiedInstanceUris(delete));

        // Prepare the JSON to send to Elasticsearch
        // refresh the models that have been modified in the indexing manager
        for (String classUri : modifiedClassUris) {
            IndexingManager.getInstance().extractModels(classUri);
        }

        for (String instanceUri : modifiedInstancesUris) {
            Map<String, JSONArray> modifiedInstanceMappings = ESMappingManager.getInstance().retrieveIndividualMapping(instanceUri);
                try {
                    for (Map.Entry<String, JSONArray> instanceMappings : modifiedInstanceMappings.entrySet()) {
                        for(int i = 0; i < instanceMappings.getValue().length(); i++) {
                            JSONObject instanceMapping = instanceMappings.getValue().getJSONObject(i);
                            IndexResponse response = connexion.sendJSON(instanceMappings.getKey(), instanceMapping);

                            if (response != null) {
                                if (response.result() == Result.Created || response.result() == Result.Updated) {
                                    logger.debug("JSON sent to Elasticsearch index {} with response: {}", instanceMappings.getKey(), response.toString());
                                } else {
                                    logger.error("Error while sending JSON to Elasticsearch index {}: {}", instanceMappings.getKey(), response.toString());
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    logger.error("Error while sending JSON to Elasticsearch", e);
                }
        }
    }

    private Set<String> extractModifiedClassesUris(List<Edge> edges) {
        Set<String> modifiedClassUris = new HashSet<>();
        if(edges.stream().anyMatch(edge -> IndexingModelOntology.isDatatypeProperty(edge.getPropertyNode().getLabel()) || IndexingModelOntology.isObjectProperty(edge.getPropertyNode().getLabel()))) {
            // What are the instances of the modified models ?
            HashSet<Node> instances = new HashSet<>();
            for (Edge edge : edges) {
                if (IndexingModelOntology.isDatatypeProperty(edge.getPropertyNode().getLabel())
                        || IndexingModelOntology.isObjectProperty(edge.getPropertyNode().getLabel())) {
                    instances.add(edge.getSubjectNode());
                }
            }

            // What are the classes of the modified models ?
            for (Node instance : instances) {
                String instanceModelType = "PREFIX im: <http://ns.mnemotix.com/ontologies/indexing-model/> SELECT ?class WHERE { <" + instance.getLabel() + "> a im:IndexingModel ; im:indexingModelOf ?class . }";
                try {
                    Mappings results = SPARQLRestAPI.getQueryProcess().query(instanceModelType);
                    for (Mapping map : results) {
                        Node classNode = map.getNode("?class");
                        modifiedClassUris.add(classNode.getLabel());
                    }
                } catch (EngineException e) {
                    logger.error("Could not determine the type of the instance " + instance.getLabel(), e);
                }
            }
        }
        return modifiedClassUris;
    }

    private Set<String> extractModifiedInstanceUris(List<Edge> edges) {
        Set<String> modifiedInstanceUris = new HashSet<>();
        if(edges.stream().anyMatch(edge -> ! IndexingModelOntology.isDatatypeProperty(edge.getPropertyNode().getLabel()) && ! IndexingModelOntology.isObjectProperty(edge.getPropertyNode().getLabel()))){

            HashSet<String> candidateInstanceUri = new HashSet<>();
            for (Edge edge : edges) {
                if(! IndexingModelOntology.isDatatypeProperty(edge.getPropertyNode().getLabel())
                        && ! IndexingModelOntology.isObjectProperty(edge.getPropertyNode().getLabel())) {
                    if(! edge.getSubjectNode().isBlank()) {
                        candidateInstanceUri.add(edge.getSubjectNode().getLabel());
                    }
                    if(! edge.getObjectNode().isBlank() && ! edge.getObjectNode().isConstant()) {
                        candidateInstanceUri.add(edge.getObjectNode().getLabel());
                    }

                }
            }

            for (String instanceUri : candidateInstanceUri) {
                String instanceTypeQuery = "PREFIX im: <http://ns.mnemotix.com/ontologies/indexing-model/> SELECT ?class WHERE { { <" + instanceUri + "> a ?class } UNION { GRAPH ?graphData { <" + instanceUri + "> a ?class . } . { ?model a im:IndexingModel ; im:indexingModelOf ?class . } UNION {  GRAPH ?modelGraph { ?model a im:IndexingModel ; im:indexingModelOf ?class . } } } }";
                try {
                    Mappings result = SPARQLRestAPI.getQueryProcess().query(instanceTypeQuery);

                    if (result.size() > 0) {
                        modifiedInstanceUris.add(instanceUri);
                    }
                } catch (EngineException e) {
                    logger.error("Could not determine the type of the instance " + instanceUri, e);
                }
            }
        }
        return modifiedInstanceUris;
    }
}
