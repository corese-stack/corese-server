plugins {
    `java-library`
    `maven-publish`
    id("org.gradlex.extra-java-module-info") version "1.8"
    id("com.gradleup.shadow") version "8.3.1"
    signing
    application
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_11
}
tasks.withType<Javadoc> { isFailOnError = false }

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}

project.setProperty("mainClassName","fr.inria.corese.server.webservice.EmbeddedJettyServer")
tasks {
    shadowJar {
        manifest {
            attributes(
                "Main-Class" to "fr.inria.corese.server.webservice.EmbeddedJettyServer"
            )
        }
        this.archiveClassifier = "app"
    }
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    api(libs.fr.inria.corese.corese.core)
    api("fr.inria.corese:corese-jena:5.0.0-SNAPSHOT")
    api("fr.inria.corese:corese-rdf4j:5.0.0-SNAPSHOT")
    api(libs.javax.xml.bind.jaxb.api)
    api(libs.com.sun.xml.bind.jaxb.core)
    api(libs.com.sun.xml.bind.jaxb.impl)
    api(libs.org.glassfish.jersey.core.jersey.client)
    api(libs.org.glassfish.jersey.containers.jersey.container.jetty.http)
    api(libs.org.glassfish.jersey.media.jersey.media.multipart)
    api(libs.org.glassfish.jersey.inject.jersey.hk2)
    api(libs.org.glassfish.metro.webservices.rt)
    api(libs.org.glassfish.jersey.containers.jersey.container.servlet.core)
    api(libs.org.apache.logging.log4j.log4j.slf4j18.impl)
    api(libs.org.apache.logging.log4j.log4j.api)
    api(libs.org.apache.logging.log4j.log4j.core)
    api(libs.commons.lang.commons.lang)
    api(libs.commons.cli.commons.cli)
    api(libs.commons.vfs.commons.vfs)
    api(libs.commons.io.commons.io)
    api(libs.org.jsoup.jsoup)
    api(libs.org.json.json)
    api(libs.org.eclipse.jetty.jetty.server)
    api(libs.org.eclipse.jetty.jetty.servlets)
    api(libs.org.eclipse.jetty.websocket.websocket.jetty.server)
    api(libs.org.eclipse.jetty.jetty.util)
    testImplementation(libs.junit.junit)
}

group = "fr.inria.corese"
version = "5.0.0-SNAPSHOT"
description = "corese-server"
java.sourceCompatibility = JavaVersion.VERSION_11

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}

extraJavaModuleInfo {
    failOnMissingModuleInfo.set(false)
    automaticModule("fr.com.hp.hpl.jena.rdf.arp:arp", "arp")
    automaticModule("com.github.jsonld-java:jsonld-java", "jsonld.java")
    automaticModule("commons-lang:commons-lang", "commons.lang")
    automaticModule("commons-cli:commons-cli", "commons.cli")
    automaticModule("commons-vfs:commons-vfs", "commons.vfs")
    automaticModule("fr.inria.lille.shexjava:shexjava-core", "shexjava.core")
    automaticModule("org.eclipse.rdf4j:rdf4j-model", "rdf4j.model")
    automaticModule("org.glassfish.jersey.media:jersey-media-multipart", "jersey.media.multipart")
    automaticModule("org.glassfish.jersey.containers:jersey-container-servlet-core", "jersey.container.servlet.core")
    automaticModule("org.glassfish.jersey.core:jersey-server", "jersey.server")
    automaticModule("org.glassfish.jersey.core:jersey-common", "jersey.common")
}