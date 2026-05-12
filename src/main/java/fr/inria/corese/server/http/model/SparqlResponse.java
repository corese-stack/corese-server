package fr.inria.corese.server.http.model;

import java.nio.charset.StandardCharsets;

/**
 * Immutable HTTP response produced by the service layer.
 * <p>Status codes follow SPARQL 1.1 Protocol
 * 200 — successful query,
 * 204 — successful update (no body),
 * 400 — malformed SPARQL,
 * 500 — internal service error.
 *
 * @param statusCode  HTTP status code
 * @param contentType value of the {@code Content-Type} header, or {@code null} for no body
 * @param body        response body bytes (never {@code null}, may be empty)
 */
public record SparqlResponse(
        int statusCode,
        String contentType,
        byte[] body
) {
    /**
     * 200 OK with a string body.
     *
     * @param contentType MIME type of the response
     * @param body        response body as a string (UTF-8 encoded)
     * @return a 200 response
     */
    public static SparqlResponse ok(String contentType, String body) {
        return new SparqlResponse(200, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 200 OK with a raw byte body.
     *
     * @param contentType MIME type of the response
     * @param body        response body as raw bytes
     * @return a 200 response
     */
    public static SparqlResponse ok(String contentType, byte[] body) {
        return new SparqlResponse(200, contentType, body);
    }

    /**
     * 204 No Content — successful update SHOULD return 2XX.
     *
     * @return a 204 response with no body
     */
    public static SparqlResponse noContent() {
        return new SparqlResponse(204, null, new byte[0]);
    }

    /**
     * 400 Bad Request — malformed SPARQL
     *
     * @param message error description
     * @return a 400 response with the message as plain text
     */
    public static SparqlResponse badRequest(String message) {
        return new SparqlResponse(400, "text/plain;charset=utf-8",
                message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 500 Internal Server Error — service failure.
     *
     * @param message error description
     * @return a 500 response with the message as plain text
     */
    public static SparqlResponse serverError(String message) {
        return new SparqlResponse(500, "text/plain;charset=utf-8",
                message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Checks if the response is successful.
     *
     * @return {@code true} if status code is 2xx
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Checks if the response has a non-empty body.
     *
     * @return {@code true} if body length is greater than zero
     */
    public boolean hasBody() {
        return body != null && body.length > 0;
    }

    /**
     * Returns the body decoded as a UTF-8 string.
     *
     * @return body as string, or empty string if body is null/empty
     */
    public String bodyAsString() {
        if (body == null || body.length == 0) return "";
        return new String(body, StandardCharsets.UTF_8);
    }
}