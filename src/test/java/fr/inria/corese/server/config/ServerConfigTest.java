package fr.inria.corese.server.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ServerConfig}.
 */
class ServerConfigTest {

    @Test
    void defaultValues() {
        ServerConfig config = new ServerConfig(8080, null, "data/dump.nt", false);

        assertEquals(8080, config.port());
        assertNull(config.dataPath());
        assertEquals("data/dump.nt", config.dumpPath());
        assertFalse(config.authEnabled());
    }

    @Test
    void customValues() {
        ServerConfig config = new ServerConfig(9090, "/data/init.ttl", "/tmp/dump.nt", true);

        assertEquals(9090, config.port());
        assertEquals("/data/init.ttl", config.dataPath());
        assertEquals("/tmp/dump.nt", config.dumpPath());
        assertTrue(config.authEnabled());
    }

    @Test
    void recordEquality() {
        ServerConfig a = new ServerConfig(8080, null, "data/dump.nt", false);
        ServerConfig b = new ServerConfig(8080, null, "data/dump.nt", false);
        assertEquals(a, b);
    }
}
