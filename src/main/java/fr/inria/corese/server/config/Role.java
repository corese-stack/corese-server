package fr.inria.corese.server.config;

import io.javalin.security.RouteRole;

/**
 * RBAC roles — implements Javalin RouteRole for use as AccessManager.
 * Hierarchy (cumulative): ANONYMOUS -> USER_R -> USER_W -> ADMIN
 * auth disabled — every request resolves to ANONYMOUS.
 */
public enum Role implements RouteRole {
    /**
     * No authentication required — public read-only access.
     */
    ANONYMOUS,
    /**
     * Authenticated — extended read on private graphs.
     */
    USER_R,
    /**
     * Authenticated — read + write.
     */
    USER_W,
    /**
     * Full access including server administration.
     */
    ADMIN
}
