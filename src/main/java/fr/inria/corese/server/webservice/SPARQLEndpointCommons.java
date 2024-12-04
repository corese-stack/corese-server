package fr.inria.corese.server.webservice;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

/**
 * Stores settings and functions shared to all SPARQL endpoints
 *
 * @author Pierre Maillot
 * @since 2024-11-26
 */
public class SPARQLEndpointCommons {
    private String key;

    // set true to prevent update/load
    private boolean isProtected = false;
    // true when Ajax
    private boolean isAjax = true;

    private SPARQLEndpointCommons() {

    }

    private static SPARQLEndpointCommons instance = null;

    public static SPARQLEndpointCommons getInstance() {
        if (instance == null) {
            instance = new SPARQLEndpointCommons();
        }
        return instance;
    }



    public void init() {
        if (getKey() == null) {
            setKey(genkey());
        }
    }

    public String genkey() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param aKey the key to set
     */
    public void setKey(String aKey) {
        key = aKey;
    }

    // access key gives special access level (RESTRICTED vs PUBLIC)
    public boolean hasKey(HttpServletRequest request, String access) {
        EventManager.getSingleton().getHostMap();
        request.getRemoteHost();
        return hasKey(access);
    }

    public boolean hasKey(String access) {
        return getKey() != null && getKey().equals(access);
    }

    public boolean isProtected() {
        return isProtected;
    }

    public void setProtected(boolean b) {
        isProtected = b;
    }

    public boolean isAjax() {
        return isAjax;
    }

    public void setAjax(boolean b) {
        isAjax = b;
    }
}
