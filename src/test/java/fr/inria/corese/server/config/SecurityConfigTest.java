package fr.inria.corese.server.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SecurityConfig}.
 */
class SecurityConfigTest {

    @Test
    @DisplayName("disabled() — auth off, no OIDC")
    void disabled_authOff() {
        SecurityConfig config = SecurityConfig.disabled();
        assertFalse(config.authEnabled());
        assertNull(config.oidcIssuer());
        assertNull(config.jwksUri());
        assertEquals("corese-server", config.audience());
        assertFalse(config.isOidcConfigured());
    }


    @Test
    @DisplayName("record equality")
    void recordEquality() {
        SecurityConfig a = SecurityConfig.disabled();
        SecurityConfig b = SecurityConfig.disabled();
        assertEquals(a, b);
    }
}
