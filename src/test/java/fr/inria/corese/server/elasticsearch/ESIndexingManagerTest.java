package fr.inria.corese.server.elasticsearch;

import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.server.JSONUtils;
import fr.inria.corese.server.elasticsearch.model.ESMappingManager;
import fr.inria.corese.server.elasticsearch.model.IndexingManager;
import fr.inria.corese.server.elasticsearch.model.IndexingModel;
import fr.inria.corese.server.webservice.endpoint.SPARQLRestAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class ESIndexingManagerTest {

    private static final String testModelFile1 = "src/test/resources/fr/inria/corese/server/elasticsearch/indexingModelExample.trig";
    private static final String testModelFile2 = "src/test/resources/fr/inria/corese/server/elasticsearch/esModel.ttl";
    private static final String testModelDataFile2 = "src/test/resources/fr/inria/corese/server/elasticsearch/esModelData.ttl";

    @Test
    public void testLoadIndexingModel1() throws EngineException, LoadException {
        SPARQLRestAPI.getTripleStore().load(testModelFile1, Loader.format.TRIG_FORMAT);

        // Test the loading of the indexing model
        IndexingManager.getInstance().extractModels();

        // Test the indexing model from the data file
        IndexingModel model = IndexingManager.getInstance().getModel("http://data.clairsienne.com/ontologies/2019/12/clr-patrimoine#Lot");

        assertNotNull(model);
        assertEquals("http://data.clairsienne.com/ontologies/2019/12/clr-patrimoine#Lot", model.getClassUri());
        // assertEquals(2, model.getPrefixes().size());
        assertEquals(3, model.getFields().size());
        assertTrue(model.getFields().containsKey("sha"));
        assertEquals("http://www.w3.org/2001/XMLSchema#decimal", model.getField("sha").getDatatype());
        assertEquals("clr:SHA", model.getField("sha").getPath());
        assertFalse(model.getField("sha").isMultivalued());
        assertFalse(model.getField("sha").isAnalyzed());
        assertTrue(model.getField("sha").isOptional());
        assertTrue(model.getFields().containsKey("su"));
        assertEquals("clr:SU", model.getField("su").getPath());
        assertEquals("http://www.w3.org/2001/XMLSchema#decimal", model.getField("su").getDatatype());
        assertFalse(model.getField("su").isMultivalued());
        assertFalse(model.getField("su").isAnalyzed());
        assertTrue(model.getField("su").isOptional());
        assertTrue(model.getFields().containsKey("address"));
        assertEquals("mnx:hasAddress", model.getField("address").getPath());
        assertEquals("http://www.w3.org/2001/XMLSchema#anyURI", model.getField("address").getDatatype());
        assertFalse(model.getField("address").isMultivalued());
        assertFalse(model.getField("address").isAnalyzed());
        assertTrue(model.getField("address").isOptional());
        assertTrue(model.getField("address").isFilterDeleted());
        assertEquals(3, model.getField("address").getSubfields().size());
        assertEquals("clr:deptCode", model.getField("address").getSubfield("deptCode").getPath());
        assertEquals("http://www.w3.org/2001/XMLSchema#string", model.getField("address").getSubfield("deptCode").getDatatype());
        assertFalse(model.getField("address").getSubfield("deptCode").isMultivalued());
        assertFalse(model.getField("address").getSubfield("deptCode").isAnalyzed());
        assertTrue(model.getField("address").getSubfield("deptCode").isOptional());
        assertEquals("mnx:street1", model.getField("address").getSubfield("street1").getPath());
        assertEquals("http://www.w3.org/2001/XMLSchema#string", model.getField("address").getSubfield("street1").getDatatype());
        assertFalse(model.getField("address").getSubfield("street1").isMultivalued());
        assertTrue(model.getField("address").getSubfield("street1").isAnalyzed());
        assertTrue(model.getField("address").getSubfield("street1").isOptional());
        assertEquals(500, model.getField("address").getSubfield("street1").getIgnoreAbove().intValue());
        assertEquals("simple", model.getField("address").getSubfield("street1").getAnalyzer());
        assertEquals("mnx:street2", model.getField("address").getSubfield("street2").getPath());
        assertEquals("http://www.w3.org/2001/XMLSchema#string", model.getField("address").getSubfield("street2").getDatatype());
        assertTrue(model.getField("address").getSubfield("street2").isMultivalued());
        assertTrue(model.getField("address").getSubfield("street2").isAnalyzed());
        assertTrue(model.getField("address").getSubfield("street2").isOptional());
        assertEquals(500, model.getField("address").getSubfield("street2").getIgnoreAbove().intValue());
        assertEquals("standard", model.getField("address").getSubfield("street2").getAnalyzer());
    }

    @Test
    public void testLoadIndexingModel2() throws EngineException, LoadException {
        SPARQLRestAPI.getTripleStore().load(testModelFile2, "", Loader.format.TURTLE_FORMAT);

        // Test the loading of the indexing model
        IndexingManager.getInstance().extractModels();

        // Test the indexing model from the data file
        IndexingModel model = IndexingManager.getInstance().getModel("https://schema.org/Person");

        assertNotNull(model);

        assertEquals("https://schema.org/Person", model.getClassUri());
        // assertEquals(2, model.getPrefixes().size());
        assertEquals(3, model.getFields().size());
        assertTrue(model.getFields().containsKey("firstName"));
        assertEquals("http://www.w3.org/2001/XMLSchema#string", model.getField("firstName").getDatatype());
        assertEquals("foaf:firstName", model.getField("firstName").getPath());
        assertFalse(model.getField("firstName").isMultivalued());
        assertFalse(model.getField("firstName").isAnalyzed());
        assertFalse(model.getField("firstName").isOptional());
        assertTrue(model.getFields().containsKey("lastName"));
        assertEquals("foaf:lastName", model.getField("lastName").getPath());
        assertEquals("http://www.w3.org/2001/XMLSchema#string", model.getField("lastName").getDatatype());
        assertFalse(model.getField("lastName").isMultivalued());
        assertFalse(model.getField("lastName").isAnalyzed());
        assertFalse(model.getField("lastName").isOptional());
        assertTrue(model.getFields().containsKey("address"));
        assertEquals("vcard:adr", model.getField("address").getPath());
        assertEquals("http://www.w3.org/2001/XMLSchema#anyURI", model.getField("address").getDatatype());
        assertTrue(model.getField("address").isMultivalued());
        assertFalse(model.getField("address").isAnalyzed());
        assertTrue(model.getField("address").isOptional());
        assertTrue(model.getField("address").isFilterDeleted());

        assertEquals(4, model.getField("address").getSubfields().size());
        assertTrue(model.getField("address").getSubfields().containsKey("country"));
        assertEquals("vcard:country-name", model.getField("address").getSubfield("country").getPath());
        assertEquals("http://www.w3.org/2001/XMLSchema#string", model.getField("address").getSubfield("country").getDatatype());
        assertFalse(model.getField("address").getSubfield("country").isMultivalued());
        assertFalse(model.getField("address").getSubfield("country").isAnalyzed());
        assertFalse(model.getField("address").getSubfield("country").isOptional());
        assertTrue(model.getField("address").getSubfields().containsKey("locality"));
        assertEquals("vcard:locality", model.getField("address").getSubfield("locality").getPath());
        assertEquals("http://www.w3.org/2001/XMLSchema#string", model.getField("address").getSubfield("locality").getDatatype());
        assertFalse(model.getField("address").getSubfield("locality").isMultivalued());
        assertFalse(model.getField("address").getSubfield("locality").isAnalyzed());
        assertFalse(model.getField("address").getSubfield("locality").isOptional());
        assertTrue(model.getField("address").getSubfields().containsKey("postalCode"));
        assertEquals("vcard:postal-code", model.getField("address").getSubfield("postalCode").getPath());
        assertEquals("http://www.w3.org/2001/XMLSchema#string", model.getField("address").getSubfield("postalCode").getDatatype());
        assertFalse(model.getField("address").getSubfield("postalCode").isMultivalued());
        assertFalse(model.getField("address").getSubfield("postalCode").isAnalyzed());
        assertFalse(model.getField("address").getSubfield("postalCode").isOptional());
        assertTrue(model.getField("address").getSubfields().containsKey("streetAddress"));
        assertEquals("vcard:street-address", model.getField("address").getSubfield("streetAddress").getPath());
        assertEquals("http://www.w3.org/2001/XMLSchema#string", model.getField("address").getSubfield("streetAddress").getDatatype());
        assertFalse(model.getField("address").getSubfield("streetAddress").isMultivalued());
        assertTrue(model.getField("address").getSubfield("streetAddress").isAnalyzed());
        assertTrue(model.getField("address").getSubfield("streetAddress").isOptional());
        assertEquals("simple", model.getField("address").getSubfield("streetAddress").getAnalyzer());
        assertEquals(500, model.getField("address").getSubfield("streetAddress").getIgnoreAbove().intValue());
    }

    /**
     * Tests the generation of mappings from JSON data including a complex field with subfields "address" and nested subfields.
     *
     * @throws LoadException
     */
    @Test
    public void jsonMappingsTest1() throws LoadException {
        SPARQLRestAPI.getTripleStore().load(testModelFile2, "", Loader.format.TURTLE_FORMAT);
        SPARQLRestAPI.getTripleStore().load(testModelDataFile2, "", Loader.format.TURTLE_FORMAT);

        // Test the loading of the indexing model
        IndexingManager.getInstance().extractModels();

        JSONArray allMappings = ESMappingManager.getInstance().getMappings("https://schema.org/Person");

        assertNotNull(allMappings);
        allMappings.forEach(o -> ((JSONObject) o).remove("uri")); // Emulating the removal of the URI done when the mappings are returned by the API
        allMappings.forEach(o -> assertTrue(
                JSONUtils.jsonObjectsAreEquals((JSONObject) o, new JSONObject("{\"firstName\":\"John\",\"lastName\":\"Doe\",\"address\":[{\"country\":\"United States\",\"streetAddress\":\"123 Main Street\",\"postalCode\":\"10001\",\"locality\":\"New York\"}]}"))
                || JSONUtils.jsonObjectsAreEquals((JSONObject) o, new JSONObject("{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"address\":[{\"country\":\"United States\",\"streetAddress\":\"124 Main Street\",\"postalCode\":\"10001\",\"locality\":\"New York\"}]}"))
                || JSONUtils.jsonObjectsAreEquals((JSONObject) o, new JSONObject("{\"firstName\":\"John\",\"lastName\":\"Smith\",\"address\":[{\"country\":\"France\",\"streetAddress\":\"3, rue du pont\",\"postalCode\":\"75001\",\"locality\":\"Paris\"}]}")
                ))
        );
        //assertTrue(JSONUtils.jsonArraysAreEquals(new JSONArray("[{\"firstName\":\"John\",\"lastName\":\"Doe\",\"address\":[{\"country\":\"United States\",\"streetAddress\":\"123 Main Street\",\"postalCode\":\"10001\",\"locality\":\"New York\"}]},{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"address\":[{\"country\":\"United States\",\"streetAddress\":\"124 Main Street\",\"postalCode\":\"10001\",\"locality\":\"New York\"}]},{\"firstName\":\"John\",\"lastName\":\"Smith\",\"address\":[{\"country\":\"France\",\"streetAddress\":\"3, rue du pont\",\"postalCode\":\"75001\",\"locality\":\"Paris\"}]}]"), allMappings));
    }

    /**
     * Tests the generation of mappings from JSON data including a complex field with subfields "author" and nested subfields extracted from a class that has its own model.
     *
     * @throws LoadException
     */
    @Test
    public void jsonMappingsTest2() throws LoadException {
        SPARQLRestAPI.getTripleStore().load(testModelFile2, Loader.format.TURTLE_FORMAT);
        SPARQLRestAPI.getTripleStore().load(testModelDataFile2, Loader.format.TURTLE_FORMAT);

        // Test the loading of the indexing model
        IndexingManager.getInstance().extractModels();

        JSONArray allMappings = ESMappingManager.getInstance().getMappings("https://schema.org/Article");
        allMappings.forEach(o -> ((JSONObject) o).remove("uri")); // Emulating the removal of the URI done when the mappings are returned by the API

        assertNotNull(allMappings);
        assertEquals(3, allMappings.length());
        ArrayList<JSONObject> resultObjectList = new ArrayList<>();
        allMappings.forEach(o -> resultObjectList.add((JSONObject) o));

        assertTrue(resultObjectList.stream().anyMatch(o -> JSONUtils.jsonObjectsAreEquals(o, (new JSONObject("{\"author\":[{\"firstName\":\"John\",\"lastName\":\"Doe\"}],\"about\":[\"dog\",\"cute\"],\"abstract\":[\"Article 1\"]}")))));
        assertTrue(resultObjectList.stream().anyMatch(o -> JSONUtils.jsonObjectsAreEquals(o, (new JSONObject("{\"author\":[{\"firstName\":\"Jane\",\"lastName\":\"Doe\"}],\"about\":[\"cute\",\"cat\"],\"abstract\":[\"Article 2\"]}")))));
        assertTrue(resultObjectList.stream().anyMatch(o -> JSONUtils.jsonObjectsAreEquals(o, (new JSONObject("{\"author\":[{\"firstName\":\"John\",\"lastName\":\"Doe\"},{\"firstName\":\"John\",\"lastName\":\"Smith\"}],\"about\":[\"dog\",\"cute\",\"cat\"],\"abstract\":[\"Article 3\"]}")))));
    }
}
