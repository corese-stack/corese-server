package fr.inria.corese.server.service;

import fr.inria.corese.server.config.ServerConfig;
import fr.inria.corese.server.store.CoreseTripleStoreManager;
import fr.inria.corese.server.store.TripleStoreManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ScheduledDumpService}.
 */
class ScheduledDumpServiceTest {

    @TempDir
    Path tempDir;

    private TripleStoreManager store;
    private ScheduledDumpService service;
    private String dumpPath;

    @BeforeEach
    void setUp() {
        dumpPath = tempDir.resolve("dump.nt").toString();
    }

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.stop();
        }
    }

    @Test
    @DisplayName("interval=0 -> service disabled, no dump")
    void disabled_whenIntervalZero() throws InterruptedException {
        ServerConfig config = new ServerConfig(8080, null, dumpPath, false, 0);
        store = new CoreseTripleStoreManager(config);
        service = new ScheduledDumpService(store, config);

        service.start();
        Thread.sleep(200);

        assertEquals(0, service.getDumpCount());
        assertFalse(Files.exists(Path.of(dumpPath)), "No dump file should be created");
    }

    @Test
    @DisplayName("null dumpPath -> service skipped, no exception")
    void disabled_whenNullDumpPath() throws InterruptedException {
        ServerConfig config = new ServerConfig(8080, null, null, false, 1);
        store = new CoreseTripleStoreManager(config);
        service = new ScheduledDumpService(store, config);

        assertDoesNotThrow(() -> service.start());
        Thread.sleep(200);
        assertEquals(0, service.getDumpCount());
    }


    @Test
    @DisplayName("interval=1s -> dump file created within 2 seconds")
    void enabled_createsDumpFile() throws InterruptedException {
        ServerConfig config = new ServerConfig(8080, null, dumpPath, false, 1);
        store = new CoreseTripleStoreManager(config);
        service = new ScheduledDumpService(store, config);

        service.start();
        Thread.sleep(2200);

        assertTrue(Files.exists(Path.of(dumpPath)), "Dump file should be created");
        assertTrue(service.getDumpCount() >= 1, "At least 1 dump should have occurred");
    }

    @Test
    @DisplayName("interval=1s -> multiple dumps within 3 seconds")
    void enabled_multipleDumps() throws InterruptedException {
        ServerConfig config = new ServerConfig(8080, null, dumpPath, false, 1);
        store = new CoreseTripleStoreManager(config);
        service = new ScheduledDumpService(store, config);

        service.start();
        Thread.sleep(3200);

        assertTrue(service.getDumpCount() >= 2, "At least 2 dumps should have occurred");
    }

    @Test
    @DisplayName("dump contains inserted triples")
    void dump_containsInsertedTriples() throws InterruptedException, IOException {
        ServerConfig config = new ServerConfig(8080, null, dumpPath, false, 1);
        store = new CoreseTripleStoreManager(config);
        service = new ScheduledDumpService(store, config);

        store.writeLock();
        try {
            store.newQueryProcess().query(
                    "INSERT DATA { <http://ex.org/s> <http://ex.org/p> <http://ex.org/o> }"
            );
        } catch (Exception e) {
            fail("INSERT failed: " + e.getMessage());
        } finally {
            store.writeUnlock();
        }

        service.start();
        Thread.sleep(2200);

        assertTrue(Files.exists(Path.of(dumpPath)));
        String content = Files.readString(Path.of(dumpPath));
        assertTrue(content.contains("ex.org"), "Dump should contain inserted triple");
    }


    @Test
    @DisplayName("stop() — no exception, scheduler terminates")
    void stop_noException() throws InterruptedException {
        ServerConfig config = new ServerConfig(8080, null, dumpPath, false, 1);
        store = new CoreseTripleStoreManager(config);
        service = new ScheduledDumpService(store, config);

        service.start();
        Thread.sleep(500);

        assertDoesNotThrow(() -> service.stop());
    }

    @Test
    @DisplayName("stop() before start() — no exception")
    void stop_beforeStart_noException() {
        ServerConfig config = new ServerConfig(8080, null, dumpPath, false, 1);
        store = new CoreseTripleStoreManager(config);
        service = new ScheduledDumpService(store, config);

        assertDoesNotThrow(() -> service.stop());
    }


    @Test
    @DisplayName("ServerConfig default dumpIntervalSeconds = 300")
    void serverConfig_defaultDumpInterval() {
        ServerConfig config = new ServerConfig(8080, null, "data/dump.nt", false, 300);
        assertEquals(300, config.dumpIntervalSeconds());
    }

    @Test
    @DisplayName("ServerConfig dumpIntervalSeconds = 0 -> disabled")
    void serverConfig_zeroInterval_disabled() {
        ServerConfig config = new ServerConfig(8080, null, "data/dump.nt", false, 0);
        assertEquals(0, config.dumpIntervalSeconds());
    }
}
