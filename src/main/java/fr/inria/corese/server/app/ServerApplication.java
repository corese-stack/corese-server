package fr.inria.corese.server.app;

import fr.inria.corese.server.config.Role;
import fr.inria.corese.server.config.ServerConfig;
import fr.inria.corese.server.http.handler.GraphStoreHandler;
import fr.inria.corese.server.http.handler.SPARQLQueryHandler;
import fr.inria.corese.server.http.handler.SPARQLUpdateHandler;
import fr.inria.corese.server.http.middleware.AuthMiddleware;
import fr.inria.corese.server.http.middleware.CorsMiddleware;
import fr.inria.corese.server.service.GraphStoreService;
import fr.inria.corese.server.service.ScheduledDumpService;
import fr.inria.corese.server.service.SparqlExecutionService;
import fr.inria.corese.server.store.CoreseTripleStoreManager;
import fr.inria.corese.server.store.TripleStoreManager;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Corese-Server entry point — wire-only layer.
 *
 */
public class ServerApplication {

    private static final Logger log = LoggerFactory.getLogger(ServerApplication.class);
    private static final Instant startTime = Instant.now();
    private static final String VERSION = readVersion();

    private ServerApplication() {
    }

    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {

        // Configuration
        ServerConfig config = ServerConfig.fromEnv();
        log.info("Starting Corese-Server {} on port {} (auth={}, dumpInterval={}s)",
                VERSION, config.port(), config.authEnabled(), config.dumpIntervalSeconds());

        // Store
        TripleStoreManager store = new CoreseTripleStoreManager(config);
        store.loadInitialData();

        // Services
        SparqlExecutionService sparqlService = new SparqlExecutionService(store);
        GraphStoreService graphService = new GraphStoreService(store);
        ScheduledDumpService dumpService = new ScheduledDumpService(store, config);
        dumpService.start();

        // Handlers
        SPARQLQueryHandler queryHandler = new SPARQLQueryHandler(sparqlService);
        SPARQLUpdateHandler updateHandler = new SPARQLUpdateHandler(sparqlService);
        GraphStoreHandler graphHandler = new GraphStoreHandler(graphService);

        // Middlewares
        CorsMiddleware corsMiddleware = new CorsMiddleware(VERSION);
        AuthMiddleware authMiddleware = new AuthMiddleware(config);

        Javalin app = Javalin.create(cfg -> {

            cfg.jetty.port = config.port();

            cfg.routes.before(corsMiddleware::apply);
            cfg.routes.beforeMatched(authMiddleware::handle);

            // SPARQL 1.1 Protocol / query
            cfg.routes.get("/sparql", queryHandler::handle, Role.ANONYMOUS);

            cfg.routes.post("/sparql", ctx -> {
                String ct = ctx.contentType() != null ? ctx.contentType() : "";
                if (ct.contains("sparql-update")
                        || ctx.formParam("update") != null
                        || ctx.queryParam("update") != null) {
                    updateHandler.handle(ctx);
                } else {
                    queryHandler.handle(ctx);
                }
            }, Role.ANONYMOUS);

            // Graph Store HTTP Protocol
            cfg.routes.head("/rdf-graph-store", graphHandler::head, Role.ANONYMOUS);
            cfg.routes.get("/rdf-graph-store", graphHandler::get, Role.ANONYMOUS);
            cfg.routes.put("/rdf-graph-store", graphHandler::put, Role.USER_W);
            cfg.routes.post("/rdf-graph-store", graphHandler::post, Role.USER_W);
            cfg.routes.patch ("/rdf-graph-store", graphHandler::patch,  Role.USER_W);
            cfg.routes.delete("/rdf-graph-store", graphHandler::delete, Role.ADMIN);

            //Health
            cfg.routes.get("/health", ctx ->
                            ctx.json(Map.of("status", "UP", "version", VERSION)),
                    Role.ANONYMOUS);

            // Status
            cfg.routes.get("/status", ctx ->
                            ctx.json(Map.of(
                                    "status", "UP",
                                    "version", VERSION,
                                    "uptime", Duration.between(startTime, Instant.now()).toString(),
                                    "startedAt", startTime.toString(),
                                    "triples", store.tripleCount(),
                                    "graphs", store.graphCount(),
                                    "dumps", dumpService.getDumpCount(),
                                    "dumpInterval", config.dumpIntervalSeconds()
                            )),
                    Role.ANONYMOUS);
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown — stopping scheduled dump...");
            dumpService.stop();
            log.info("Shutdown — final dump to {}...", config.dumpPath());
            store.dumpToFile(config.dumpPath());
            log.info("Shutdown complete");
        }, "corese-shutdown"));

        app.start();
        log.info("Corese-Server ready: http://localhost:{}/sparql", config.port());
    }

    /**
     * Read version from JAR manifest, then version.properties, then "dev".
     *
     * @return the implementation version string
     */
    private static String readVersion() {
        String version = ServerApplication.class.getPackage().getImplementationVersion();
        if (version != null) return version;
        try (var is = ServerApplication.class.getResourceAsStream("/version.properties")) {
            if (is != null) {
                var props = new java.util.Properties();
                props.load(is);
                String v = props.getProperty("version");
                if (v != null && !v.startsWith("@") && !v.startsWith("$")) return v;
            }
        } catch (Exception e) {
            log.debug("Could not read version.properties: {}", e.getMessage());
        }
        return "dev";
    }
}
