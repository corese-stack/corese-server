package fr.inria.corese.server.webservice.endpoint;

import co.elastic.clients.elasticsearch.core.BulkResponse;
import fr.inria.corese.server.elasticsearch.ElasticsearchConnexion;
import fr.inria.corese.server.elasticsearch.model.ESMappingManager;
import fr.inria.corese.server.elasticsearch.model.IndexingManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

@Path("elasticsearch")
public class ElasticsearchControl {
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchControl.class);

    ElasticsearchConnexion connexion = ElasticsearchConnexion.create();

    @GET
    @Path("dump")
    public Response refresh() {
        IndexingManager.getInstance().extractModels();
        Map<String, JSONArray> allMappings = ESMappingManager.getInstance().getAllMappings();
        for(Map.Entry<String, JSONArray> modelMappingsEntry : allMappings.entrySet()) {
            try {
                BulkResponse response = connexion.sendBulkJSON(modelMappingsEntry.getKey(), modelMappingsEntry.getValue());
                if (response.errors()) {
                    logger.error("Error while sending mapping to Elasticsearch: Received code {}", response.toString());
                }
            } catch (IOException e) {
                logger.error("Error while sending mapping to Elasticsearch", e);
            }
        };
        return Response.ok().build();
    }
}
