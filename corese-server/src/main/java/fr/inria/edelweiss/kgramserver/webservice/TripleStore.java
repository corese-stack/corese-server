package fr.inria.edelweiss.kgramserver.webservice;

import fr.inria.acacia.corese.exceptions.EngineException;
import fr.inria.acacia.corese.triple.parser.Dataset;
import fr.inria.edelweiss.kgram.core.Mappings;
import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgraph.core.GraphStore;
import fr.inria.edelweiss.kgraph.query.QueryProcess;
import fr.inria.edelweiss.kgraph.rule.RuleEngine;
import fr.inria.edelweiss.kgtool.load.Load;
import fr.inria.edelweiss.kgtool.load.LoadException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2015
 *
 */
public class TripleStore {

    private static Logger logger = LogManager.getLogger(TripleStore.class);
    GraphStore graph = GraphStore.create(false);
    QueryProcess exec = QueryProcess.create(graph);
    boolean rdfs = false, owl = false;

    TripleStore(boolean rdfs, boolean owl) {
       this(rdfs, owl, true);
    }
    
    // 
    TripleStore(boolean rdfs, boolean owl, boolean b) {
        graph = GraphStore.create(rdfs);
        init(graph);
        exec  = QueryProcess.create(graph, b);
        this.owl = owl;
    }
    
    TripleStore(GraphStore g){
        graph = g;
        init(g);
        exec = QueryProcess.create(g);
    }
    
    TripleStore(GraphStore g, boolean b){
        graph = g;
        init(g);
        //exec = QueryProcess.create(g, b);
    }
    
    void init(GraphStore g){
    }
    
    void finish(boolean b){
        exec = QueryProcess.create(graph, true);
        init(b);
    }
    
    public String toString(){
        return graph.toString();
    }
    
    QueryProcess getQueryProcess(){
        return exec;
    }
    
    GraphStore getGraph(){
        return graph;
    }
    
    void setGraph(GraphStore g){
        graph = g;
    }
    
     void setGraph(Graph g){
         if (g instanceof GraphStore){
            graph = (GraphStore) g;
         }
    }
    
    int getMode(){
        return exec.getMode();
    }
    
    void setMode(int m){
        exec.setMode(m);                              
    }
    
    void setOWL(boolean b){
        owl = b;
    }
    
    void init(boolean b) {
       
        if (b){
            exec.setMode(QueryProcess.SERVER_MODE);
        }

        if (rdfs) {
            logger.info("Endpoint successfully reset with RDFS entailments.");
        }

        if (owl) {
            RuleEngine re = RuleEngine.create(graph);
            re.setProfile(RuleEngine.OWL_RL);
            graph.addEngine(re);
            if (owl) {
                logger.info("Endpoint successfully reset with OWL RL entailments.");
            }

        }
        
    }
    
    
    void load(String[] load) {
        Load ld = Load.create(graph);
        for (String f : load) {
            try {
                logger.info("Load: " + f);
                //ld.loadWE(f, f, Load.TURTLE_FORMAT);
                ld.parse(f, Load.TURTLE_FORMAT);
            } catch (LoadException ex) {
                LogManager.getLogger(SPARQLRestAPI.class.getName()).log(Level.ERROR, "", ex);
            }
        }
    }
    
    void load(String path, String src) throws LoadException{
        Load ld = Load.create(graph);
        ld.parse(path, src, Load.TURTLE_FORMAT);
    }
    
    Mappings query(String query, Dataset ds) throws EngineException{
        return exec.query(query, ds);
    }

    Mappings query(String query) throws EngineException{
        return exec.query(query);
    }
}
