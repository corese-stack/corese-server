package fr.inria.corese.server.elasticsearch.model;

import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.server.webservice.SPARQLRestAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class IndexingManager {
    private static final Logger logger = LoggerFactory.getLogger(IndexingManager.class);
    private static IndexingManager instance = null;

    private Map<String, IndexingModel> models;

    private IndexingManager() {
        models = new HashMap<>();
    }

    public static IndexingManager getInstance() {
        if (instance == null) {
            instance = new IndexingManager();
        }
        return instance;
    }

    public IndexingModel getModel(String classUri) {
        return models.get(classUri);
    }

    public void extractModels() {
        // Extract models from the server
        String query = generateQuery();
        QueryProcess exec = SPARQLRestAPI.getQueryProcess();
        try {
            Mappings result = exec.query(query);
            for(Mapping mapping : result.getMappingList()) {
                logger.info("Mapping: " + mapping);
                String classUri = mapping.getValue("?class").stringValue();
                if(!models.containsKey(classUri)) {
                    models.put(classUri, new IndexingModel(classUri));
                }
                String fieldLabel = mapping.getValue("?fLabel").stringValue();
                String fieldDatatype = mapping.getValue("?dt").stringValue();
                String fieldPath = mapping.getValue("?path").stringValue();

 /*               if(mapping.getValue("?prefixes") != null) {
                    String prefixes = mapping.getValue("?prefixes").stringValue();
                    String[] prefixArray = prefixes.split(",");
                    for(String prefix : prefixArray) {
                        String[] parts = prefix.split(" : <");
                        String prLabel = parts[0];
                        String prVal = parts[1].substring(0, parts[1].length() - 1);
                        models.get(classUri).addPrefix(prLabel, prVal);
                    }
                }
*/
                if(! models.get(classUri).getFields().containsKey(fieldLabel)) {
                    IndexingField field = new IndexingField(fieldLabel, fieldDatatype, fieldPath);
                    models.get(classUri).addField(fieldLabel, field);
                }

                if(mapping.getValue("?multi") != null) {
                    boolean multivalued = mapping.getValue("?multi").stringValue().equals("true");
                    models.get(classUri).getField(fieldLabel).setMultivalued(multivalued);
                }

                if(mapping.getValue("?analyzed") != null) {
                    boolean analyzed = mapping.getValue("?analyzed").stringValue().equals("true");
                    models.get(classUri).getField(fieldLabel).setAnalyzed(analyzed);
                }

                if(mapping.getValue("?optional") != null) {
                    boolean optional = mapping.getValue("?optional").stringValue().equals("true");
                    models.get(classUri).getField(fieldLabel).setOptional(optional);
                }

                if(mapping.getValue("?analyzer") != null) {
                    String analyzer = mapping.getValue("?analyzer").stringValue();
                    models.get(classUri).getField(fieldLabel).setAnalyzer(analyzer);
                }

                if(mapping.getValue("?ignore") != null) {
                    Integer ignore = Integer.parseInt(mapping.getValue("?ignore").stringValue());
                    models.get(classUri).getField(fieldLabel).setIgnoreAbove(ignore);
                }

                if(mapping.getValue("?filterDeleted") != null) {
                    boolean filter = mapping.getValue("?filterDeleted").stringValue().equals("true");
                    models.get(classUri).getField(fieldLabel).setFilterDeleted(filter);
                }

                if(mapping.getValue("?subfield") != null) {
                    String subfieldLabel = mapping.getValue("?subfieldLabel").stringValue();
                    String subfieldDatatype = mapping.getValue("?subfieldDatatype").stringValue();
                    String subfieldDataPath = mapping.getValue("?subfieldDataPath").stringValue();
                    models.get(classUri).getField(fieldLabel).addSubfield(subfieldLabel, new IndexingField(subfieldLabel, subfieldDatatype, subfieldDataPath));

                    if(mapping.getValue("?subfieldMulti") != null) {
                        boolean subfieldMultivalued = mapping.getValue("?subfieldMulti").stringValue().equals("true");
                        models.get(classUri).getField(fieldLabel).getSubfield(subfieldLabel).setMultivalued(subfieldMultivalued);
                    }

                    if(mapping.getValue("?subfieldAnalyzed") != null) {
                        boolean subfieldAnalyzed = mapping.getValue("?subfieldAnalyzed").stringValue().equals("true");
                        models.get(classUri).getField(fieldLabel).getSubfield(subfieldLabel).setAnalyzed(subfieldAnalyzed);
                    }

                    if(mapping.getValue("?subfieldOptional") != null) {
                        boolean subfieldOptional = mapping.getValue("?subfieldOptional").stringValue().equals("true");
                        models.get(classUri).getField(fieldLabel).getSubfield(subfieldLabel).setOptional(subfieldOptional);
                    }

                    if(mapping.getValue("?subfieldAnalyzer") != null) {
                        String subfieldAnalyzer = mapping.getValue("?subfieldAnalyzer").stringValue();
                        models.get(classUri).getField(fieldLabel).getSubfield(subfieldLabel).setAnalyzer(subfieldAnalyzer);
                    }

                    if(mapping.getValue("?subfieldIgnore") != null) {
                        Integer subfieldIgnore = Integer.parseInt(mapping.getValue("?subfieldIgnore").stringValue());
                        models.get(classUri).getField(fieldLabel).getSubfield(subfieldLabel).setIgnoreAbove(subfieldIgnore);
                    }
                }
            }

        } catch (EngineException e) {
            e.printStackTrace();
        }
    }

    private String generateQuery() {
        StringBuilder sb = new StringBuilder();
        sb.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
        sb.append("PREFIX im: <http://ns.mnemotix.com/ontologies/2019/1/indexing-model#>\n");
        sb.append("PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n");
        sb.append("SELECT ?im ?class\n");
 //       sb.append("(GROUP_CONCAT(CONCAT(?prLabel, \" : <\", ?prVal, \">\") ; SEPARATOR = \",\") AS ?prefixes)\n");
        sb.append("        ?fLabel\n");
        sb.append("        ?dt\n");
        sb.append("        ?path\n");
        sb.append("        ?multi\n");
        sb.append("        ?analyzed\n");
        sb.append("        ?optional\n");
        sb.append("        ?analyzer\n");
        sb.append("        ?ignore\n");
        sb.append("        ?filterDeleted\n");
        sb.append("        ?subfield\n");
        sb.append("        ?subfieldLabel\n");
        sb.append("        ?subfieldDatatype\n");
        sb.append("        ?subfieldDataPath\n");
        sb.append("        ?subfieldMulti\n");
        sb.append("        ?subfieldAnalyzed\n");
        sb.append("        ?subfieldAnalyzer\n");
        sb.append("        ?subfieldOptional\n");
        sb.append("        ?subfieldIgnore\n");
        sb.append("         WHERE {\n");
        sb.append("    ?im a im:IndexingModel ; im:indexingModelOf ?class ; im:field ?field .\n");
        sb.append("    ?field rdfs:label ?fLabel ; im:fieldDatatype ?dt ; im:dataPath ?path .\n");
        sb.append("    OPTIONAL {\n");
        sb.append("        ?im im:prefix ?pref .\n");
        sb.append("        ?pref rdfs:label ?prLabel ; im:value ?prVal .\n");
        sb.append("    }\n");
        sb.append("    OPTIONAL {?field im:multivalued ?multi }\n");
        sb.append("    OPTIONAL {?field im:analyzed ?analyzed }\n");
        sb.append("    OPTIONAL {?field im:optional ?optional }\n");
        sb.append("    OPTIONAL {?field im:analyzer ?analyzer }\n");
        sb.append("    OPTIONAL {?field im:ignore_above ?ignore }\n");
        sb.append("    OPTIONAL {?field im:filterDeleted ?filterDeleted }\n");
        sb.append("    OPTIONAL {\n");
        sb.append("        ?field im:subfield ?subfield .\n");
        sb.append("        ?subfield rdfs:label ?subfieldLabel ; im:fieldDatatype ?subfieldDatatype ; im:dataPath ?subfieldDataPath .\n");
        sb.append("        OPTIONAL { ?subfield im:multivalued ?subfieldMulti }\n");
        sb.append("        OPTIONAL { ?subfield im:analyzed ?subfieldAnalyzed }\n");
        sb.append("        OPTIONAL { ?subfield im:analyzer ?subfieldAnalyzer }\n");
        sb.append("        OPTIONAL { ?subfield im:optional ?subfieldOptional }\n");
        sb.append("        OPTIONAL { ?subfield im:ignore_above ?subfieldIgnore }\n");
        sb.append("    }\n");
        sb.append("}\n");
        sb.append("GROUP BY ?im ?class ?fLabel ?dt ?path ?multi ?analyzed ?optional ?analyzer ?ignore ?filterDeleted ?subfield ?subfieldLabel ?subfieldDatatype ?subfieldDataPath ?subfieldMulti ?subfieldAnalyzed ?subfieldAnalyzer ?subfieldOptional ?subfieldIgnore\n");


        return sb.toString();
    }

}
