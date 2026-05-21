package fr.inria.corese.server.store;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.query.QueryProcess;

/**
 * Abstraction over the RDF triplestore.
 * This is the ONLY entry point to corese-core in the entire project.
 * No other class imports corese-core types directly.
 */
public interface TripleStoreManager {

    //  Execution

    /**
     * Creates a new {@link QueryProcess} bound to this store's graph.
     *
     * @return a ready-to-use query processor
     */
    QueryProcess newQueryProcess();

    //  Graph CRUD

    /**
     * Retrieves a named graph by URI.
     *
     * @param graphUri the named graph URI
     * @return the graph, or {@code null} if not found
     */
    Graph getNamedGraph(String graphUri);

    /**
     * Merges {@code graph} into the named graph identified by {@code graphUri}.
     *
     * @param graphUri the named graph URI
     * @param graph    the graph whose triples are merged in
     */
    void addToGraph(String graphUri, Graph graph);

    /**
     * Replaces the named graph identified by {@code graphUri} with {@code graph}.
     *
     * @param graphUri the named graph URI
     * @param graph    the new graph to store
     */
    void replaceGraph(String graphUri, Graph graph);

    /**
     * Drops the named graph identified by {@code graphUri}. No-op if not found.
     *
     * @param graphUri the named graph URI to delete
     */
    void deleteGraph(String graphUri);

    // Lifecycle

    /**
     * Loads initial RDF data from the path configured in {@link fr.inria.corese.server.config.ServerConfig}.
     */
    void loadInitialData();

    /**
     * Serializes the entire store to a file in Turtle format.
     *
     * @param path destination file path
     */
    void dumpToFile(String path);

    /**
     * Clears all triples and named graphs from the store.
     */
    void reset();

    // Concurrency

    /**
     * Acquires the shared read lock.
     */
    void readLock();

    /**
     * Releases the shared read lock.
     */
    void readUnlock();

    /**
     * Acquires the exclusive write lock.
     */
    void writeLock();

    /**
     * Releases the exclusive write lock.
     */
    void writeUnlock();

    // Statistics

    /**
     * Returns the total number of triples across all graphs.
     *
     * @return triple count
     */
    long tripleCount();

    /**
     * Returns the number of named graphs in the store.
     *
     * @return graph count
     */
    int graphCount();
}
