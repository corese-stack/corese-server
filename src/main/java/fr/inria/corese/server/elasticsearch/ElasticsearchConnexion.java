package fr.inria.corese.server.elasticsearch;

import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the HTTP calls to an Elasticsearch server.
 * We do not expect to need more than one connection to the Elasticsearch server.
 */
public class ElasticsearchConnexion {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ElasticsearchConnexion.class);

    private static final String DEFAULT_ELASTICSEARCH_URL = "http://localhost:9200";

    private ElasticsearchConnexion() {
    }

    public static ElasticsearchConnexion create() {
        return new ElasticsearchConnexion();
    }

    public static ElasticsearchConnexion create(String url, String key) {
        ElasticsearchConnexion connexion = new ElasticsearchConnexion();
        connexion.setElasticSearchUrl(url);
        connexion.setElasticSearchKey(key);
        return connexion;
    }

    private String elasticSearchUrl = DEFAULT_ELASTICSEARCH_URL;
    private String elasticSearchKey = "";

    public void setElasticSearchUrl(String url) {
        elasticSearchUrl = url;
    }

    public void setElasticSearchKey(String key) {
        elasticSearchKey = key;
    }

    public String getElasticSearchUrl() {
        return elasticSearchUrl;
    }

    public String getElasticSearchKey() {
        return elasticSearchKey;
    }

    /**
     * Send a JSON object to the Elasticsearch server
     * @see ElasticsearchJSONUtils
     */
    public void sendJSON(JSONObject json) {
        List<List<String>> headers = new ArrayList<>();
        List<String> header = new ArrayList<>();
        header.add("Authorization-Type");
        header.add("ApiKey " + elasticSearchKey);
        headers.add(header);
        try {
            logger.info("Sending JSON to Elasticsearch server [{}]: {} {}", elasticSearchUrl, headers, json.toString());
            HTTPConnectionUtils.postConnection(elasticSearchUrl, headers, json.toString());
        } catch (Exception e) {
            logger.error("Error while sending {} to the server", json.toString(), e);
        }
    }
}
