package fr.inria.corese.server.webservice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.print.JSOND3Format;
import fr.inria.corese.core.print.JSONFormat;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.kgram.core.Eval;
import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.sparql.api.ResultFormatDef;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.parser.Dataset;
import fr.inria.corese.core.sparql.triple.parser.NSManager;
import fr.inria.corese.core.sparql.triple.parser.URLParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * KGRAM SPARQL endpoint exposed as a rest web service.
 * <p>
 * The engine can be remotely initialized, populated with an RDF file, and
 * queried through SPARQL requests.
 *
 * @author Eric TOGUEM, eric.toguem@uy1.uninet.cm
 * @author Alban Gaignard, alban.gaignard@cnrs.fr
 * @author Olivier Corby
 */
@Path("sparql")
public class SPARQLRestAPI implements ResultFormatDef, URLParam {
    private static final String ERROR_ENDPOINT = "Error while querying Corese.Core.Sparql endpoint";
    private static final String HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String HEADER_CONTENT_TYPE = "Content-type";
    private static final String TEXT_PLAIN = "text/plain";

    static final String SPARQL_RESULTS_XML = ResultFormat.SPARQL_RESULTS_XML;
    static final String SPARQL_RESULTS_JSON = ResultFormat.SPARQL_RESULTS_JSON;
    static final String SPARQL_RESULTS_CSV = ResultFormat.SPARQL_RESULTS_CSV;
    static final String SPARQL_RESULTS_TSV = ResultFormat.SPARQL_RESULTS_TSV;
    static final String SPARQL_RESULTS_MD = ResultFormat.SPARQL_RESULTS_MD;
    static final String SPARQL_QUERY = ResultFormat.SPARQL_QUERY;
    static final String SPARQL_UPDATE_QUERY = "application/sparql-update";

    static final String XML = ResultFormat.XML;
    static final String RDF_XML = ResultFormat.RDF_XML;
    static final String TURTLE = ResultFormat.TURTLE;
    static final String TURTLE_TEXT = ResultFormat.TURTLE_TEXT;
    static final String JSON_LD = ResultFormat.JSON_LD;
    static final String JSON = ResultFormat.JSON;
    static final String TRIG = ResultFormat.TRIG;
    static final String TRIG_TEXT = ResultFormat.TRIG_TEXT;
    static final String NT_TEXT = ResultFormat.NT_TEXT;
    static final String N_TRIPLES = ResultFormat.N_TRIPLES;
    static final String N_QUADS = ResultFormat.N_QUADS;
    static final String TEXT = ResultFormat.TEXT;
    static final String HTML = ResultFormat.HTML;

    // Profiles
    private final String CN10_SHA = "https://www.w3.org/TR/rdf-canon";
    private final String CN10_SHA256 = "https://www.w3.org/TR/rdf-canon#sha-256";
    private final String CN10_SHA384 = "https://www.w3.org/TR/rdf-canon#sha-384";

    public static final String PROFILE_DEFAULT = "profile.ttl";
    public static final String DEFAULT = NSManager.STL + "default";

    private static final String URL = "url";
    private static final String OPER = "operation";

    static final int ERROR = 500;

    private static boolean isDebug = false;
    private static boolean isDetail = false;
    // set true to prevent update/load
    static boolean isProtected = !true;
    // true when Ajax
    static boolean isAjax = true;

    static String localProfile;

    // default sparql endpoint
    static TripleStore store = new TripleStore(false, false);

    private static Profile mprofile;

    private static final Logger logger = LogManager.getLogger(SPARQLRestAPI.class);
    private static String key;

    static {
    }

    QuerySolverVisitorServer visitor;

    public SPARQLRestAPI() {
        setVisitor(QuerySolverVisitorServer.create(createEval()));
    }

    /**
     * Current graph is SPARQL endpoint graph.
     */
    static Eval createEval() {
        QueryProcess exec = QueryProcess.create(getTripleStore().getGraph());
        try {
            return exec.getCreateEval();
        } catch (EngineException ex) {
            logger.error(ex.getMessage());
        }
        return null;
    }

    QueryProcess getQueryProcess() {
        return getTripleStore().getQueryProcess();
    }

    // default sparql endpoint
    static TripleStore getTripleStore() {
        return store;
    }

    // Named sparql endpoint
    static TripleStore getTripleStore(String name) {
        if (name == null) {
            return getTripleStore();
        }
        return Manager.getEndpoint(name);
    }

