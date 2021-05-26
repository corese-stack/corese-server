package fr.inria.corese.server.webservice;

import fr.inria.corese.compiler.federate.FederateVisitor;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.kgram.core.Mappings;
import fr.inria.corese.sparql.api.IDatatype;
import fr.inria.corese.sparql.api.ResultFormatDef;
import fr.inria.corese.sparql.datatype.DatatypeMap;
import fr.inria.corese.sparql.triple.function.term.Binding;
import fr.inria.corese.sparql.triple.parser.Dataset;
import fr.inria.corese.sparql.triple.parser.Context;
import fr.inria.corese.sparql.triple.parser.URLParam;
import fr.inria.corese.sparql.triple.parser.Access.Level;
import fr.inria.corese.sparql.triple.parser.Access;
import fr.inria.corese.sparql.triple.parser.NSManager;
import fr.inria.corese.sparql.triple.parser.URLServer;
import fr.inria.corese.core.transform.Transformer;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.Response;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 */
public class SPARQLResult implements ResultFormatDef, URLParam    {
    
    static private final Logger logger = LogManager.getLogger(SPARQLResult.class);
    private static final String headerAccept = "Access-Control-Allow-Origin";
    private static final String ERROR_ENDPOINT = "Error while querying Corese SPARQL endpoint";
    private static final String OPER = "operation";    
    private static final String URL = "url";   
    static final int ERROR = 500;
    private static SPARQLResult singleton;
    
    private HttpServletRequest request;
    QuerySolverVisitorServer visitor;
    
    static {
        setSingleton(new SPARQLResult());                
    }
    
    
    SPARQLResult(){}
    
    SPARQLResult(HttpServletRequest request) {
        setRequest(request);
    }

    static TripleStore getTripleStore() {
        return SPARQLRestAPI.getTripleStore();
    }
    
    static TripleStore getTripleStore (String name) {
           if (name == null) {
               return getTripleStore();
           }
           return Manager.getEndpoint(name);
    }
    
    
    /**
     * Specific endpoint function where format can be specified by format parameter
     * Content-Type is set according to format parameter and what is returned by ResultFormat.
     */
    public Response getResultFormat(String name, String oper, List<String> uri, List<String> param, List<String> mode,
            String query, String access, 
            List<String> defaut, List<String> named,
            String format, int type, List<String> transform) { 
           
        try {  
            logger.info("Endpoint URL: " + getRequest().getRequestURL());
            if (query == null)
                throw new Exception("No query");

            beforeRequest(getRequest(), query);
            Dataset ds = createDataset(getRequest(), defaut, named, access);
                                  
            beforeParameter(ds, oper, uri, param, mode);
            Mappings map = getTripleStore(name).query(getRequest(), query, ds);  
            afterParameter(ds, map);
            
            ResultFormat rf = getFormat(map, ds, format, type, transform);            
            String res = rf.toString();
                       
            ResponseBuilder rb = Response.status(Response.Status.OK).header(headerAccept, "*");
            
            if (format != null) {
                // real content type of result, possibly different from @Produces
                rb = rb.header("Content-Type", rf.getContentType());
            }
            Response resp = rb.entity(res).build();
            
            afterRequest(getRequest(), resp, query, map, res);  
                                 
            return resp;
        } catch (Exception ex) {
            logger.error(ERROR_ENDPOINT, ex);
            return Response.status(ERROR).header(headerAccept, "*").entity(ERROR_ENDPOINT).build();
        }
    }
    
