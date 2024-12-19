package fr.inria.corese.server.elasticsearch;

import com.github.tomakehurst.wiremock.admin.model.GetServeEventsResult;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.util.HTTPHeaders;
import fr.inria.corese.server.elasticsearch.model.IndexingManager;
import fr.inria.corese.server.webservice.endpoint.SPARQLRestAPI;
import org.junit.Rule;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@WireMockTest
public class ElasticsearchTest {

    private static final String testModelFile = "src/test/resources/fr/inria/corese/server/elasticsearch/esModel.ttl";
    private static final String testModelDataFile = "src/test/resources/fr/inria/corese/server/elasticsearch/esModelData.ttl";

    @Rule
    public WireMockRule elasticsearchService = new WireMockRule(9200);

    /**
     * Load the test model file and test if all mappings in the file are sent to Elasticsearch.
     */
    @Test
    public void loadThroughAPITest() throws LoadException, MalformedURLException {
        String query = "prefix foaf: <http://xmlns.com/foaf/0.1/> prefix schema: <https://schema.org/> prefix vcard: <http://www.w3.org/2006/vcard/ns#> INSERT DATA { <http://example.com/personSimpleInsertTest> a schema:Person ; foaf:firstName \"Jean\" ; foaf:lastName \"Dupont\" ; vcard:adr [ vcard:country-name \"France\" ; vcard:locality \"Nice\" ; vcard:postal-code \"06000\" ; vcard:street-address \"75 Promenade des anglais\" ] }";

        ElasticsearchConnexion connexion = ElasticsearchConnexion.create(elasticsearchService.baseUrl(), "testKey");

        SPARQLRestAPI.getTripleStore().getGraph().addEdgeChangeListener(new ElasticsearchListener(connexion));
        SPARQLRestAPI.getTripleStore().load(testModelFile); // Model declaration

        IndexingManager.getInstance().extractModels(); // Instantiation of model for mapping creation

        // Stub the elasticsearch service that accepts calls with the right response body according to https://www.elastic.co/guide/en/elasticsearch/reference/7.17/docs-index_.html
        elasticsearchService.stubFor(put("/person/_doc/httpexamplecomperson1")
                .withHeader(HTTPHeaders.AUTHORIZATION_TYPE, containing("ApiKey " + connexion.getElasticsearchAPIKey()))
                .withHeader(HTTPHeaders.CONTENT_TYPE, containing("application/vnd.elasticsearch+json"))
                .willReturn(ok()
                        .withHeader("X-Elastic-Product", "Elasticsearch")
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ " +
                                "\"_index\": \"person\", " +
                                "\"_type\": \"_doc\", " +
                                "\"_id\": \"httpexamplecomperson1\", " +
                                "\"_version\": 1, " +
                                "\"result\": \"created\", " +
                                "\"_shards\": { " +
                                "\"total\": 1, " +
                                "\"successful\": 1, " +
                                "\"failed\": 0 " +
                                "}, " +
                                "\"_seq_no\": 0, " +
                                "\"_primary_term\": 1 " +
                                "}"))
        );
        elasticsearchService.stubFor(put("/person/_doc/httpexamplecomperson2")
                .withHeader(HTTPHeaders.AUTHORIZATION_TYPE, containing("ApiKey " + connexion.getElasticsearchAPIKey()))
                .withHeader(HTTPHeaders.CONTENT_TYPE, containing("application/vnd.elasticsearch+json"))
                .willReturn(ok()
                        .withHeader("X-Elastic-Product", "Elasticsearch")
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ " +
                                "\"_index\": \"person\", " +
                                "\"_type\": \"_doc\", " +
                                "\"_id\": \"httpexamplecomperson2\", " +
                                "\"_version\": 1, " +
                                "\"result\": \"created\", " +
                                "\"_shards\": { " +
                                "\"total\": 1, " +
                                "\"successful\": 1, " +
                                "\"failed\": 0 " +
                                "}, " +
                                "\"_seq_no\": 0, " +
                                "\"_primary_term\": 1 " +
                                "}"))
        );
        elasticsearchService.stubFor(put("/person/_doc/httpexamplecomperson3")
                .withHeader(HTTPHeaders.AUTHORIZATION_TYPE, containing("ApiKey " + connexion.getElasticsearchAPIKey()))
                .withHeader(HTTPHeaders.CONTENT_TYPE, containing("application/vnd.elasticsearch+json"))
                .willReturn(ok()
                        .withHeader("X-Elastic-Product", "Elasticsearch")
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ " +
                                "\"_index\": \"person\", " +
                                "\"_type\": \"_doc\", " +
                                "\"_id\": \"httpexamplecomperson3\", " +
                                "\"_version\": 1, " +
                                "\"result\": \"created\", " +
                                "\"_shards\": { " +
                                "\"total\": 1, " +
                                "\"successful\": 1, " +
                                "\"failed\": 0 " +
                                "}, " +
                                "\"_seq_no\": 0, " +
                                "\"_primary_term\": 1 " +
                                "}"))
        );
        elasticsearchService.stubFor(put("/article/_doc/httpexamplecomarticle1")
                .withHeader(HTTPHeaders.AUTHORIZATION_TYPE, containing("ApiKey " + connexion.getElasticsearchAPIKey()))
                .withHeader(HTTPHeaders.CONTENT_TYPE, containing("application/vnd.elasticsearch+json"))
                .willReturn(ok()
                        .withHeader("X-Elastic-Product", "Elasticsearch")
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ " +
                                "\"_index\": \"person\", " +
                                "\"_type\": \"_doc\", " +
                                "\"_id\": \"httpexamplecomarticle1\", " +
                                "\"_version\": 1, " +
                                "\"result\": \"created\", " +
                                "\"_shards\": { " +
                                "\"total\": 1, " +
                                "\"successful\": 1, " +
                                "\"failed\": 0 " +
                                "}, " +
                                "\"_seq_no\": 0, " +
                                "\"_primary_term\": 1 " +
                                "}"))
        );
        elasticsearchService.stubFor(put("/article/_doc/httpexamplecomarticle2")
                .withHeader(HTTPHeaders.AUTHORIZATION_TYPE, containing("ApiKey " + connexion.getElasticsearchAPIKey()))
                .withHeader(HTTPHeaders.CONTENT_TYPE, containing("application/vnd.elasticsearch+json"))
                .willReturn(ok()
                        .withHeader("X-Elastic-Product", "Elasticsearch")
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ " +
                                "\"_index\": \"person\", " +
                                "\"_type\": \"_doc\", " +
                                "\"_id\": \"httpexamplecomarticle2\", " +
                                "\"_version\": 1, " +
                                "\"result\": \"created\", " +
                                "\"_shards\": { " +
                                "\"total\": 1, " +
                                "\"successful\": 1, " +
                                "\"failed\": 0 " +
                                "}, " +
                                "\"_seq_no\": 0, " +
                                "\"_primary_term\": 1 " +
                                "}"))
        );
        elasticsearchService.stubFor(put("/article/_doc/httpexamplecomarticle3")
                .withHeader(HTTPHeaders.AUTHORIZATION_TYPE, containing("ApiKey " + connexion.getElasticsearchAPIKey()))
                .withHeader(HTTPHeaders.CONTENT_TYPE, containing("application/vnd.elasticsearch+json"))
                .willReturn(ok()
                        .withHeader("X-Elastic-Product", "Elasticsearch")
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ " +
                                "\"_index\": \"person\", " +
                                "\"_type\": \"_doc\", " +
                                "\"_id\": \"httpexamplecomarticle3\", " +
                                "\"_version\": 1, " +
                                "\"result\": \"created\", " +
                                "\"_shards\": { " +
                                "\"total\": 1, " +
                                "\"successful\": 1, " +
                                "\"failed\": 0 " +
                                "}, " +
                                "\"_seq_no\": 0, " +
                                "\"_primary_term\": 1 " +
                                "}"))
        );


        // Verify that the elasticsearch service was called with the right data
        SPARQLRestAPI.getTripleStore().load(testModelDataFile); // Mappings should be sent as triples are loaded

        elasticsearchService.verify(moreThanOrExactly(1), putRequestedFor(urlEqualTo("/person/_doc/httpexamplecomperson1")).withRequestBody(
                equalToJson("{\"firstName\":\"John\",\"lastName\":\"Doe\",\"address\":[{\"country\":\"United States\",\"streetAddress\":\"123 Main Street\",\"postalCode\":\"10001\",\"locality\":\"New York\"}]}", true, true)
        ));

        elasticsearchService.verify(moreThanOrExactly(1), putRequestedFor(urlEqualTo("/person/_doc/httpexamplecomperson2")).withRequestBody(
                equalToJson("{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"address\":[{\"country\":\"United States\",\"streetAddress\":\"124 Main Street\",\"postalCode\":\"10001\",\"locality\":\"New York\"}]}", true, true)
        ));

        elasticsearchService.verify(moreThanOrExactly(1), putRequestedFor(urlEqualTo("/person/_doc/httpexamplecomperson3")).withRequestBody(
                equalToJson("{\"firstName\":\"John\",\"lastName\":\"Smith\",\"address\":[{\"country\":\"France\",\"streetAddress\":\"3, rue du pont\",\"postalCode\":\"75001\",\"locality\":\"Paris\"}]}", true, true)
        ));

        elasticsearchService.verify(moreThanOrExactly(1), putRequestedFor(urlEqualTo("/article/_doc/httpexamplecomarticle1")).withRequestBody(
                equalToJson("{\"author\":[{\"firstName\":\"John\",\"lastName\":\"Doe\"}],\"about\":[\"dog\",\"cute\"],\"abstract\":[\"Article 1\"]}", true, true)));

        elasticsearchService.verify(moreThanOrExactly(1), putRequestedFor(urlEqualTo("/article/_doc/httpexamplecomarticle2")).withRequestBody(
                equalToJson("{\"author\":[{\"firstName\":\"Jane\",\"lastName\":\"Doe\"}],\"about\":[\"cute\",\"cat\"],\"abstract\":[\"Article 2\"]}", true, true)));

        elasticsearchService.verify(moreThanOrExactly(1), putRequestedFor(urlEqualTo("/article/_doc/httpexamplecomarticle3")).withRequestBody(
                equalToJson("{\"author\":[{\"firstName\":\"John\",\"lastName\":\"Doe\"}, {\"firstName\":\"John\",\"lastName\":\"Smith\"}],\"about\":[\"dog\",\"cute\",\"cat\"],\"abstract\":[\"Article 3\"]}", true, true)
        ));
    }

    /**
     * Insertion of a description of a new person with a simple address
     */
    @Test
    public void simpleInsertTest() throws EngineException, LoadException, MalformedURLException {
        String query = "prefix foaf: <http://xmlns.com/foaf/0.1/> prefix schema: <https://schema.org/> prefix vcard: <http://www.w3.org/2006/vcard/ns#> INSERT DATA { <http://example.com/personSimpleInsertTest> a schema:Person ; foaf:firstName \"Jean\" ; foaf:lastName \"Dupont\" ; vcard:adr [ vcard:country-name \"France\" ; vcard:locality \"Nice\" ; vcard:postal-code \"06000\" ; vcard:street-address \"75 Promenade des anglais\" ] }";

        ElasticsearchConnexion connexion = ElasticsearchConnexion.create(elasticsearchService.baseUrl(), "testKey");

        SPARQLRestAPI.getTripleStore().getGraph().addEdgeChangeListener(new ElasticsearchListener(connexion));
        SPARQLRestAPI.getTripleStore().load(testModelFile);
        SPARQLRestAPI.getTripleStore().load(testModelDataFile);

        IndexingManager.getInstance().extractModels();

        // Stub the elasticsearch service that accepts calls with the right response body according to https://www.elastic.co/guide/en/elasticsearch/reference/7.17/docs-index_.html
        elasticsearchService.stubFor(put("/person/_doc/httpexamplecompersonSimpleInsertTest"
                // + URLEncoder.encode("http://example.com/personSimpleInsertTest", StandardCharsets.UTF_8)
                )
                        .withHeader(HTTPHeaders.AUTHORIZATION_TYPE, containing("ApiKey " + connexion.getElasticsearchAPIKey()))
                        .withHeader(HTTPHeaders.CONTENT_TYPE, containing("application/vnd.elasticsearch+json"))
                        .willReturn(ok()
                                .withHeader("X-Elastic-Product", "Elasticsearch")
                                .withHeader("Content-Type", "application/json")
                                .withBody("{ " +
                                        "\"_index\": \"person\", " +
                                        "\"_type\": \"_doc\", " +
                                        "\"_id\": \"1\", " +
                                        "\"_version\": 2, " +
                                        "\"result\": \"deleted\", " +
                                        "\"_shards\": { " +
                                        "\"total\": 1, " +
                                        "\"successful\": 1, " +
                                        "\"failed\": 0 " +
                                        "}, " +
                                        "\"_seq_no\": 0, " +
                                        "\"_primary_term\": 1 " +
                                        "}"))
        );

        SPARQLRestAPI.getTripleStore().getQueryProcess().query(query);

        // Verify that the elasticsearch service was called with the right data
        elasticsearchService.verify(exactly(1), putRequestedFor(urlEqualTo("/person/_doc/httpexamplecompersonSimpleInsertTest")).withRequestBody(
                equalToJson("{\"firstName\":\"Jean\",\"lastName\":\"Dupont\",\"address\":[{\"country\":\"France\",\"streetAddress\":\"75 Promenade des anglais\",\"postalCode\":\"06000\",\"locality\":\"Nice\"}]}", true, true)
        ));
    }

    /**
     * Insertion of a description of a second address to person 1
     */
    @Test
    public void insertMultiValuedTest() throws EngineException, LoadException, MalformedURLException {
        String query = "prefix foaf: <http://xmlns.com/foaf/0.1/> prefix schema: <https://schema.org/> prefix vcard: <http://www.w3.org/2006/vcard/ns#> INSERT DATA { <http://example.com/person1> vcard:adr [ vcard:country-name \"France\" ; vcard:locality \"Cannes\" ; vcard:postal-code \"06100\" ; vcard:street-address \"6 Promenade des anglais\" ] }";

        ElasticsearchConnexion connexion = ElasticsearchConnexion.create(elasticsearchService.baseUrl(), "testKey");

        SPARQLRestAPI.getTripleStore().getGraph().addEdgeChangeListener(new ElasticsearchListener(connexion));
        SPARQLRestAPI.getTripleStore().load(testModelFile);
        SPARQLRestAPI.getTripleStore().load(testModelDataFile);

        IndexingManager.getInstance().extractModels();

        // Stub the elasticsearch service that accepts calls with the right response body according to https://www.elastic.co/guide/en/elasticsearch/reference/7.17/docs-index_.html
        elasticsearchService.stubFor(put("/person/_doc/httpexamplecomperson1"
                // + URLEncoder.encode("http://example.com/personSimpleInsertTest", StandardCharsets.UTF_8)
                )
                        .withHeader(HTTPHeaders.AUTHORIZATION_TYPE, containing("ApiKey " + connexion.getElasticsearchAPIKey()))
                        .withHeader(HTTPHeaders.CONTENT_TYPE, containing("application/vnd.elasticsearch+json"))
                        .willReturn(ok()
                                .withHeader("X-Elastic-Product", "Elasticsearch")
                                .withHeader("Content-Type", "application/json")
                                .withBody("{ " +
                                        "\"_index\": \"person\", " +
                                        "\"_type\": \"_doc\", " +
                                        "\"_id\": \"1\", " +
                                        "\"_version\": 2, " +
                                        "\"result\": \"updated\", " +
                                        "\"_shards\": { " +
                                        "\"total\": 1, " +
                                        "\"successful\": 1, " +
                                        "\"failed\": 0 " +
                                        "}, " +
                                        "\"_seq_no\": 0, " +
                                        "\"_primary_term\": 1 " +
                                        "}"))
        );

        SPARQLRestAPI.getTripleStore().getQueryProcess().query(query);

        // Verify that the elasticsearch service was called with the right data
        elasticsearchService.verify(exactly(1), putRequestedFor(urlEqualTo("/person/_doc/httpexamplecomperson1")).withRequestBody(
                equalToJson("{\"firstName\":\"John\",\"lastName\":\"Doe\",\"address\":[{\"country\":\"France\",\"streetAddress\":\"6 Promenade des anglais\",\"postalCode\":\"06100\",\"locality\":\"Cannes\"}, {\"country\":\"United States\",\"streetAddress\":\"123 Main Street\",\"postalCode\":\"10001\",\"locality\":\"New York\"}]}", true, true)
        ));
    }

    @Test
    public void deleteNonExistingTest() throws EngineException, MalformedURLException, LoadException {
        String query = "prefix foaf: <http://xmlns.com/foaf/0.1/> prefix schema: <https://schema.org/> prefix vcard: <http://www.w3.org/2006/vcard/ns#> DELETE DATA { <http://example.com/personSimpleInsertTest> a schema:Person ; foaf:firstName \"Jean\" ; foaf:lastName \"Dupont\" ; vcard:adr [ vcard:country-name \"France\" ; vcard:locality \"Nice\" ; vcard:postal-code \"06000\" ; vcard:street-address \"75 Promenade des anglais\" ] }";

        ElasticsearchConnexion connexion = ElasticsearchConnexion.create(elasticsearchService.baseUrl(), "testKey");

        SPARQLRestAPI.getTripleStore().getGraph().addEdgeChangeListener(new ElasticsearchListener(connexion));
        SPARQLRestAPI.getTripleStore().load(testModelFile);
        SPARQLRestAPI.getTripleStore().load(testModelDataFile);

        IndexingManager.getInstance().extractModels();

        // Stub the elasticsearch service that accepts calls with the right response body according to https://www.elastic.co/guide/en/elasticsearch/reference/7.17/docs-index_.html
        elasticsearchService.stubFor(post("/person/_doc")
                .withHeader(HTTPHeaders.AUTHORIZATION_TYPE, containing("ApiKey " + connexion.getElasticsearchAPIKey()))
                .withHeader(HTTPHeaders.CONTENT_TYPE, containing("application/vnd.elasticsearch+json"))
                .willReturn(ok()
                        .withHeader("X-Elastic-Product", "Elasticsearch")
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ " +
                                "\"_index\": \"person\", " +
                                "\"_type\": \"_doc\", " +
                                "\"_id\": \"1\", " +
                                "\"_version\": 1, " +
                                "\"result\": \"updated\", " +
                                "\"_shards\": { " +
                                "\"total\": 0, " +
                                "\"successful\": 0, " +
                                "\"failed\": 1 " +
                                "}, " +
                                "\"_seq_no\": 0, " +
                                "\"_primary_term\": 1 " +
                                "}"))
        );

        SPARQLRestAPI.getTripleStore().getQueryProcess().query(query);

        // Verify that the elasticsearch service was not called
        elasticsearchService.verify(exactly(0), postRequestedFor(urlEqualTo("/person/_doc"))
        );
    }
