package fr.inria.corese.server.webservice;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import fr.inria.corese.core.GraphStore;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.util.HTTPHeaders;
import fr.inria.corese.server.elasticsearch.ElasticsearchConnexion;
import fr.inria.corese.server.elasticsearch.ElasticsearchListener;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

@WireMockTest
public class ElasticsearchTest {

    @Rule
    public WireMockRule elasticsearchService = new WireMockRule(9200);

    @Test
    public void insertTest() throws EngineException {
        String query = "INSERT DATA { <http://example.com/subject> <http://example.com/property> <http://example.com/object> }";

        GraphStore graphStore = GraphStore.create();

        class EdgeChangeListenerTest extends ElasticsearchListener {
            public EdgeChangeListenerTest(ElasticsearchConnexion connexion) {
                super(connexion);
            }

            @Override
            public void onBulkEdgeChange(List<Edge> delete, List<Edge> insert) {
                super.onBulkEdgeChange(delete, insert);

                assertEquals(0, delete.size());
                assertEquals(1, insert.size());
                assertEquals("http://example.com/subject", insert.get(0).getSubjectNode().getLabel());
                assertEquals("http://example.com/property", insert.get(0).getPropertyNode().getLabel());
                assertEquals("http://example.com/object", insert.get(0).getObjectNode().getLabel());
            }
        }
        ElasticsearchConnexion connexion = ElasticsearchConnexion.create();
        connexion.setElasticSearchKey("test");
        connexion.setElasticSearchUrl(elasticsearchService.baseUrl());

        EdgeChangeListenerTest edgeChangeListenerTest = new EdgeChangeListenerTest(connexion);
        graphStore.addEdgeChangeListener(edgeChangeListenerTest);

        elasticsearchService.stubFor(post("/")
                .withHeader(HTTPHeaders.AUTHORIZATION_TYPE, containing("ApiKey " + connexion.getElasticSearchKey()))
                .withHeader(HTTPHeaders.CONTENT_TYPE, containing("application/json"))
                .willReturn(ok())
        );

        QueryProcess queryProcess = QueryProcess.create(graphStore);
        queryProcess.query(query);

        elasticsearchService.verify(postRequestedFor(urlEqualTo("/")).withRequestBody(
                matchingJsonPath("$.insert[0].subject", equalTo("http://example.com/subject"))
                        .and(matchingJsonPath("$.insert[0].predicate", equalTo("http://example.com/property")))
                        .and(matchingJsonPath("$.insert[0].object", equalTo("http://example.com/object")))));
    }

    @Test
    public void deleteNonExistingTest() throws EngineException {
        String insertQuery = "INSERT DATA { <http://example.com/subject1> <http://example.com/property1> <http://example.com/object1> . <http://example.com/subject2> <http://example.com/property2> <http://example.com/object2> }";
        String deletenonexisttingQueryy = "DELETE DATA { <http://example.com/subject3> <http://example.com/property3> <http://example.com/object3> }";
    }

    @Test
    public void deleteExistingTest() {
        String insertQuery = "INSERT DATA { <http://example.com/subject1> <http://example.com/property1> <http://example.com/object1> . <http://example.com/subject2> <http://example.com/property2> <http://example.com/object2> }";
        String deleteQuery = "DELETE DATA { <http://example.com/subject1> <http://example.com/property1> <http://example.com/object1> }";
    }

    @Test
    public void loadTest() throws LoadException {
        String query = "LOAD <src/test/resources/elasticsearch/data.ttl>";
    }
}