    QuerySolverVisitorServer getVisitor() {
        return visitor;
    }

    void setVisitor(QuerySolverVisitorServer vis) {
        visitor = vis;
    }

    public Response initRDF() {
        return initRDF("false", "false", "false", null, "false");
    }

    /**
     * This webservice is used to define (and reset) the endpoint.
     */
    @POST
    @Path("/reset")
    public Response initRDF(
            @DefaultValue("false") @FormParam("owlrl") String owlrl,
            @DefaultValue("false") @FormParam("entailments") String entailments,
            @DefaultValue("false") @FormParam("load") String load,
            @FormParam("profile") String profile,
            @DefaultValue("false") @FormParam("localhost") String localhost) {
        boolean ent = entailments.equals("true");
        boolean owl = owlrl.equals("true");
        boolean ld = load.equals("true");
        localProfile = profile;
        // option -init propertyFile may declare db storage path
        // with property STORAGE=path
        // DatasetManager create appropriate DataManager for db storage
        DatasetManagerServer man = new DatasetManagerServer().init();
        Manager.getManager().setDatasetManager(man);
        // create default sparql endpoint
        store = new TripleStore(ent, owl);
        // default db storage DataManager is for sparql endpoint
        store.setDataManager(man.getDataManager());

        init(localhost.equals("true"));
        if (ld) {
            // load data from st:default or st:user service profile if any
            Manager.getManager().init(store);
        }
        store.init(isProtected);
        setVisitor(QuerySolverVisitorServer.create(createEval()));
        getVisitor().initServer(EmbeddedJettyServer.BASE_URI);
        init();
        return Response.status(200).header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*").entity("Endpoint reset").build();
    }

    void init() {
        if (getKey() == null) {
            setKey(genkey());
        }
    }

    String genkey() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    // access key gives special access level (RESTRICTED vs PUBLIC)
    static boolean hasKey(HttpServletRequest request, String access) {
        EventManager.getSingleton().getHostMap();
        request.getRemoteHost();
        return hasKey(access);
    }

    static boolean hasKey(String access) {
        return access != null && getKey() != null && getKey().equals(access);
    }

    void init(boolean localhost) {
        mprofile = new Profile(localhost);
        mprofile.setProtect(isProtected);
        Profile.setProfile(mprofile);
        if (localProfile != null) {
            localProfile = NSManager.toURI(localProfile);
        }
        mprofile.initServer(PROFILE_DEFAULT, localProfile);
    }

    /**
     * This webservice is used to load a dataset to the endpoint. Therefore, if we
     * have many files for our datastore, we could load them by recursivelly calling
     * this webservice
     */
    @POST
    @Path("/load")
    public Response loadRDF(
            @FormParam("remote_path") String remotePath,
            @FormParam("source") String source) {
        logger.traceEntry("loadRDF");
        String output = "File Uploaded";
        if (source != null) {
            if (source.isEmpty()) {
                source = null;
            } else if (!source.startsWith("http://")) {
                source = "http://" + source;
            }
        }

        if (remotePath == null) {
            String error = "Null remote path";
            logger.error(error);
            return Response.status(404).header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*").entity(error).build();
        }

        logger.debug(remotePath);

        try {
            // path with extension : use extension
            // path with no extension : load as turtle
            // use case: rdf: is in Turtle
            if (!getTripleStore().isProtect()) {
                getTripleStore().load(remotePath, source);
            }
        } catch (LoadException ex) {
            logger.error(ex);
            return Response.status(404).header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*").entity(output).build();
        }

        return Response.status(200).header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*").entity(output).build();
    }

    @GET
    @Path("/debug")
    public Response setDebug(@QueryParam("value") String debug, @QueryParam("detail") String detail) {
        if (debug != null) {
            isDebug = debug.equals("true");
        }
        if (detail != null) {
            isDetail = detail.equals("true");
        }
        return Response.status(200).header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .entity("debug: " + isDebug + " ; " + "detail: " + isDetail).build();
    }

    // ----------------------------------------------------
    // SPARQL QUERY - SELECT and ASK with HTTP GET
    // ----------------------------------------------------

