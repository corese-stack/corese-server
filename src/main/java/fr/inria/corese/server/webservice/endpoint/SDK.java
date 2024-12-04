package fr.inria.corese.server.webservice.endpoint;

import fr.inria.corese.core.GraphStore;
import fr.inria.corese.core.load.LoadException;
import java.io.IOException;

import fr.inria.corese.server.webservice.Param;
import fr.inria.corese.server.webservice.Profile;
import fr.inria.corese.server.webservice.TripleStore;
import org.apache.logging.log4j.Level;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2015
 *
 */
@Path("sdk")
public class SDK {
    
    
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response sdk(
            @QueryParam("query") String query, // SPARQL query
            @QueryParam("name")  String name,  // SPARQL query name (in webapp/query or path or URL)
            @QueryParam("value") String value) // values clause that may complement query           
     {
         GraphStore g;
        try {
            g = Profile.getProfile().loadServer("sdk.ttl");
        } catch (IOException | LoadException ex) {
            LogManager.getLogger(SDK.class.getName()).log(Level.ERROR, "", ex);
            g = GraphStore.create();
        }
         TripleStore st = new TripleStore(g);
         Param par = new Param("/sdk", null, null, null, Profile.getProfile().getQueryPath("sdk.rq"), null);
         par.setValue(value);
         
         return new Transformer().template(st, par);
    }
    
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response sdkPostMD(
            @FormDataParam("query") String query, // SPARQL query
            @FormDataParam("name")  String name,  // SPARQL query name (in webapp/query or path or URL)
            @FormDataParam("value") String value){ // values clause that may complement query          
         return this.sdk(query, name, value);
    }
}
