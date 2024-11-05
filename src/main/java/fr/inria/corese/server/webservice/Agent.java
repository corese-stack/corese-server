package fr.inria.corese.server.webservice;

import fr.inria.corese.core.util.HTTPHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * Agent service enables SPARQL endpoint to answer to http request
 * /agent?action=trace&param=abc
 * Request processed by LDScript event function @message
 * Draft event function in webapp/data/demo/system/event.rq
 * 
 * @author Olivier Corby, Wimmics INRIA I3S 2020
 */
@Path("agent")
public class Agent {
    
    QuerySolverVisitorServer visitor;

    public Agent() {
        visitor = QuerySolverVisitorServer.create(SPARQLRestAPI.createEval());
    }

  
    QuerySolverVisitorServer getVisitor() {
        return visitor;
    }
    
    /**
     * HTTP request processed by LDScript event function @message
     * /agent?action=trace&param=abc
     */
    @GET
    @Produces({"text/plain"})
    public Response message(@jakarta.ws.rs.core.Context HttpServletRequest request) {
        
        IDatatype dt = getVisitor().message(request);
        String mess = "undefined";
        if (dt != null) {
            mess = dt.getLabel();
        }
        return Response.status(200).header(HTTPHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*").entity(mess).build();
    }
    
    
    
    
}
