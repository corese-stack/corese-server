package fr.inria.corese.server.webservice;

import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.server.elasticsearch.model.IndexingManager;
import fr.inria.corese.server.elasticsearch.model.IndexingModel;
import org.junit.Test;

import static org.junit.Assert.*;

public class ESIndexingManagerTest {

    private static final String testDataFile = "src/test/resources/elasticsearch/indexingModelExample.trig";

    @Test
    public void testLoadIndexingModel() throws EngineException, LoadException {
        SPARQLRestAPI.getTripleStore().load(testDataFile, "");

        // Test the loading of the indexing model
        IndexingManager.getInstance().extractModels();

        // Test the indexing model from the data file
        IndexingModel model = IndexingManager.getInstance().getModel("http://data.clairsienne.com/ontologies/2019/12/clr-patrimoine#Lot");

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
}
