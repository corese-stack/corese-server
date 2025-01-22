package fr.inria.corese.server.elasticsearch;

import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import fr.inria.corese.core.elasticsearch.EdgeChangeListener;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.logic.RDF;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.server.elasticsearch.model.ESMappingManager;
import fr.inria.corese.server.elasticsearch.model.IndexingModel;
import fr.inria.corese.server.elasticsearch.model.IndexingModelManager;
import fr.inria.corese.server.elasticsearch.util.ElasticsearchUtils;
import fr.inria.corese.server.elasticsearch.util.IndexingModelOntology;
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
        if (IndexingModelManager.getInstance().hasModels()) {
            HashSet<Node> modifiedClassUris = new HashSet<>();
            // If edge modifies models, then we refresh the model objects and send their instances to Elasticsearch
            // If an edge modifies an instance of a model, we send the description of the instance to Elasticsearch or the deletion order

            if (delete.isEmpty() && add.isEmpty()) {
                return;
            }
            // Any edge modifies a model ?
            modifiedClassUris.addAll(extractModifiedClassesNodes(add));
            modifiedClassUris.addAll(extractModifiedClassesNodes(delete));

            // refresh the models that have been modified in the indexing manager
            for (Node classNode : modifiedClassUris) {
                if (classNode.isConstant()) {
                    String classUri = classNode.getLabel();
                    IndexingModelManager.getInstance().extractModels(classUri);
                }
            }

            // Search for model instances that have been modified to send ES calls as appropriate
            // Pure delete are handled separately than modifications and insertion
            if (add.isEmpty() && !delete.isEmpty()) {
                handleBulkDelete(delete);
            } else {
                handleBulkEdgeInsertionModification(delete, add);
            }
        }
    }

    private void handleBulkEdgeInsertionModification(List<Edge> delete, List<Edge> add) {
        scanInsertedTriplesForInstancesAndDependencies(add);

        HashSet<Node> modifiedInstancesNodes = new HashSet<>();

        // If it modifies an instance of a model, we will send the description of the instance to Elasticsearch
        modifiedInstancesNodes.addAll(extractModifiedInstanceNodes(add));
        modifiedInstancesNodes.addAll(extractModifiedInstanceNodes(delete));

        // Update/addition handling
        for (Node instanceNode : modifiedInstancesNodes) {
            if (instanceNode.getDatatypeValue().isURI()) {
                Map<String, JSONArray> modifiedInstanceMappings = ESMappingManager.getInstance().retrieveIndividualMapping(instanceNode);
                try {
                    for (Map.Entry<String, JSONArray> instanceMappings : modifiedInstanceMappings.entrySet()) {
                        for (int i = 0; i < instanceMappings.getValue().length(); i++) {

                            JSONObject instanceMapping = instanceMappings.getValue().getJSONObject(i);
                            IndexResponse response = connexion.sendJSON(instanceMappings.getKey(), instanceMapping);

                            if (response != null) {
                                if (response.result() == Result.Created || response.result() == Result.Updated) {
                                } else {
                                    logger.error("Error while sending JSON to Elasticsearch index {}: {}", instanceMappings.getKey(), response);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    logger.error("Error while sending JSON to Elasticsearch", e);
                }
            }
        }
    }

    private void scanInsertedTriplesForInstancesAndDependencies(List<Edge> add) {
        add.forEach(edge -> {

            // scanning for instantiation of known model classes
            if ((edge.getPropertyNode().getLabel().equals(RDF.TYPE) )
                    && IndexingModelManager.getInstance().isModelClass(edge.getObjectNode().getLabel())) {
                Node classNode = edge.getObjectNode();
                Node instanceNode = edge.getSubjectNode();
                ESMappingManager.getInstance().addClassInstance(classNode.getLabel(), instanceNode);

                // There may be triples linking this instance to sub-resources that have been added previously
                IndexingModel model = IndexingModelManager.getInstance().getModel(classNode.getLabel());
                ESMappingManager.getInstance().extractDependencies(instanceNode, model);

                // If the triple uses an property from any of the models, we add it to the dependencies
            } else if (!edge.getSubjectNode().getDatatypeValue().isLiteral()
                    && !edge.getObjectNode().getDatatypeValue().isLiteral()
                    && IndexingModelManager.getInstance().getModels().stream().anyMatch(model -> model.usesProperty(edge.getPropertyNode().getLabel()))) {
                // If the edge is uses a model property and both subject and object are resources, we add the edge to the dependencies
                ESMappingManager.getInstance().addDependency(edge.getSubjectNode(), edge.getObjectNode());
            }
        });
    }

    private void handleBulkDelete(List<Edge> delete) {
        HashSet<Node> modifiedInstancesUrisInDeletion = new HashSet<>(extractModifiedInstanceNodes(delete));

        for (Node instanceNode : modifiedInstancesUrisInDeletion) {
            if(instanceNode.getDatatypeValue().isURI()) {
                // If the instance is in deletion, we check if it is complete and if not, we delete it
                ESMappingManager.getInstance().getModelsOfInstance(instanceNode).forEach(model -> {
                    String isInstanceCompleteQueryString = model.generateCheckInstanceIsCompleteQuery(instanceNode);
                    try {
                        Mappings instanceCompletudeResult = SPARQLRestAPI.getQueryProcess().query(isInstanceCompleteQueryString);
                        if (instanceCompletudeResult.size() == 0) {
                            connexion.sendDelete(model.getIndexName(), ElasticsearchUtils.generateDocIdFromUri(instanceNode.getDatatypeValue().toSparql()));
                        }
                    } catch (EngineException | IOException e) {
                        logger.error("Error while checking if instance is complete", e);
                    }
                });
            }
        }

    }

    private Set<Node> extractModifiedClassesNodes(List<Edge> edges) {
        Set<Node> modifiedClassUris = new HashSet<>();
        if (edges.stream().anyMatch(edge ->
                IndexingModelOntology.isDatatypeProperty(edge.getPropertyNode().getLabel())
                        || IndexingModelOntology.isObjectProperty(edge.getPropertyNode().getLabel()))) {
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
                String instanceModelType = "PREFIX im: <http://ns.mnemotix.com/ontologies/indexing-model/> SELECT ?class WHERE { " + instance.getDatatypeValue().toSparql() + " a im:IndexingModel ; im:indexingModelOf ?class . }";
                try {
                    Mappings results = SPARQLRestAPI.getQueryProcess().query(instanceModelType);
                    for (Mapping map : results) {
                        Node classNode = map.getNode("?class");
                        modifiedClassUris.add(classNode);
                    }
                } catch (EngineException e) {
                    logger.error("Could not determine the type of the instance " + instance.getLabel(), e);
                }
            }
        }
        return modifiedClassUris;
    }

    private Set<Node> extractModifiedInstanceNodes(List<Edge> edges) {
        Set<Node> modifiedInstanceUris = new HashSet<>();
        if (!edges.isEmpty()) {
            HashSet<Node> candidateInstanceNodes = new HashSet<>();
            for (Edge edge : edges) {
                // Does not modify a model
                if (!IndexingModelOntology.isDatatypeProperty(edge.getPropertyNode().getLabel())
                        && !IndexingModelOntology.isObjectProperty(edge.getPropertyNode().getLabel())) {
                    // Subject BN or URI
                    if (!edge.getSubjectNode().getDatatypeValue().isLiteral()) {
                        candidateInstanceNodes.add(edge.getSubjectNode());
                    }
                    // Object BN or URI
                    if (!edge.getObjectNode().getDatatypeValue().isLiteral()) {
                        candidateInstanceNodes.add(edge.getObjectNode());
                    }
                }
            }

            for (Node instanceNode : candidateInstanceNodes) {
                if (ESMappingManager.getInstance().isInstanceOfAModelClass(instanceNode)) {
                    modifiedInstanceUris.add(instanceNode);
                }
                if(ESMappingManager.getInstance().hasDependencies(instanceNode)) {
                    modifiedInstanceUris.addAll(ESMappingManager.getInstance().getDependencies(instanceNode));
                }
                if (ESMappingManager.getInstance().hasInverseDependencies(instanceNode)) {
                    modifiedInstanceUris.addAll(ESMappingManager.getInstance().getInverseDependencies(instanceNode));
                }
            }
        }
        return modifiedInstanceUris;
    }
}
