package fr.inria.corese.server.webservice;

import fr.inria.corese.core.util.Property;

/**
 * Stores the properties specific to the server configuration
 * TODO Make it connectable to core.util.Property
 */
public class ServerProperties {

    enum Values {
        ELASTICSEARCH_API_KEY,
        ELASTICSEARCH_API_ADDRESS,
    }
}