    /**
     * Default function for HTTP GET:
     * This function is called when there is no header Accept
     * Additional HTTP parameter format: eg application/sparql-results+xml
     * Return Content-Type according to what is really returned
     * 
     * PathParam name may come from SPARQLService
     * with URL http://corese.inria.fr/name/sparql
     * In this case, name is the name of the service (eg ai4eu) which manages the
     * RDF graph to query
     * instead of default SPARQL endpoint
     * param oper may specify oper=federate or oper=sparql in federation mode
     * .
     */
    @GET
    @Produces({ SPARQL_RESULTS_XML, XML })
    public Response getSPARQLXMLForGet(@jakarta.ws.rs.core.Context HttpServletRequest request,
            // name of server from SPARQLService
            @PathParam("name") String name,
            // name of federation from SPARQLService
            @PathParam("oper") String oper,
            @QueryParam("query") String query,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            @QueryParam("format") String format,
            @QueryParam("transform") List<String> transform,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        String ft = request.getHeader("Accept");
        if (ft.contains(SPARQL_RESULTS_XML) || ft.contains(XML)) {
            // Explicit @Produces, skip format parameter
            format = SPARQL_RESULTS_XML;
        }
        // default function called in absence of http header accept
        // in this case, the value of format is taken into account
        // if there is header accept, the value of format is overloaded by header accept
        // if there is no header and no format, default format is chosen (xml or turtle)
        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, format,
                UNDEF_FORMAT, transform);
    }

    public Response getResultFormat(HttpServletRequest request,
            String name, String oper, List<String> uri, List<String> param, List<String> mode,
            String query, String access,
            List<String> defaut, List<String> named,
            String format, int type, List<String> transform) {
        return new SPARQLResult(request).setVisitor(getVisitor())
                .getResultFormat(name, oper, uri, param, mode, query, access, defaut, named, format, type, transform);
    }

    public Response getResultFormat(HttpServletRequest request,
            String name, String oper, List<String> uri, List<String> param, List<String> mode,
            String query, String access,
            List<String> defaut, List<String> named,
            int type) {
        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, null, type, null);
    }

    /**
     * parameter format=application/sparql-results+json
     * format may be null: return default format wrt map kind
     * template with format return format
     * template without format return text/plain
     * 
     */
    ResultFormat getResultFormat(Mappings map, String format) {
        return ResultFormat.create(map, format);
    }

    String getResult(Mappings map, String format) {
        return getResultFormat(map, format).toString();
    }

    /**
     * Get the profiles from the Accept header
     * 
     * @param accept The Accept header
     * @return The profiles
     */
    private ArrayList<String> getProfiles(String accept) {
        ArrayList<String> profiles = new ArrayList<>();
        String[] parts = accept.split(";");
        for (String part : parts) {
            if (part.contains("profile=")) {
                String[] profileParts = part.split("=");
                String[] profileUrls = profileParts[1].split(" ");

                for (String profileUrl : profileUrls) {
                    // Remove the quotes
                    profileUrl = profileUrl.replace("\"", "");

                    profiles.add(profileUrl);
                }
            }
        }
        return profiles;
    }

    @GET
    @Produces({ HTML })
    public Response getHTMLForGet(@jakarta.ws.rs.core.Context HttpServletRequest request,
            // name of server from SPARQLService
            @PathParam("name") String name,
            // name of federation from SPARQLService
            @PathParam("oper") String oper,
            @QueryParam("query") String query,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            @QueryParam("format") String format,
            @QueryParam("transform") List<String> transform,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        if ((query == null || query.isEmpty()) &&
                (mode == null || mode.isEmpty())) {
            query = "select * where {?s ?p ?o} limit 5";
            return new Transformer()
                    .queryGETHTML(
                            request,
                            oper,
                            fr.inria.corese.core.transform.Transformer.SPARQL,
                            null,
                            null,
                            null,
                            null,
                            format,
                            access,
                            query,
                            name,
                            null,
                            null,
                            defaut,
                            named);
        }
        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, null, HTML_FORMAT,
                transform);
    }

    @GET
    @Produces({ "text/plain" })
    public Response getPlainTextForGet(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @QueryParam("query") String query,
            @QueryParam("access") String access,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, TEXT_FORMAT);
    }

    @GET
    @Produces(SPARQL_RESULTS_JSON)
    public Response getTriplesJSONForGet(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @QueryParam("query") String query,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            @QueryParam("transform") List<String> transform,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, null, JSON_FORMAT,
                transform);
    }

    @GET
    @Produces(SPARQL_RESULTS_CSV)
    public Response getTriplesCSVForGet(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @QueryParam("query") String query,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, CSV_FORMAT);
    }

    @GET
    @Produces(SPARQL_RESULTS_TSV)
    public Response getTriplesTSVForGet(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @QueryParam("query") String query,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, TSV_FORMAT);
    }

    @GET
    @Produces(SPARQL_RESULTS_MD)
    public Response getTriplesMDForGet(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @QueryParam("query") String query,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, MARKDOWN_FORMAT);
    }

    // ----------------------------------------------------
    // SPARQL QUERY - DESCRIBE and CONSTRUCT with HTTP GET
    // ----------------------------------------------------

    @GET
    @Produces(RDF_XML)
    public Response getRDFGraphXMLForGet(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @QueryParam("query") String query,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, RDF_XML_FORMAT);
    }

    @GET
    @Produces({ TURTLE_TEXT, TURTLE, NT_TEXT })
    public Response getRDFGraphNTripleForGet(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @QueryParam("query") String query,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, TURTLE_FORMAT);
    }

    @GET
    @Produces({ TRIG_TEXT, TRIG })
    public Response getRDFGraphTrigForGet(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @QueryParam("query") String query,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, TRIG_FORMAT);
    }

    @GET
    @Produces({ JSON, JSON_LD })
    public Response getRDFGraphJsonLDForGet(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @QueryParam("query") String query,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, JSONLD_FORMAT);
    }

    @GET
    @Produces({ N_TRIPLES })
    public Response getRDFGraphNTriplesForGet(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @QueryParam("query") String query,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, NTRIPLES_FORMAT);
    }

    @GET
    @Produces({ N_QUADS })
    public Response getRDFGraphNQuadsForGet(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @QueryParam("query") String query,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        // Get the profiles from the Accept header
        ArrayList<String> profiles = getProfiles(request.getHeader("Accept"));

        for (String profile : profiles) {
            if (profile.equals(this.CN10_SHA) || profile.equals(this.CN10_SHA256)) {

                return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named,
                        RDFC10_FORMAT);
            }
            if (profile.equals(this.CN10_SHA384)) {

                return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named,
                        RDFC10_SHA384_FORMAT);
            }
        }

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, NQUADS_FORMAT);
    }

    // ----------------------------------------------------
    // SPARQL QUERY - SELECT and ASK with HTTP POST
    // ----------------------------------------------------

    String getQuery(String query, String message) {
        return (query == null || query.isEmpty()) ? message : query;
    }

    String getQuery(String query, String update, String message) {
        return (query.isEmpty()) ? getQuery(update, message) : query;
    }

    @POST
    @Produces({ SPARQL_RESULTS_XML, XML })
    @Consumes(SPARQL_QUERY)
    public Response getXMLForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @QueryParam("query") String query,
            @DefaultValue("") @QueryParam("update") String update,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            String message,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, XML_FORMAT);
    }

    @POST
    @Produces({ SPARQL_RESULTS_CSV })
    @Consumes(SPARQL_QUERY)
    public Response getCSVForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @QueryParam("query") String query,
            @DefaultValue("") @QueryParam("update") String update,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            String message,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, CSV_FORMAT);
    }

    @POST
    @Produces({ SPARQL_RESULTS_TSV })
    @Consumes(SPARQL_QUERY)
    public Response getTSVForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @QueryParam("query") String query,
            @DefaultValue("") @QueryParam("update") String update,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            String message,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, TSV_FORMAT);
    }

    @POST
    @Produces({ SPARQL_RESULTS_MD })
    @Consumes(SPARQL_QUERY)
    public Response getMDForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @QueryParam("query") String query,
            @DefaultValue("") @QueryParam("update") String update,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            String message,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, MARKDOWN_FORMAT);
    }

    @POST
    @Produces({ TURTLE, TURTLE_TEXT })
    @Consumes(SPARQL_QUERY)
    public Response getTurtleForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @QueryParam("query") String query,
            @DefaultValue("") @QueryParam("update") String update,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            String message,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, TURTLE_FORMAT);
    }

    @POST
    @Produces({ RDF_XML })
    @Consumes(SPARQL_QUERY)
    public Response getRDFXMLForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @QueryParam("query") String query,
            @DefaultValue("") @QueryParam("update") String update,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            String message,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, RDF_XML_FORMAT);
    }

    @POST
    @Produces({ TRIG })
    @Consumes(SPARQL_QUERY)
    public Response getTrigForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @QueryParam("query") String query,
            @DefaultValue("") @QueryParam("update") String update,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            String message,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, TRIG_FORMAT);
    }

    @POST
    @Produces({ JSON_LD })
    @Consumes(SPARQL_QUERY)
    public Response getJSONLDForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @QueryParam("query") String query,
            @DefaultValue("") @QueryParam("update") String update,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            String message,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, JSONLD_FORMAT);
    }

    @POST
    @Produces({ NT_TEXT, N_TRIPLES })
    @Consumes(SPARQL_QUERY)
    public Response getNTriplesForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @QueryParam("query") String query,
            @DefaultValue("") @QueryParam("update") String update,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            String message,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, NTRIPLES_FORMAT);
    }

    @POST
    @Produces({ N_QUADS })
    @Consumes(SPARQL_QUERY)
    public Response getNQuadsForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @QueryParam("query") String query,
            @DefaultValue("") @QueryParam("update") String update,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            String message,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, NQUADS_FORMAT);
    }

    @POST
    @Produces({ TEXT })
    @Consumes(SPARQL_QUERY)
    public Response getXMLForPostText(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @QueryParam("query") String query,
            @DefaultValue("") @QueryParam("update") String update,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            String message,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {

        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, TEXT_FORMAT);
    }

    @POST
    @Produces(SPARQL_RESULTS_JSON)
    @Consumes(SPARQL_QUERY)
    public Response getTriplesJSONForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @QueryParam("query") String query,
            @DefaultValue("") @QueryParam("update") String update,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri,
            String message) {
        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, JSON_FORMAT);
    }

    /**
     * Default POST function (ie when there is no header Accept)
     * SPARQL service clause executed here by corese
     */
    @POST
    @Produces({ SPARQL_RESULTS_XML, XML })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getTriplesXMLForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            // name of server with a specific rdf graph (SPARQLService)
            @PathParam("name") String name,
            // name of federation (SPARQLFederate)
            @PathParam("oper") String oper,
            @DefaultValue("") @FormParam("query") String query,
            @DefaultValue("") @FormParam("update") String update,
            @FormParam("access") String access,
            @FormParam("default-graph-uri") List<String> defaut,
            @FormParam("named-graph-uri") List<String> named,
            @FormParam("using-graph-uri") List<String> using,
            @FormParam("using-named-graph-uri") List<String> usingNamed,
            @FormParam("format") String format,
            @FormParam("transform") List<String> transform,
            @FormParam("param") List<String> param,
            @FormParam("mode") List<String> mode,
            @FormParam("uri") List<String> uri,
            String message) {
        String accept = request.getHeader("Accept");

        query = getQuery(query, update, message);

        if (accept.contains(SPARQL_RESULTS_XML) || accept.contains(XML)) {
            format = SPARQL_RESULTS_XML;
        }

        // dataset(defaut, using), dataset(named, usingNamed)
        return getResultFormat(request, name, oper, uri, param, mode, query,
                access, defaut, named, format, UNDEF_FORMAT, transform);
    }

    List<String> dataset(List<String> from, List<String> using) {
        if (from == null || from.isEmpty()) {
            return using;
        }
        return from;
    }

    @POST
    @Produces(HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getHTMLForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @FormParam("query") String query,
            @DefaultValue("") @FormParam("update") String update,
            @FormParam("access") String access,
            @FormParam("default-graph-uri") List<String> defaut,
            @FormParam("named-graph-uri") List<String> named,
            @FormParam("param") List<String> param,
            @FormParam("mode") List<String> mode,
            @FormParam("uri") List<String> uri,
            String message) {

        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, HTML_FORMAT);
    }

    @POST
    @Produces(TEXT)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getTriplesTEXTForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @FormParam("query") String query,
            @DefaultValue("") @FormParam("update") String update,
            @FormParam("access") String access,
            @FormParam("default-graph-uri") List<String> defaut,
            @FormParam("named-graph-uri") List<String> named,
            @FormParam("param") List<String> param,
            @FormParam("mode") List<String> mode,
            @FormParam("uri") List<String> uri,
            String message) {
        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, TEXT_FORMAT);
    }

    @POST
    @Produces(SPARQL_RESULTS_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getTriplesJSONForPostFormUrlEncoded(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @FormParam("query") String query,
            @DefaultValue("") @FormParam("update") String update,
            @FormParam("access") String access,
            @FormParam("transform") List<String> transform,
            @FormParam("default-graph-uri") List<String> defaut,
            @FormParam("named-graph-uri") List<String> named,
            @FormParam("param") List<String> param,
            @FormParam("mode") List<String> mode,
            @FormParam("uri") List<String> uri,
            String message) {

        query = getQuery(query, update, message);
        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, null, JSON_FORMAT,
                transform);
    }

    @POST
    @Produces(SPARQL_RESULTS_CSV)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getTriplesCSVForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @FormParam("query") String query,
            @DefaultValue("") @FormParam("update") String update,
            @FormParam("access") String access,
            @FormParam("transform") List<String> transform,
            @FormParam("default-graph-uri") List<String> defaut,
            @FormParam("named-graph-uri") List<String> named,
            @FormParam("param") List<String> param,
            @FormParam("mode") List<String> mode,
            @FormParam("uri") List<String> uri,
            String message) {

        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, null, CSV_FORMAT,
                transform);
    }

    @POST
    @Produces(SPARQL_RESULTS_TSV)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getTriplesTSVForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @FormParam("query") String query,
            @DefaultValue("") @FormParam("update") String update,
            @FormParam("access") String access,
            @FormParam("transform") List<String> transform,
            @FormParam("default-graph-uri") List<String> defaut,
            @FormParam("named-graph-uri") List<String> named,
            @FormParam("param") List<String> param,
            @FormParam("mode") List<String> mode,
            @FormParam("uri") List<String> uri,
            String message) {
        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, null, TSV_FORMAT,
                transform);
    }

    // ----------------------------------------------------
    // SPARQL QUERY - DESCRIBE and CONSTRUCT with HTTP POST
    // ----------------------------------------------------

    @POST
    @Produces(RDF_XML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getRDFGraphXMLForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @FormParam("query") String query,
            @DefaultValue("") @FormParam("update") String update,
            @FormParam("access") String access,
            @FormParam("default-graph-uri") List<String> defaut,
            @FormParam("named-graph-uri") List<String> named,
            @FormParam("param") List<String> param,
            @FormParam("mode") List<String> mode,
            @FormParam("uri") List<String> uri,
            String message) {

        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, RDF_XML_FORMAT);
    }

    @POST
    @Produces({ TURTLE_TEXT, TURTLE, NT_TEXT })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getRDFGraphNTripleForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @FormParam("query") String query,
            @DefaultValue("") @FormParam("update") String update,
            @FormParam("access") String access,
            @FormParam("default-graph-uri") List<String> defaut,
            @FormParam("named-graph-uri") List<String> named,
            @FormParam("param") List<String> param,
            @FormParam("mode") List<String> mode,
            @FormParam("uri") List<String> uri,
            String message) {

        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, TURTLE_FORMAT);
    }

    @POST
    @Produces({ SPARQL_RESULTS_MD })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getRDFGraphMDForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @FormParam("query") String query,
            @DefaultValue("") @FormParam("update") String update,
            @FormParam("access") String access,
            @FormParam("default-graph-uri") List<String> defaut,
            @FormParam("named-graph-uri") List<String> named,
            @FormParam("param") List<String> param,
            @FormParam("mode") List<String> mode,
            @FormParam("uri") List<String> uri,
            String message) {

        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, MARKDOWN_FORMAT);
    }

    @POST
    @Produces({ JSON, JSON_LD })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getRDFGraphJsonLDForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @FormParam("query") String query,
            @DefaultValue("") @FormParam("update") String update,
            @FormParam("access") String access,
            @FormParam("default-graph-uri") List<String> defaut,
            @FormParam("named-graph-uri") List<String> named,
            @FormParam("param") List<String> param,
            @FormParam("mode") List<String> mode,
            @FormParam("uri") List<String> uri,
            String message) {
        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, JSONLD_FORMAT);
    }

    @POST
    @Produces({ N_TRIPLES })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getRDFGraphNTriplesForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @FormParam("query") String query,
            @DefaultValue("") @FormParam("update") String update,
            @FormParam("access") String access,
            @FormParam("default-graph-uri") List<String> defaut,
            @FormParam("named-graph-uri") List<String> named,
            @FormParam("param") List<String> param,
            @FormParam("mode") List<String> mode,
            @FormParam("uri") List<String> uri,
            String message) {
        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, NTRIPLES_FORMAT);
    }

    @POST
    @Produces({ TRIG, TRIG_TEXT })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getRDFGraphTrigForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @FormParam("query") String query,
            @DefaultValue("") @FormParam("update") String update,
            @FormParam("access") String access,
            @FormParam("default-graph-uri") List<String> defaut,
            @FormParam("named-graph-uri") List<String> named,
            @FormParam("param") List<String> param,
            @FormParam("mode") List<String> mode,
            @FormParam("uri") List<String> uri,
            String message) {
        query = getQuery(query, update, message);

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, TRIG_FORMAT);
    }

    @POST
    @Produces({ N_QUADS })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response getRDFGraphNQuadsForPost(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @DefaultValue("") @FormParam("query") String query,
            @DefaultValue("") @FormParam("update") String update,
            @FormParam("access") String access,
            @FormParam("default-graph-uri") List<String> defaut,
            @FormParam("named-graph-uri") List<String> named,
            @FormParam("param") List<String> param,
            @FormParam("mode") List<String> mode,
            @FormParam("uri") List<String> uri,
            String message) {
        query = getQuery(query, update, message);

        // Get the profiles from the Accept header
        ArrayList<String> profiles = getProfiles(request.getHeader("Accept"));

        for (String profile : profiles) {
            if (profile.equals(this.CN10_SHA) || profile.equals(this.CN10_SHA256)) {

                return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named,
                        RDFC10_FORMAT);
            }
            if (profile.equals(this.CN10_SHA384)) {

                return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named,
                        RDFC10_SHA384_FORMAT);
            }
        }

        return getResultFormat(request, name, oper, uri, param, mode, query, access, defaut, named, NQUADS_FORMAT);
    }

    // ----------------------------------------------------
    // SPARQL UPDATE with HTTP POST
    // ----------------------------------------------------

    Dataset createDataset(HttpServletRequest request, List<String> defaut, List<String> named, String access) {
        return SPARQLResult.getSingleton().createDataset(request, defaut, named, access);
    }

    void beforeRequest(HttpServletRequest request, String query) {
        getVisitor().beforeRequest(request, query);
    }

    void afterRequest(HttpServletRequest request, Response resp, String query, Mappings map, String res) {
        getVisitor().afterRequest(request, resp, query, map, res);
    }

    @POST
    @Consumes(SPARQL_UPDATE_QUERY)
    @Produces(SPARQL_RESULTS_XML)
    public Response updateTriplesDirectXML(@jakarta.ws.rs.core.Context HttpServletRequest request,
            String message, // standard parameter, do not add @QueryParam()
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @QueryParam("access") String access,
            @QueryParam("using-graph-uri") List<String> defaut,
            @QueryParam("using-named-graph-uri") List<String> named,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {
        if (message != null) {
            return getResultFormat(request, name, oper, uri, param, mode, message, access, defaut, named, XML_FORMAT);
        } else {
            logger.warn("Null update query !");
            return Response.status(ERROR).header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*").entity(ERROR_ENDPOINT)
                    .build();
        }
    }

    @POST
    @Consumes(SPARQL_UPDATE_QUERY)
    @Produces(SPARQL_RESULTS_CSV)
    public Response updateTriplesDirectCSV(@jakarta.ws.rs.core.Context HttpServletRequest request,
            String message, // standard parameter, do not add @QueryParam()
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @QueryParam("access") String access,
            @QueryParam("using-graph-uri") List<String> defaut,
            @QueryParam("using-named-graph-uri") List<String> named,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {
        if (message != null) {
            return getResultFormat(request, name, oper, uri, param, mode, message, access, defaut, named, CSV_FORMAT);
        } else {
            logger.warn("Null update query !");
            return Response.status(ERROR).header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*").entity(ERROR_ENDPOINT)
                    .build();
        }
    }

    @POST
    @Consumes(SPARQL_UPDATE_QUERY)
    @Produces(SPARQL_RESULTS_TSV)
    public Response updateTriplesDirectTSV(@jakarta.ws.rs.core.Context HttpServletRequest request,
            String message, // standard parameter, do not add @QueryParam()
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @QueryParam("access") String access,
            @QueryParam("using-graph-uri") List<String> defaut,
            @QueryParam("using-named-graph-uri") List<String> named,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {
        if (message != null) {
            return getResultFormat(request, name, oper, uri, param, mode, message, access, defaut, named, TSV_FORMAT);
        } else {
            logger.warn("Null update query !");
            return Response.status(ERROR).header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*").entity(ERROR_ENDPOINT)
                    .build();
        }
    }

    @POST
    @Consumes(SPARQL_UPDATE_QUERY)
    @Produces(SPARQL_RESULTS_JSON)
    public Response updateTriplesDirectJSON(@jakarta.ws.rs.core.Context HttpServletRequest request,
            String message, // standard parameter, do not add @QueryParam()
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @QueryParam("access") String access,
            @QueryParam("using-graph-uri") List<String> defaut,
            @QueryParam("using-named-graph-uri") List<String> named,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {
        if (message != null) {
            return getResultFormat(request, name, oper, uri, param, mode, message, access, defaut, named, JSON_FORMAT);
        } else {
            logger.warn("Null update query !");
            return Response.status(ERROR).header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*").entity(ERROR_ENDPOINT)
                    .build();
        }
    }

    @POST
    @Consumes(SPARQL_UPDATE_QUERY)
    @Produces(SPARQL_RESULTS_MD)
    public Response updateTriplesDirectMD(@jakarta.ws.rs.core.Context HttpServletRequest request,
            String message, // standard parameter, do not add @QueryParam()
            @PathParam("name") String name,
            @PathParam("oper") String oper,
            @QueryParam("access") String access,
            @QueryParam("using-graph-uri") List<String> defaut,
            @QueryParam("using-named-graph-uri") List<String> named,
            @QueryParam("param") List<String> param,
            @QueryParam("mode") List<String> mode,
            @QueryParam("uri") List<String> uri) {
        if (message != null) {
            return getResultFormat(request, name, oper, uri, param, mode, message, access, defaut, named, MARKDOWN_FORMAT);
        } else {
            logger.warn("Null update query !");
            return Response.status(ERROR).header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*").entity(ERROR_ENDPOINT)
                    .build();
        }
    }

    @HEAD
    public Response getTriplesForHead(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @QueryParam("query") String query,
            @QueryParam("access") String access,
            @PathParam("name") String name,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named) {
        try {
            Mappings mp = getTripleStore(name).query(request, query, createDataset(request, defaut, named, access));
            return Response.status(mp.size() > 0 ? 200 : 400).header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .entity("Query has no response")
                    .build();
        } catch (Exception ex) {
            logger.error(ERROR_ENDPOINT, ex);
            return Response.status(ERROR).header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*").entity(ERROR_ENDPOINT)
                    .build();
        }
    }

    @GET
    @Path("/draw")
    @Produces(SPARQL_RESULTS_JSON)
    public Response getJSON(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @QueryParam("query") String query) {
        if (logger.isDebugEnabled())
            logger.debug("Querying: " + query);
        try {
            if (query == null)
                throw new Exception("No query");
            Mappings maps = getTripleStore().query(request, query);

            Graph g = (Graph) maps.getGraph();
            String mapsProvJson = "{ \"mappings\" : " + JSONFormat.create(maps).toString() + " , " + "\"d3\" : "
                    + JSOND3Format.create(g).toString() + " }";
            return Response.status(200).header("Access-Control-Allow-Origin", "*").entity(mapsProvJson).build();

        } catch (Exception ex) {
            logger.error(ERROR_ENDPOINT, ex);
            return Response.status(ERROR).header("Access-Control-Allow-Origin", "*").entity(ERROR_ENDPOINT).build();
        }
    }

    @GET
    @Path("/d3")
    @Produces(SPARQL_RESULTS_JSON)
    public Response getTriplesJSONForGetWithGraph(@jakarta.ws.rs.core.Context HttpServletRequest request,
            @QueryParam("query") String query,
            @QueryParam("access") String access,
            @QueryParam("default-graph-uri") List<String> defaut,
            @QueryParam("named-graph-uri") List<String> named) {
        try {
            if (logger.isDebugEnabled())
                logger.debug("Querying: " + query);
            if (query == null)
                throw new Exception("No query");
            Mappings m = getTripleStore().query(request, query, createDataset(request, defaut, named, access));

            String mapsD3 = "{ \"mappings\" : " + JSONFormat.create(m).toString() + " , " + "\"d3\" : "
                    + JSOND3Format.create((Graph) m.getGraph()).toString() + " }";
            return Response.status(200).header("Access-Control-Allow-Origin", "*").entity(mapsD3).build();

        } catch (Exception ex) {
            logger.error(ERROR_ENDPOINT, ex);
            return Response.status(ERROR).header(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*").entity(ERROR_ENDPOINT)
                    .build();
        }
    }

    /**
     * @return the key
     */
    public static String getKey() {
        return key;
    }

    /**
     * @param aKey the key to set
     */
    public static void setKey(String aKey) {
        key = aKey;
    }

}
