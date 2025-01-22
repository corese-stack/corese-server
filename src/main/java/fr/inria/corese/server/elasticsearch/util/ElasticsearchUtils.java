package fr.inria.corese.server.elasticsearch.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ElasticsearchUtils {

    /**
     * Replace all special characters in a URI to generate a valid Elasticsearch document ID.
     */
    public static String generateDocIdFromUri(String uri) throws UnsupportedEncodingException {
        String replacedURI = uri.replaceAll("<", "").replaceAll(">", "").replaceAll(":", "").replaceAll("/", "").replaceAll("#", "").replaceAll("\\.", "").replaceAll("\\?", "").replaceAll("&", "").replaceAll("=", "").replaceAll(";", "").replaceAll(",", "").replaceAll("\\+", "").replaceAll("\\*", "").replaceAll("\\(", "").replaceAll("\\)", "").replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("\\|", "").replaceAll("\"", "").replaceAll("'", "").replaceAll("`", "").replaceAll(" ", "_");
        return URLEncoder.encode(replacedURI, StandardCharsets.UTF_8.toString());
    }
}