/*
    @Test
    public void deleteExistingTest() throws EngineException, MalformedURLException, LoadException {
        String insertQuery = "prefix foaf: <http://xmlns.com/foaf/0.1/> prefix schema: <https://schema.org/> prefix vcard: <http://www.w3.org/2006/vcard/ns#> INSERT DATA { <http://example.com/personSimpleInsertTest> a schema:Person ; foaf:firstName \"Jean\" ; foaf:lastName \"Dupont\" ; vcard:adr [ vcard:country-name \"France\" ; vcard:locality \"Nice\" ; vcard:postal-code \"06000\" ; vcard:street-address \"75 Promenade des anglais\" ] }";
        String deleteQuery = "prefix foaf: <http://xmlns.com/foaf/0.1/> prefix schema: <https://schema.org/> prefix vcard: <http://www.w3.org/2006/vcard/ns#> DELETE DATA { <http://example.com/personSimpleInsertTest> a schema:Person ; foaf:firstName \"Jean\" ; foaf:lastName \"Dupont\" ; vcard:adr [ vcard:country-name \"France\" ; vcard:locality \"Nice\" ; vcard:postal-code \"06000\" ; vcard:street-address \"75 Promenade des anglais\" ] }";

        ElasticsearchConnexion connexion = ElasticsearchConnexion.create(elasticsearchService.baseUrl(), "testKey");

        SPARQLRestAPI.getTripleStore().getGraph().addEdgeChangeListener(new ElasticsearchListener(connexion));
        SPARQLRestAPI.getTripleStore().load(testModelFile);
        SPARQLRestAPI.getTripleStore().load(testModelDataFile);

        // Stub the elasticsearch service that accepts calls with the right response body according to https://www.elastic.co/guide/en/elasticsearch/reference/7.17/docs-index_.html
        elasticsearchService.stubFor(post("/person/_doc")
                .withHeader(HTTPHeaders.AUTHORIZATION_TYPE, containing("ApiKey " + connexion.getElasticsearchAPIKey()))
                .withHeader(HTTPHeaders.CONTENT_TYPE, containing("application/vnd.elasticsearch+json"))
                .willReturn(ok()
                        .withHeader("X-Elastic-Product", "Elasticsearch")
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ " +
                                "\"_index\": \"person\", " +
                                "\"_type\": \"_doc\", " +
                                "\"_id\": \"1\", " +
                                "\"_version\": 1, " +
                                "\"result\": \"updated\", " +
                                "\"_shards\": { " +
                                "\"total\": 0, " +
                                "\"successful\": 0, " +
                                "\"failed\": 1 " +
                                "}, " +
                                "\"_seq_no\": 0, " +
                                "\"_primary_term\": 1 " +
                                "}"))
        );

        SPARQLRestAPI.getTripleStore().getQueryProcess().query(insertQuery);

        IndexingManager.getInstance().extractModels();

        SPARQLRestAPI.getTripleStore().getGraph().addEdgeChangeListener(new ElasticsearchListener(connexion));

        SPARQLRestAPI.getTripleStore().getQueryProcess().query(deleteQuery);

        System.out.println("deleteExistingTest");

        elasticsearchService.verify(exactly(1), deleteRequestedFor(urlEqualTo("/person/_doc"))
                .withHeader(HTTPHeaders.AUTHORIZATION_TYPE, containing("ApiKey " + connexion.getElasticsearchAPIKey()))
                .withHeader(HTTPHeaders.CONTENT_TYPE, containing("application/vnd.elasticsearch+json"))
        );
    }*/
}
