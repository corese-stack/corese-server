package fr.inria.corese.server.store;

import fr.inria.corese.server.config.ServerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CoreseTripleStoreManager}.
 */
class CoreseTripleStoreManagerTest {

    private CoreseTripleStoreManager store;

    @BeforeEach
    void setUp() {
        ServerConfig config = new ServerConfig(8080, null, null, false);
        store = new CoreseTripleStoreManager(config);
    }

    // Initial state

    @Test
    @DisplayName("New store is empty")
    void newStore_isEmpty() {
        assertEquals(0, store.tripleCount());
        assertEquals(0, store.graphCount());
    }

    // Query execution

    @Test
    @DisplayName("newQueryProcess returns non-null")
    void newQueryProcess_notNull() {
        assertNotNull(store.newQueryProcess());
    }

    @Test
    @DisplayName("INSERT increases triple count")
    void insert_increasesTripleCount() throws Exception {
        store.writeLock();
        try {
            store.newQueryProcess().query(
                    "INSERT DATA { <http://ex.org/s> <http://ex.org/p> <http://ex.org/o> }"
            );
        } finally {
            store.writeUnlock();
        }
        assertEquals(1, store.tripleCount());
    }

    @Test
    @DisplayName("SELECT on empty store returns results")
    void select_emptyStore_returnsNoException() throws Exception {
        store.readLock();
        try {
            var mappings = store.newQueryProcess().query("SELECT * WHERE { ?s ?p ?o }");
            assertNotNull(mappings);
            assertEquals(0, mappings.size());
        } finally {
            store.readUnlock();
        }
    }

    // Named graph operations

    @Test
    @DisplayName("getNamedGraph returns null for unknown graph")
    void getNamedGraph_unknown_returnsNull() {
        assertNull(store.getNamedGraph("http://example.org/unknown"));
    }

    @Test
    @DisplayName("deleteGraph — DROP SILENT does not throw on missing graph")
    void deleteGraph_missing_noException() {
        assertDoesNotThrow(() ->
                store.deleteGraph("http://example.org/does-not-exist")
        );
    }


    // Concurrency locks

    @Test
    @DisplayName("readLock / readUnlock — no exception")
    void readLockUnlock_noException() {
        assertDoesNotThrow(() -> {
            store.readLock();
            store.readUnlock();
        });
    }

    @Test
    @DisplayName("writeLock / writeUnlock — no exception")
    void writeLockUnlock_noException() {
        assertDoesNotThrow(() -> {
            store.writeLock();
            store.writeUnlock();
        });
    }

    // loadInitialData

    @Test
    @DisplayName("loadInitialData — null dataPath does nothing")
    void loadInitialData_nullPath_noException() {
        assertDoesNotThrow(() -> store.loadInitialData());
        assertEquals(0, store.tripleCount());
    }

    @Test
    @DisplayName("loadInitialData — missing file logs warning, no exception")
    void loadInitialData_missingFile_noException() {
        ServerConfig config = new ServerConfig(8080, "/does/not/exist.ttl", null, false);
        CoreseTripleStoreManager s = new CoreseTripleStoreManager(config);
        assertDoesNotThrow(s::loadInitialData);
        assertEquals(0, s.tripleCount());
    }
}
