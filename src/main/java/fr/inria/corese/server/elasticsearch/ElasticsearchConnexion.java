package fr.inria.corese.server.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.json.JsonData;
import fr.inria.corese.core.util.HTTPHeaders;
import fr.inria.corese.core.util.Property;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.client.RestClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;

/**
 * Handles the HTTP calls to an Elasticsearch server.
 */
public class ElasticsearchConnexion {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ElasticsearchConnexion.class);

    private static final String DEFAULT_ELASTICSEARCH_URL = "http://localhost:9200";
    private String elasticSearchUrl = Property.getStringValue(Property.Value.ELASTICSEARCH_API_ADDRESS);
    private String elasticSearchAPIKey = Property.getStringValue(Property.Value.ELASTICSEARCH_API_KEY);

    private ElasticsearchClient esClient;

    private ElasticsearchConnexion() {
    }

    public static ElasticsearchConnexion create(String elasticSearchUrl, String key) throws MalformedURLException {
        ElasticsearchConnexion connexion = new ElasticsearchConnexion();
        connexion.setElasticsearchUrl(elasticSearchUrl);
        connexion.setElasticsearchAPIKey(key);
        URL url = new URL(elasticSearchUrl);
        logger.debug("Connecting to Elasticsearch server at {}, host: {}, port:{}, protocol: {}", url, url.getHost(), url.getPort(), url.getProtocol());

        RestClient restClient = RestClient.builder(
                new org.apache.http.HttpHost(url.getHost(), url.getPort(), url.getProtocol())).setDefaultHeaders(new Header[] {
                        new BasicHeader(HTTPHeaders.AUTHORIZATION_TYPE, "ApiKey " + key)
        }).build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        connexion.esClient = new ElasticsearchClient(transport);
        return connexion;
    }

    public static ElasticsearchConnexion create(String key) throws MalformedURLException {
        return ElasticsearchConnexion.create(DEFAULT_ELASTICSEARCH_URL, key);
    }

    public static ElasticsearchConnexion create() throws IllegalStateException {
        ElasticsearchConnexion connexion = new ElasticsearchConnexion();
        if(connexion.getElasticsearchAPIKey() == null) {
            throw new IllegalStateException("Elasticsearch API key is not set");
        }
        if(connexion.getElasticsearchUrl() == null) {
            throw new IllegalStateException("Elasticsearch URL is not set");
        }
        URL url = null;
        try {
            url = new URL(connexion.getElasticsearchUrl());
        } catch (MalformedURLException e) {
            logger.error("Error while creating ElasticsearchConnexion {}", url, e);
        }
        logger.debug("Connecting to Elasticsearch server at {}, host: {}, port:{}, protocol: {}", url, url.getHost(), url.getPort(), url.getProtocol());

        RestClient restClient = RestClient.builder(
                new org.apache.http.HttpHost(url.getHost(), url.getPort(), url.getProtocol())).setDefaultHeaders(new Header[] {
                new BasicHeader(HTTPHeaders.AUTHORIZATION_TYPE, "ApiKey " + connexion.getElasticsearchAPIKey())
        }).build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        connexion.esClient = new ElasticsearchClient(transport);
        return connexion;
    }

    public String getElasticsearchUrl() {
        return elasticSearchUrl;
    }

    public void setElasticsearchUrl(String url) {
        elasticSearchUrl = url;
    }

    public String getElasticsearchAPIKey() {
        return elasticSearchAPIKey;
    }

    public void setElasticsearchAPIKey(String key) {
        elasticSearchAPIKey = key;
    }

    /**
     * Send a JSON object to the Elasticsearch server
     * @param index the index to send the JSON object to
     * @param json the JSON object to send
     * @return the response from the server or null if no server is set
     */
    public IndexResponse sendJSON(String index, JSONObject json) throws IOException {
        if((elasticSearchUrl != null) && (elasticSearchAPIKey != null)) {
            logger.info("Sending to Elasticsearch server {} {}", elasticSearchUrl, json);
            json.remove("uri"); // remove the uri field (it is used as the id in the index
            Reader input = new StringReader(json.toString());
            IndexRequest<JsonData> request = IndexRequest.of(i -> i
                    .index(index)
                    .id(json.getString("uri"))
                    .withJson(input)
            );
            logger.debug("Sending to Elasticsearch server {}", request);

            return esClient.index(request);
        } else {
            return null;
        }
    }

    public BulkResponse sendBulkJSON(String index, JSONArray json) throws IOException {
        if((elasticSearchUrl != null) && (elasticSearchAPIKey != null)) {
            logger.info("Sending to Elasticsearch server {} {}", elasticSearchUrl, json);
            BulkRequest.Builder br = new BulkRequest.Builder();

            for(int i = 0; i < json.length(); i++) {
                JSONObject obj = json.getJSONObject(i);
                obj.remove("uri"); // remove the uri field (it is used as the id in the index
                Reader input = new StringReader(obj.toString());

                br.operations(op -> op
                        .index(idx -> idx
                            .index(index)
                            .id(obj.getString("uri"))
                            .withJson(input)
                        ));
            }

            return esClient.bulk(br.build());
        } else {
            return null;
        }
    }

    public UpdateResponse<JsonData> sendUpdateJSON(String index, String id, JSONObject json) throws IOException {
        if((elasticSearchUrl != null) && (elasticSearchAPIKey != null)) {
            Reader input = new StringReader(json.toString());
            UpdateRequest<JsonData, JsonData> request = (new UpdateRequest.Builder<JsonData, JsonData>()
                    .id(id)
                    .index(index)
                    .withJson(input))
                    .build();

            return esClient.update(request, JsonData.class);
        } else {
            return null;
        }
    }

    public ElasticsearchClient getElasticsearchClient() {
        return esClient;
    }

    public void close() {
        try {
            esClient.close();
        } catch (IOException e) {
            logger.error("Error while closing the Elasticsearch client", e);
        }
    }
}
