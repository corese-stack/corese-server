package fr.inria.edelweiss.kgramserver.webservice;

import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.cg.datatype.DatatypeMap;
import fr.inria.acacia.corese.exceptions.EngineException;
import fr.inria.acacia.corese.triple.parser.Context;
import fr.inria.acacia.corese.triple.parser.Dataset;
import fr.inria.corese.kgtool.workflow.Data;
import fr.inria.corese.kgtool.workflow.ResultProcess;
import fr.inria.corese.kgtool.workflow.SemanticWorkflow;
import fr.inria.corese.kgtool.workflow.WorkflowParser;
import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgraph.core.GraphStore;
import fr.inria.edelweiss.kgtool.load.LoadException;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * Workflow Engine, "independent" of Web server
 * 
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class TransformerEngine {
    
    private static Logger logger = LogManager.getLogger(Transformer.class);

    // TripleStore RDF Graph  
    GraphStore graph;
    // Profile RDF graph: profile, server and workflow definitions
    GraphStore profile;
    // Context shared by Workflow and transformations
    private Context context;
    // Web service parameters
    Param param;
    
    private boolean debug = false;

    /**
     * 
     * @param graph    RDF graph to be processed
     * @param profile  RDF graph specifies workflow to be executed on graph
     * @param param    service Param and Context
     * 
     * Workflow URI/bnode IDatatype dt = param.getContext().get(Context.STL_WORKFLOW) // st:workflow
     * Workflow node = profile.getNode(dt)
     */
    public TransformerEngine(GraphStore graph, GraphStore profile, Param param) {
        this.graph = graph;
        this.profile = profile;
        this.param = param;
        init();
    }
    
    void init(){
        setContext(create(param));
        complete(graph, profile, context);
    }

    /**
     * Create and run a Workflow
     * Workflow result is data.stringValue();
     */
    public Data process() throws EngineException, LoadException {
        Dataset ds = createDataset(param.getFrom(), param.getNamed());
        SemanticWorkflow sw = workflow(getContext(), ds, profile, param.getQuery());
        //sw.setDebug(isDebug());
        if (isDebug()) {
            logger.info("Run workwlow");
            graph.setVerbose(true);
        }
        Data data = sw.process(new Data(graph));
        return data;
    }
          

    /**
     * Create a Workflow to process service 
     * If there is no explicit workflow
     * specification, i.e no st:workflow [ ] create a Workflow with
     * query/transform.
     */
    SemanticWorkflow workflow(Context context, Dataset dataset, Graph profile, String query) throws LoadException {
        SemanticWorkflow wp = new SemanticWorkflow();
        wp.setContext(context);
        wp.setDataset(dataset);
        wp.setLog(true);
        IDatatype swdt = context.get(Context.STL_WORKFLOW);
        String transform = context.getTransform();
        if (swdt != null) {
            // there is a workflow
            if (query != null) {
                logger.warn("Workflow skip query: " + query);
            }
            if (isDebug()) {
                logger.info("Parse workflow");
            }
            WorkflowParser parser = new WorkflowParser(wp, profile);
            parser.parse(profile.getNode(swdt));
        } else if (query != null) {
            if (transform == null) {
                // emulate sparql endpoint
                wp.addQueryMapping(query);
                wp.add(new ResultProcess());
                return wp;
            } else {
                // select where return Graph Mappings
                wp.addQueryGraph(query);
            }
        }
        defaultTransform(wp, transform);
        return wp;
    }

    /**
     * If transform = null and workflow does not end with transform: use
     * st:sparql as default transform
     */
    void defaultTransform(SemanticWorkflow wp, String transform) {
        boolean isDefault = false;
        if (transform == null && !wp.hasTransformation()) {
            isDefault = true;
            transform = fr.inria.edelweiss.kgtool.transform.Transformer.SPARQL;
        }
        if (transform != null) {
            wp.addTemplate(transform, isDefault);
            wp.getContext().setTransform(transform);
            wp.getContext().set(Context.STL_DEFAULT, true);
        }
    }
    
    
    Context create(Param par) {
        Context ctx= par.createContext();        
        complete(ctx, par);         
        return ctx;
    }
    
    Context complete(Context c, Param par){
        if (par.isAjax()){
            c.setProtocol(Context.STL_AJAX);
            c.export(Context.STL_PROTOCOL, c.get(Context.STL_PROTOCOL));
        }
        return c;
    }

    void complete(GraphStore graph, GraphStore profile, Context context) {
        Graph cg = graph.getNamedGraph(Context.STL_CONTEXT);
        if (cg != null) {
            context.set(Context.STL_CONTEXT,    DatatypeMap.createObject(Context.STL_CONTEXT, cg));
        }
        context.set(Context.STL_DATASET,        DatatypeMap.createObject(Context.STL_DATASET, graph));
        context.set(Context.STL_SERVER_PROFILE, DatatypeMap.createObject(Context.STL_SERVER_PROFILE, profile));
    }

    private Dataset createDataset(List<String> defaultGraphUris, List<String> namedGraphUris) {
        return createDataset(defaultGraphUris, namedGraphUris, null);
    }

    private Dataset createDataset(List<String> defaultGraphUris, List<String> namedGraphUris, Context c) {
        if (c != null
                || ((defaultGraphUris != null) && (!defaultGraphUris.isEmpty()))
                || ((namedGraphUris != null) && (!namedGraphUris.isEmpty()))) {
            Dataset ds = Dataset.instance(defaultGraphUris, namedGraphUris);
            ds.setContext(c);
            return ds;
        } else {
            return null;
        }
    }
    
    /**
     * @return the context
     */
    public Context getContext() {
        return context;
    }

    /**
     * @param context the context to set
     */
    public void setContext(Context context) {
        this.context = context;
    }
    
     /**
     * @return the debug
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * @param debug the debug to set
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

}
