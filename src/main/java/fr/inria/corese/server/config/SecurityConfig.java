package fr.inria.corese.server.config;

/**
 * Immutable security configuration.
 *
 * @param authEnabled whether authentication is enabled
 * @param oidcIssuer  OIDC issuer URL
 * @param jwksUri     JWKS endpoint URI used to fetch public keys for JWT validation
 * @param audience    expected JWT audience claim value
 */
public record SecurityConfig(
        boolean authEnabled,
        String oidcIssuer,
        String jwksUri,
        String audience
) {

    /**
     * Load security configuration from environment variables.
     *
     * @return a fully-initialized {@link SecurityConfig}
     */
    @SuppressWarnings("SameParameterValue")
    public static SecurityConfig fromEnv() {
        return new SecurityConfig(
                boolEnv("CORESE_AUTH_ENABLED", false),
                System.getenv("CORESE_OIDC_ISSUER"),
                System.getenv("CORESE_JWKS_URI"),
                stringEnv("CORESE_OIDC_AUDIENCE", "corese-server")
        );
    }

    /**
     * Create a Phase 1 config — auth disabled, no OIDC.
     *
     * @return a disabled security config
     */
    public static SecurityConfig disabled() {
        return new SecurityConfig(false, null, null, "corese-server");
    }

    /**
     * Returns true if OIDC is fully configured (Phase 3 ready).
     *
     * @return true if issuer and JWKS URI are both set
     */
    public boolean isOidcConfigured() {
        return oidcIssuer != null && !oidcIssuer.isBlank()
                && jwksUri != null && !jwksUri.isBlank();
    }

    @SuppressWarnings("SameParameterValue")
    private static boolean boolEnv(String key, boolean def) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) return def;
        return "true".equalsIgnoreCase(v.trim());
    }

    @SuppressWarnings("SameParameterValue")
    private static String stringEnv(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v.trim();
    }
}