        /**
     * Creates a Dataset based on a set of default or named graph URIs. 
     * For *strong* SPARQL compliance, use dataset.complete() before returning the dataset.
     *
     * @return a dataset
     */    
    Dataset createDataset(HttpServletRequest request, List<String> defaut, List<String> named, String access) {
        Dataset ds = null;
        if (((defaut != null) && (!defaut.isEmpty())) 
                || ((named != null) && (!named.isEmpty()))) {
            ds = Dataset.instance(defaut, named);
        } 
        else {
            ds = new Dataset();
        }
        boolean b = SPARQLRestAPI.hasKey(access);
        if (b) {
            System.out.println("has key access");
        }
        Level level = Access.getQueryAccessLevel(true, b);
        ds.getCreateContext().setLevel(level);
        ds.getContext().setURI(URL, request.getRequestURL().toString());
        return ds;
    }
    
    
    /**
     * Parameters of sparql service URL: 
     * http://corese.fr/sparql?mode=debug&param=format:rdf;test:12
     * 
     * http://corese.fr/d2kab/sparql
     * http://corese.fr/d2kab/federate
     * name = d2kab ; oper = sparql|federate
     * parameter recorded in context and as ldscript global variable
     */
    Dataset beforeParameter(Dataset ds, String oper, List<String> uri, List<String> param, List<String> mode) {
        if (oper != null) {
            ds.getContext().set(OPER, oper);
            List<String> federation = new ArrayList<>();
            switch (oper) {
                
                case FEDERATE:
                    // From SPARQLService: var name is bound to d2kab
                    // URL = http://corese.inria.fr/d2kab/federate
                    // sparql query processed as a federated query on list of endpoints
                    // From SPARQL endpoint (alternative) mode and uri are bound
                    // http://corese.inria.fr/sparql?mode=federate&uri=http://ns.inria.fr/federation/d2kab
                    mode = leverage(mode);
                    //uri  = leverage(uri);
                    // declare federate mode for TripleStore query()
                    mode.add(FEDERATE);
                    // federation URL defined in /webapp/data/demo/fedprofile.ttl
                    //uri.add(ds.getContext().get(URL).getLabel());
                    federation.add(ds.getContext().get(URL).getLabel());
                    defineFederation(ds, federation);
                    break;
                    
                case SPARQL:
                    // URL = http://corese.inria.fr/id/sparql
                    // when id is a federation: union of query results of endpoint of id federation
                    // otherwise query triple store with name=id
                    String surl = ds.getContext().get(URL).getLabel();
                    String furl = surl;
                    
                    if (FederateVisitor.getFederation(furl) == null) {
                        furl = surl.replace("/sparql", "/federate");
                    }
                    
                    if (FederateVisitor.getFederation(furl) != null) {
                        // federation is defined 
                        mode = leverage(mode);
                        //uri = leverage(uri);
                        mode.add(FEDERATE);
                        mode.add(SPARQL);
                        // record the name of the federation
                        //uri.add(furl);
                        federation.add(furl);
                        defineFederation(ds, federation);
                    }
                    break;
                    
                // default:
                // other operations considered as sparql endpoint
                // with server name if any  
                default:
                    context(ds);
            }
        }
        
        if (uri!=null && !uri.isEmpty()) {
            // list of URI given as parameter uri= 
            ds.getContext().set(URI, DatatypeMap.listResource(uri));
            //ds.setUriList(uri);
        }
        
        if (param != null) {
            for (String kw : param) {
                mode(ds, PARAM, decode(kw));
            }
        }
        
        if (mode != null) {
            for (String kw : mode) {
                mode(ds, MODE, decode(kw));
            }
        }
        
        beforeParameter(ds);
        
        return ds;
    }
    
    /**
     * urlprofile.ttl may predefine parameters for endpoint URL eg /psparql
     */
    void context(Dataset ds) {
        Context ct = ds.getContext();
        IDatatype dt = Profile.getProfile().getContext().get(ct.get(URL).getLabel());
        
        if (dt != null) {
            
            for (IDatatype pair : dt) {
                String key = pair.get(0).getLabel();
                IDatatype val = pair.get(1);
                if (key.equals(MODE)) {
                    mode(ds, key, val.getLabel());
                }
                else {
                    ct.add(key, val);
                }
            }
            System.out.println("Context:\n" + ct);
        }
    }
    
    void defineFederation(Dataset ds, List<String> federation) {
        ds.setUriList(federation);
        ds.getContext().set(FEDERATION, DatatypeMap.listResource(federation));
    }
    
