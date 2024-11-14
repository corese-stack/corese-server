package fr.inria.corese.server.elasticsearch;

import fr.inria.corese.core.util.HTTPHeaders;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the HTTP calls to an Elasticsearch server.
 * We do not expect to need more than one connection to the Elasticsearch server.
 */
public class ElasticsearchConnexion {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ElasticsearchConnexion.class);

    private static final String DEFAULT_ELASTICSEARCH_URL = "http://localhost:9200";
    private String elasticSearchUrl = DEFAULT_ELASTICSEARCH_URL;
    private String elasticSearchKey = "";

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

    public String getElasticSearchUrl() {
        return elasticSearchUrl;
    }

    public void setElasticSearchUrl(String url) {
        elasticSearchUrl = url;
    }

    public String getElasticSearchKey() {
        return elasticSearchKey;
    }

    public void setElasticSearchKey(String key) {
        elasticSearchKey = key;
    }

    /**
     * Send a JSON object to the Elasticsearch server
     *
     * @return the HTTP status code of the request
     * @see ElasticsearchJSONUtils
     */
    public int sendJSON(JSONObject json) throws IOException {
        List<List<String>> headers = new ArrayList<>();
        List<String> authorizationHeader = new ArrayList<>();
        authorizationHeader.add(HTTPHeaders.AUTHORIZATION_TYPE);
        authorizationHeader.add("ApiKey " + elasticSearchKey);
        List<String> contentTypeHeader = new ArrayList<>();
        contentTypeHeader.add(HTTPHeaders.CONTENT_TYPE);
        contentTypeHeader.add("application/json");
        headers.add(authorizationHeader);
        headers.add(contentTypeHeader);
        logger.info("Sending JSON to Elasticsearch server {}: {} {}", elasticSearchUrl, headers, json.toString());
        HttpURLConnection connexion = HTTPConnectionUtils.postConnection(elasticSearchUrl, headers, json.toString());
        int responseCode = connexion.getResponseCode();
        connexion.disconnect();

        return responseCode;
    }
}
