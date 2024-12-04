module fr.inria.corese.corese_server {
    requires fr.inria.corese.corese_core;
    requires org.apache.jena.core;
    requires fr.inria.corese.corese_jena;
    requires fr.inria.corese.corese_rdf4j;

    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.servlets;
    requires org.eclipse.jetty.util;
    requires org.eclipse.jetty.http;
    requires org.eclipse.jetty.websocket.servlet;

    requires jersey.server;
    requires jersey.common;
    requires jersey.media.multipart;
    requires jersey.container.servlet.core;

    requires jakarta.ws.rs;

    requires org.apache.logging.log4j;

    requires commons.cli;
    requires org.apache.commons.io;

    requires java.logging;

    requires org.jsoup;
    requires org.json;

    requires org.slf4j;
    requires commons.vfs;
    requires org.apache.httpcomponents.httpcore;
    requires elasticsearch.rest.client;
    requires elasticsearch.java;
}