    String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            return value;
        }
    }
          
    List<String> leverage(List<String> name) {
        return (name == null) ? new ArrayList<>() : name;
    }
    
    /**
     * Record dataset from named in context for documentation purpose
     */
    void beforeParameter(Dataset ds) {
        IDatatype from = ds.getFromList();
        if (from.size() > 0) {
            ds.getContext().set(DEFAULT_GRAPH, from);
        }
        IDatatype named = ds.getNamedList();
        if (named.size() > 0) {
            ds.getContext().set(NAMED_GRAPH, named);
        }
    }   
    
    /**
     * name:  param | mode
     * value: debug | trace.
     */
    void mode(Dataset ds, String name, String value) {
        if (value.contains(";")) {
            for (String val : value.split(";")) {
                basicMode(ds, name, val);
            }
        }
        else {
            basicMode(ds, name, value);
        }
    }

    void basicMode(Dataset ds, String name, String value) {
        ds.getContext().add(name, value);

        switch (name) {
            case MODE:
                switch (value) {
                    case DEBUG:
                        ds.getContext().setDebug(true);
                    // continue

                    default:
                        ds.getContext().set(value, true);
                        break;
                }
                break;

            case PARAM:
                URLServer.decode(ds.getContext(), value);
                break;
        }
    }
       
    
    void afterParameter(Dataset ds, Mappings map) {
        if (ds.getContext().hasValue(TRACE)) {
            System.out.println("SPARQL endpoint");
            System.out.println(map.getQuery().getAST());
            System.out.println(map.toString(false, true, 10));
        }
        // draft for testing
        if (ds.getContext().hasValue(FORMAT)) {
            ResultFormat ft = ResultFormat.create(map, ds.getContext().get(FORMAT).getLabel());
            System.out.println(ft);
        }
    }
    
    QuerySolverVisitorServer getVisitor() {
        return visitor;
    }
    
    SPARQLResult setVisitor(QuerySolverVisitorServer vis) {
        visitor = vis;
        return this;
    }
    
    /**
     * Visitor call LDScript event @beforeRequest @public function 
     * profile.ttl must load function definitions, 
     * e.g. <demo/system/event.rq>
     * 
     */
    void beforeRequest(HttpServletRequest request, String query) {
        getVisitor().beforeRequest(request, query);
    }
    
    void afterRequest(HttpServletRequest request, String query, Mappings map) {
        getVisitor().afterRequest(request, query, map);
    }
    
    void afterRequest(HttpServletRequest request, Response resp, String query, Mappings map, String res) {
        getVisitor().afterRequest(request, resp, query, map, res);
    }
    
         
    ResultFormat getFormat(Mappings map, Dataset ds, String format, int type, List<String> transformList) {
        // predefined parameter associated to URL in urlparameter.ttl
        transformList = getValue(ds.getContext(), TRANSFORM, transformList);
        
        if (transformList == null || transformList.isEmpty()) {
            return getFormatSimple(map, ds, format, type);
        } else {
            return getFormatTransform( map,  ds,  format,  type, prepare(ds.getContext(), transformList));           
        }
    }
    
    /**
     * Post process query result map with transformation(s)
     * Return either 
     * a) when mode=link : query result with link url to transformation result document
     * b) otherwise transformation result
     */
    ResultFormat getFormatTransform(Mappings map, Dataset ds, String format, int type, List<String> transformList) {
            logger.info("Transform: " + transformList);
            
            boolean link = ds.getContext().hasAnyValue(LINK, LINK_REST);           
            ResultFormat std ;
            
            if (link) {
                // prepare (and return) std result with link to transform
                // map will record link url of transform in function getFormatTransformList
                // result format will be generated when returning HTTP result
                std = getFormatSimple(map, ds, format, type);
            }
            else {
                // return transform result 
                // record std result in href document in case transform generate link href
                int mytype = (type==ResultFormat.HTML_FORMAT) ? ResultFormat.UNDEF_FORMAT : type;
                std = getFormatSimple(map, ds, format, mytype);
                String url = TripleStore.document(std.toString(), "std");
                // record url of std result document for transform
                ds.getContext().add(Context.STL_LINK, DatatypeMap.newResource(url));
                logger.info("Query result in: " + url);
            }
            
            Optional<ResultFormat> res = getFormatTransformList(map, ds, format, type, transformList);
            if (res.isPresent()) {
                // no link: return transformation result 
                return res.get();
            }
            
            // link: return query result
            return std;
    }
    
    /**
     * Process transformations
     * When mode=link, add in map query result link url to transformation result document
     * and return empty 
     * Otherwise return result of (first) transformation
     */
    Optional<ResultFormat> getFormatTransformList(Mappings map, Dataset ds, String format, int type, List<String> transformList) {
        ResultFormat fst = null;

        for (String transform : transformList) {
            ResultFormat res = getFormatTransform(map, ds, format, type, transform);
            if (fst == null) {
                fst = res;
            }
            if (ds.getContext().hasValue(TRACE)) {
                logger.info(transform);
                logger.info(res);
            }

            if (ds.getContext().hasAnyValue(LINK, LINK_REST)) {
                // mode=link
                // save transformation result in document and return URL of document in map result link
                String url = TripleStore.document(res.toString(), getName(transform));
                // PRAGMA: map record link url. It will be considered 
                // by ResultFormat std in function getFormatTransform above
                map.addLink(url);
                logger.info("Transformation result in: " + url);
            } else {
                // no link: return result of first transformation
                return Optional.of(res);
            }
        }
        
        if (ds.getContext().hasValue(LINK_REST)) {
            // return result of first transformation (it may have generated links to other transformations)
            return Optional.of(fst);
        }
        else {
            // query result will be returned with link url to transformation result
            return Optional.empty();
        }
    }
    
    ResultFormat getFormatTransform(Mappings map, Dataset ds, String format, int type, String transform) {
        ResultFormat ft;
        if (type == UNDEF_FORMAT) {
            ft = ResultFormat.create(map, format, transform).init(ds);
        } else {
            ft = ResultFormat.create(map, type, transform).init(ds);
        }
        if (map.getBinding()!=null && ft.getBind()==null) {
            // share binding environment with transformer
            ft.setBind((Binding)map.getBinding());
        }
        return ft;
    }
    
    ResultFormat getFormatSimple(Mappings map, Dataset ds, String format, int type) {
        if (type == UNDEF_FORMAT) {
            return ResultFormat.create(map, format);
        } else {
            return ResultFormat.create(map, type);
        }
    }
    
    /**
     * predefined parameter associated to URL in urlparameter.ttl
     */
    List<String> getValue(Context ct, String name, List<String> value) {
        if (value != null && !value.isEmpty()) {
            return value;
        }
        IDatatype dt = ct.get(name);
        if (dt == null) {
            return null;
        }
        return DatatypeMap.toStringList(dt);
    }
    
    /**
     * trans;trans -> list of trans
     * st:all -> st:xml st:json
     */
    List<String> prepare(Context c, List<String> transformList) {
        List<String> list = new ArrayList<>();
        
        for (String name : transformList) {
            if (name.contains(";")) {
                for (String key : name.split(";")) {
                    prepare(c, key, list);
                }
            }
            else {
                prepare(c, name, list);
            }
        }

        return list;
    }
    
    void prepare(Context c, String name, List<String> list) {
        name = c.nsm().toNamespace(name);
        List<String> alist = Transformer.getFormatList(name);
        if (alist == null) {
            list.add(name);
        } else {
            list.addAll(alist);
        }
    }
    
   
    
    String getName(String transform) {
        if (transform.contains("#")) {
            return transform.substring(1+transform.indexOf("#"));
        }
        return transform.substring(1+transform.lastIndexOf("/"));
    }
    
    public static SPARQLResult getSingleton() {
        return singleton;
    }

    public static void setSingleton(SPARQLResult aSingleton) {
        singleton = aSingleton;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }
    
}