plugins {
    `java-library`
    `maven-publish`
    `jacoco`
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

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    executionData.setFrom(fileTree(buildDir).include("/jacoco/*.exec"))

    reports {
        xml.required.set(true)
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
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
    mavenLocal()    // First, check the local Maven repository
    mavenCentral()  // Then, check Maven Central
}

dependencies {
    api("fr.inria.corese:corese-core:4.6.1-SNAPSHOT")
    api("fr.inria.corese:corese-jena:5.0.0-SNAPSHOT")
    api("fr.inria.corese:corese-rdf4j:5.0.0-SNAPSHOT")

    api("javax.xml.bind:jaxb-api:2.3.1")
    api("com.sun.xml.bind:jaxb-core:2.3.0.1")
    api("com.sun.xml.bind:jaxb-impl:2.3.2")

    val jersey_version = "3.0.4"
    api("org.glassfish.jersey.core:jersey-client:${jersey_version}")
    api("org.glassfish.jersey.containers:jersey-container-jetty-http:${jersey_version}")
    api("org.glassfish.jersey.containers:jersey-container-servlet-core:${jersey_version}")
    api("org.glassfish.jersey.media:jersey-media-multipart:${jersey_version}")
    api("org.glassfish.jersey.inject:jersey-hk2:${jersey_version}")

    api("org.glassfish.metro:webservices-rt:4.0.4")

    val lo4j_version = "2.18.0"
    api("org.apache.logging.log4j:log4j-slf4j18-impl:${lo4j_version}")
    api("org.apache.logging.log4j:log4j-api:${lo4j_version}")
    api("org.apache.logging.log4j:log4j-core:${lo4j_version}")

    api("commons-lang:commons-lang:2.4")
    api("commons-cli:commons-cli:1.4")
    api("commons-vfs:commons-vfs:1.0")
    api("commons-io:commons-io:2.11.0")

    api("org.jsoup:jsoup:1.15.3")
    api("org.json:json:20240303")

    val jetty_version = "11.0.24"
    api("org.eclipse.jetty:jetty-server:${jetty_version}")
    api("org.eclipse.jetty:jetty-servlets:${jetty_version}")
    api("org.eclipse.jetty.websocket:websocket-jetty-server:${jetty_version}")
    api("org.eclipse.jetty:jetty-util:${jetty_version}")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.wiremock:wiremock:3.9.2")
}

group = "fr.inria.corese"
version = "4.6.1-SNAPSHOT"
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
