package fr.inria.corese.server.config;

/**
 * Immutable server configuration loaded from environment variables.
 * <p>All settings have sensible defaults so the server works out of the box
 *
 * @param port        HTTP listening port
 * @param dataPath    path to initial RDF data file, or {@code null} for empty store
 * @param dumpPath    path where the store is serialized on shutdown
 * @param authEnabled whether RBAC / JWT authentication is active
 */
public record ServerConfig(
        int port,
        String dataPath,
        String dumpPath,
        boolean authEnabled
) {
    /**
     * Load configuration from environment variables with defaults.
     *
     * @return a fully-initialized {@link ServerConfig}
     */
    public static ServerConfig fromEnv() {
        return new ServerConfig(
                intEnv("PORT", 8080),
                System.getenv("CORESE_DATA_PATH"),
                stringEnv("CORESE_DUMP_PATH", "data/dump.nt"),
                boolEnv("CORESE_AUTH_ENABLED", false)
        );
    }

    @SuppressWarnings("SameParameterValue")
    private static int intEnv(String key, int def) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) return def;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static String stringEnv(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    @SuppressWarnings("SameParameterValue")
    private static boolean boolEnv(String key, boolean def) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) return def;
        return "true".equalsIgnoreCase(v.trim());
    }
}
