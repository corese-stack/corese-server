package fr.inria.corese.server.elasticsearch;

import fr.inria.corese.core.elasticsearch.EdgeChangeListener;
import fr.inria.corese.core.kgram.api.core.Edge;
import org.apache.jena.atlas.json.JSON;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ElasticsearchListener extends EdgeChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchListener.class);

    private ElasticsearchConnexion connexion;

    public ElasticsearchListener() {
        this.connexion = ElasticsearchConnexion.create();
    }

    public ElasticsearchListener(String url, String key) {
        this.connexion = ElasticsearchConnexion.create(url, key);
    }

    public ElasticsearchListener(ElasticsearchConnexion connexion) {
        this.connexion = connexion;
    }

    @Override
    public void onBulkEdgeChange(List<Edge> delete, List<Edge> add) {
        JSONObject json = ElasticsearchJSONUtils.toJSONObject(delete, add);
        logger.info("Sending JSON to Elasticsearch: " + json.toString());
        connexion.sendJSON(json);
    }
}
