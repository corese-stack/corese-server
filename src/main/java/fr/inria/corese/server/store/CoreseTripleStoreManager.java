package fr.inria.corese.server.store;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.GraphStore;
import fr.inria.corese.core.api.Loader;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.core.sparql.api.ResultFormatDef;
import fr.inria.corese.server.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * corese-core implementation of {@link TripleStoreManager}.
 * <p>Owns a single {@link GraphStore} and a {@link ReentrantReadWriteLock}.
 * <p>Thread safety:
 * concurrent SELECT/ASK/CONSTRUCT use a shared read lock;
 * any INSERT/DELETE/UPDATE uses an exclusive write lock.
 */
public class CoreseTripleStoreManager implements TripleStoreManager {

    private static final Logger log = LoggerFactory.getLogger(CoreseTripleStoreManager.class);

    private final GraphStore graphStore;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ServerConfig config;

    /**
     * Creates a new empty triplestore. RDFS inference is disabled.
     *
     * @param config server configuration
     */
    public CoreseTripleStoreManager(ServerConfig config) {
        this.config = config;
        this.graphStore = GraphStore.create(false);
        log.info("Triplestore initialized (empty)");
    }

    // Query execution

    @Override
    public QueryProcess newQueryProcess() {
        return QueryProcess.create(graphStore);
    }

    // Graph operations

    @Override
    public Graph getNamedGraph(String graphUri) {
        return graphStore.getNamedGraph(graphUri);
    }

    @Override
    public void addToGraph(String graphUri, Graph graph) {
        writeLock();
        try {
            Graph existing = graphStore.getNamedGraph(graphUri);
            if (existing == null) {
                graphStore.setNamedGraph(graphUri, graph);
            } else {
                String turtle = ResultFormat
                        .create(graph, ResultFormatDef.format.TURTLE_FORMAT)
                        .toString();
                Load loader = Load.create(graphStore);
                loader.loadString(turtle, graphUri, Loader.format.TURTLE_FORMAT);
            }
        } catch (Exception e) {
            log.error("addToGraph({}) failed: {}", graphUri, e.getMessage(), e);
        } finally {
            writeUnlock();
        }
    }

    @Override
    public void replaceGraph(String graphUri, Graph graph) {
        writeLock();
        try {
            graphStore.setNamedGraph(graphUri, graph);
            log.debug("Replaced graph: {}", graphUri);
        } finally {
            writeUnlock();
        }
    }

    @Override
    public void deleteGraph(String graphUri) {
        writeLock();
        try {
            graphStore.getStore().remove(graphUri);
            log.debug("Deleted graph: {}", graphUri);
        } finally {
            writeUnlock();
        }
    }

    @Override
    public Collection<String> getGraphNames() {
        return graphStore.getNames();
    }

    @Override
    public void loadInitialData() {
        if (config.dataPath() == null || config.dataPath().isBlank()) {
            log.info("No CORESE_DATA_PATH — starting with empty store");
            return;
        }
        File file = new File(config.dataPath());
        if (!file.exists()) {
            log.warn("Data file not found: {}", config.dataPath());
            return;
        }
        writeLock();
        try {
            log.info("Loading data from: {}", config.dataPath());
            Load.create(graphStore).parse(config.dataPath());
            log.info("Loaded {} triple(s)", tripleCount());
        } catch (LoadException e) {
            log.error("Failed to load data: {}", e.getMessage(), e);
        } finally {
            writeUnlock();
        }
    }

    @Override
    public void dumpToFile(String path) {
        if (path == null || path.isBlank()) return;
        readLock();
        try {
            File file = new File(path);
            if (file.getParentFile() != null && !file.getParentFile().mkdirs()) {
                log.debug("Parent directories already exist or could not be created for: {}", path);
            }
            log.info("Dumping {} triple(s) to {}", tripleCount(), path);
            String dump = ResultFormat
                    .create(graphStore, ResultFormatDef.format.TURTLE_FORMAT)
                    .toString();
            try (FileWriter w = new FileWriter(file)) {
                w.write(dump);
            }
            log.info("Dump complete → {}", path);
        } catch (Exception e) {
            log.error("Dump failed: {}", e.getMessage(), e);
        } finally {
            readUnlock();
        }
    }

    @Override
    public void reset() {
        writeLock();
        try {
            graphStore.clean();
            log.info("Store reset");
        } finally {
            writeUnlock();
        }
    }


    @Override
    public void readLock() {
        lock.readLock().lock();
    }

    @Override
    public void readUnlock() {
        lock.readLock().unlock();
    }

    @Override
    public void writeLock() {
        lock.writeLock().lock();
    }

    @Override
    public void writeUnlock() {
        lock.writeLock().unlock();
    }


    @Override
    public long tripleCount() {
        long count = graphStore.size();
        for (String name : graphStore.getNames()) {
            Graph g = graphStore.getNamedGraph(name);
            if (g != null) count += g.size();
        }
        return count;
    }

    @Override
    public int graphCount() {
        return graphStore.getNames().size();
    }
}
