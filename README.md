# Corese-Server

[![Discussions](https://img.shields.io/badge/Discussions-GitHub-blue)](https://github.com/orgs/corese-stack/discussions)
[![Java](https://img.shields.io/badge/Java-25-orange)](https://openjdk.org/projects/jdk/25/)
[![Javalin](https://img.shields.io/badge/Javalin-7.2-green)](https://javalin.io/)

Corese-Server is the HTTP server component of the [corese-core](https://github.com/corese-stack/corese-core/tree/feature/corese-next) as a SPARQL 1.1 endpoint over HTTP,
following the [SPARQL 1.1 Protocol](https://www.w3.org/TR/sparql11-protocol/) W3C Recommendation.

## Features

- SPARQL 1.1 Protocol compliant endpoint (`/sparql`)
- All three query transmission forms: GET, POST URL-encoded, POST direct
- Content negotiation: XML, JSON, CSV, TSV (SELECT/ASK) — Turtle, RDF/XML, N-Triples, JSON-LD (CONSTRUCT/DESCRIBE)
- NTriples persistence — dump on shutdown, reload on startup
- Health and status endpoints (`/health`, `/status`)

## Quick Start

### Run with JAR

```bash
# Build
./gradlew shadowJar

```

The server starts at `http://localhost:8080`.

### Run with Docker

```bash
docker build -t corese-server .
docker run -p 8080:8080 corese-server
```

## SPARQL Endpoint

### Query — `GET /sparql`

**Linux / macOS:**
```bash
# Form 1 — §2.1.1 GET
curl "http://localhost:8080/sparql?query=SELECT+*+WHERE+{+?s+?p+?o+}"
 
# Form 1 — with content negotiation
curl -H "Accept: application/sparql-results+json" \
     "http://localhost:8080/sparql?query=SELECT+*+WHERE+{+?s+?p+?o+}"
 
# Form 2 — §2.1.2 POST URL-encoded
curl -X POST http://localhost:8080/sparql \
     -H "Content-Type: application/x-www-form-urlencoded" \
     --data-urlencode "query=SELECT * WHERE { ?s ?p ?o }"
 
# Form 3 — §2.1.3 POST direct
curl -X POST http://localhost:8080/sparql \
     -H "Content-Type: application/sparql-query" \
     -d "SELECT * WHERE { ?s ?p ?o }"
```

**Windows PowerShell:**
```powershell
# Form 1 — §2.1.1 GET
Invoke-WebRequest "http://localhost:8080/sparql?query=SELECT+*+WHERE+{+?s+?p+?o+}"
 
# Form 1 — with content negotiation
Invoke-WebRequest -Uri "http://localhost:8080/sparql?query=SELECT+*+WHERE+{+?s+?p+?o+}" `
    -Headers @{ Accept = "application/sparql-results+json" }
 
# Form 2 — §2.1.2 POST URL-encoded
Invoke-WebRequest -Uri "http://localhost:8080/sparql" `
    -Method POST `
    -ContentType "application/x-www-form-urlencoded" `
    -Body "query=SELECT+*+WHERE+{+?s+?p+?o+}"
 
# Form 3 — §2.1.3 POST direct
Invoke-WebRequest -Uri "http://localhost:8080/sparql" `
    -Method POST `
    -ContentType "application/sparql-query" `
    -Body "SELECT * WHERE { ?s ?p ?o }"
```

### Health & Status

```bash
curl http://localhost:8080/health
# → {"status":"UP","version":"4.6.4"}

curl http://localhost:8080/status
# → {"status":"UP","uptime":"PT5M","triples":0,"graphs":0,...}
```

## Development

### Prerequisites

- Java 25
- Gradle 9.5.0

### Build and Test

```bash
# Compile
./gradlew compileJava

# Run tests
./gradlew test

# Build fat JAR
./gradlew shadowJar

# Full build with coverage
./gradlew build
```
## Architecture

```
fr.inria.corese.server
├── app/             ServerApplication.java      Entry point — wires all components
├── config/          ServerConfig, Role,          Configuration + RBAC roles
│                    SecurityConfig               OIDC config
├── http/
│   ├── handler/     SPARQLQueryHandler           GET/POST /sparql — query
│   │                SPARQLUpdateHandler          POST /sparql — update
│   │                
│   ├── middleware/  CorsMiddleware, AuthMiddleware Cross-cutting HTTP concerns
│   └── model/       SparqlRequest, SparqlResponse Immutable data transfer objects
├── service/         SparqlExecutionService       SPARQL business logic
│                    
│                    ContentNegotiator            Accept header → corese-core format
└── store/           TripleStoreManager           Interface to corese-core
                     CoreseTripleStoreManager     corese-core 4.6.4 implementation
```

**Dependency rule:** `http/` → `service/` → `store/` → `corese-core`
## Migration from legacy corese-server

| Legacy component       | New component                          |
|------------------------|----------------------------------------|
| Jersey 3.0.4           | Javalin 7.2.0                          |
| Jetty 11 (EOL)         | Jetty 12 (embedded via Javalin 7)      |
| EmbeddedJettyServer    | ServerApplication.java                 |
| SPARQLRestAPI (god obj)| Focused handlers + services            |
| Log4j2                 | SLF4J + Logback                        |
| JUnit 4                | JUnit 5                                |
| Java 11                | Java 25                                |
 