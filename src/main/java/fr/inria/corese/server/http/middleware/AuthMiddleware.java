package fr.inria.corese.server.http.middleware;

import fr.inria.corese.server.config.Role;
import fr.inria.corese.server.config.ServerConfig;
import io.javalin.http.Context;
import io.javalin.security.RouteRole;

import java.util.Set;

/**
 * Access manager
 * This class is used as a before-matched handler in ServerApplication.
 */
public class AuthMiddleware {

    private final ServerConfig config;

    /**
     * Creates a new AuthMiddleware.
     *
     * @param config server configuration (used to check whether auth is enabled)
     */
    public AuthMiddleware(ServerConfig config) {
        this.config = config;
    }

    /**
     * Called via {@code app.beforeMatched()} in Javalin 6.
     * Resolves the effective role and enforces it against the route's required roles.
     *
     * @param ctx the current Javalin request context
     */
    public void handle(Context ctx) {
        Role effective = resolveRole(ctx);
        ctx.attribute("role", effective);

        Set<? extends RouteRole> required = ctx.routeRoles();

        if (!isAuthorised(effective, required)) {
            if (effective == Role.ANONYMOUS) {
                ctx.status(401).result("Authentication required");
                ctx.skipRemainingHandlers();
            } else {
                ctx.status(403).result("Insufficient permissions — required: " + required);
                ctx.skipRemainingHandlers();
            }
        }
    }

    // Role resolution

    private Role resolveRole(Context ctx) {
        if (!config.authEnabled()) return Role.ANONYMOUS;

        // Phase 3 — OIDC validation (to be implemented)

        return Role.ANONYMOUS;
    }

    // Authorisation

    private boolean isAuthorised(Role effective, Set<? extends RouteRole> required) {
        if (required == null || required.isEmpty() || required.contains(Role.ANONYMOUS))
            return true;
        return switch (effective) {
            case ADMIN -> true;
            case USER_W -> required.contains(Role.USER_W) || required.contains(Role.USER_R);
            case USER_R -> required.contains(Role.USER_R);
            case ANONYMOUS -> false;
        };
    }
}
