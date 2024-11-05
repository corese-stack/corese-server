package fr.inria.corese.server.webservice;

import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.kgram.api.core.DatatypeValue;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.kgram.core.Exp;
import fr.inria.corese.core.kgram.core.Mapping;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.kgram.core.Query;
import static fr.inria.corese.server.webservice.SPARQLRestAPI.ERROR;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import fr.inria.corese.core.util.HTTPHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;

/**
 *
 */
@Path("compute/{name}")
/**
 * Service for extended SPARQL service clause where result 
 * Mappings is computed by program not by graph matching
 * Service body is a values clause that defines input and result variables
 * service <http://corese.inria.fr/compute/test> { values (?x ?res) { (1 undef) }}
 */
public class ServiceCompute  {
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(ServiceCompute.class);
    
    @POST
    @Produces({ResultFormat.SPARQL_RESULTS_XML})
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response compute(@Context HttpServletRequest request,
            @PathParam("name") String name, 
            @DefaultValue("") @FormParam("query") String query, 
            @FormParam("access") String access,            
            String message) {
        String q = getQuery(query, message);
        QueryProcess exec = getQueryProcess();
        try {
            Query qq = exec.compile(q);
            Mappings map = compute(request, qq, name);
            ResultFormat rf = ResultFormat.create(map);
            String res = rf.toString();

            return Response.status(200)
                    .header(HTTPHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .header("Content-Type", rf.getContentType())
                    .entity(res).build();
        
        }  
        catch (EngineException |NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            logger.error("Error in service call", ex);
            return Response.status(ERROR).header(HTTPHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*").entity("Error while querying the remote KGRAM engine").build();
        }
    }
    
    QueryProcess getQueryProcess() {
        return SPARQLRestAPI.getTripleStore().getQueryProcess();
    }
    
    
    /**
     * call method name(request, q)
     */
    Mappings compute(HttpServletRequest request, Query q, String name) 
            throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class[] types = new Class[2];
        types[0] = HttpServletRequest.class;
        types[1] = Query.class;
        Method m = getClass().getMethod(name, types);
        Object obj = m.invoke(this, request, q);
        if (obj instanceof Mappings) {
            return (Mappings) obj;
        }
        throw new IllegalArgumentException(obj.getClass().getName());
    }
    
    
    public Mappings compile(HttpServletRequest request, Query q) throws EngineException {
        IDatatype dt = DatatypeMap.newInstance(q.getAST().toString());
        Mappings map = getResult();
        map.get(0).setNode(map.getSelect().get(0), dt);
        return map;
    }
    
    Mappings getResult() throws EngineException {
        String tmp = "select (rdf:nil as ?output) where {}";
        Mappings map = getQueryProcess().query(tmp);
        return map;
    }

    
    
    /**
     * query = values (?x ?res) {(1 undef)(3 undef) } 
     */
    public Mappings test(HttpServletRequest request, Query q) {
        //trace(request);
        Exp exp = q.getBody().first();
        Exp res = q.getBody().last();
        Mappings map = exp.getMappings();
        map.setQuery(q);
        // default result variable
        Node qn = q.getSelectNode("?res");
        
        if (res != null && res.getMappings()!=null && !res.getNodeList().isEmpty()) {
            // any declared result variable:
            qn = res.getNodeList().get(0);
        }
        
        for (Mapping m : map) {
            m.initValues();
            DatatypeValue x = m.getValue("?x");
            DatatypeValue y = m.getValue("?y");
            m.addNode(qn, DatatypeMap.newInstance(request.getRequestURL().toString()));
        }
        
        return map;
    }
    
    String getQuery(String query, String message) {
        return (query.isEmpty()) ? message : query;
    }
    
    
}
