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

}
