plugins {
    `java-library`
    `maven-publish`
    `jacoco`
    id("com.gradleup.shadow") version "8.3.1"
    signing
}

jacoco {
    toolVersion = "0.8.12"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
    isFailOnError = false
}

tasks.shadowJar {
    manifest {
        attributes(
            "Main-Class" to "fr.inria.corese.server.app.ServerApplication",
            "Implementation-Version" to project.version
        )
    }
    archiveClassifier.set("app")
    mergeServiceFiles()
    exclude("module-info.class")
    exclude("META-INF/versions/*/module-info.class")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// JaCoCo 0.8.12 cannot instrument Java 25 (class file major version 69).
// Re-enable when toolVersion is upgraded to 0.8.13+.
tasks.jacocoTestReport {
    enabled = false
}

// Gradle 9.x Testing DSL — handles junit-platform-launcher automatically
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.10.3")
        }
    }
}

tasks.test {
    configure<JacocoTaskExtension> {
        isEnabled = false
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.maven.apache.org/maven2/") }
}

// ── Dependency vulnerability fixes ────────────────────────────────────────
configurations.all {
    resolutionStrategy {
        // CVE-2012-0881, CVE-2022-23437, CVE-2013-4002, CVE-2020-14338, CVE-2009-2625
        force("xerces:xercesImpl:2.12.2")
        // Jetty CVEs fixed by Javalin 7 (Jetty 12)
    }
}

dependencies {

    // SPARQL engine — corese-core
    api("fr.inria.corese:corese-core:4.6.4")

    // HTTP framework — Javalin 7 + Jetty 12
    // Fixes: CVE-2026-2332, CVE-2024-6763, CVE-2025-11143 (Jetty 11 → 12)
    implementation("io.javalin:javalin:7.2.0")

    // Logging — SLF4J + Logback
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // JSON
    implementation("org.json:json:20250517")

    // Tests
    testImplementation("io.javalin:javalin-testtools:7.2.0")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")

}

group = "fr.inria.corese"
version = "4.6.4"

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.processResources {
    filesMatching("version.properties") {
        expand("version" to project.version)
    }
}