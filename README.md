# Corese-Server

[![License: CECILL-C](https://img.shields.io/badge/License-CECILL--C-blue.svg)](https://cecill.info/licences/Licence_CeCILL-C_V1-en.html) [![Discussions](https://img.shields.io/badge/Discussions-GitHub-blue)](https://github.com/orgs/corese-stack/discussions)

Corese-Server is the server-side component of the Corese platform. It provides an HTTP interface to expose Corese functionalities, allowing users to send SPARQL queries and updates via a web API, manipulate RDF data, and manage reasoning tasks.

## Features

- Expose RDF data through an HTTP interface.
- Process SPARQL queries and updates.
- RESTful API for querying and updating RDF datasets.

## Getting Started

### Download and Install

You can run Corese-Server using Docker or by downloading and executing the JAR file.

**Docker:**

To run the server using Docker, pull the latest image and run the container:

``` bash
docker run --name my-corese \
    -p 8080:8080 \
    -d wimmics/corese
```

The server will be running at `http://localhost:8080`. Check the [Docker Hub page](https://hub.docker.com/r/wimmics/corese) for more information.

**JAR File:**

Download the latest version of the Corese-Server JAR from the [releases page](https://github.com/corese-stack/corese-server/releases), then start the server with the following command:

``` bash
java -jar corese-server-4.5.0.jar
```

By default, the server will be running at `http://localhost:8080`.

## Documentation

Explore the available documentation to help you get started with Corese-Core:

- [Getting Started Guide](https://corese-stack.github.io/corese-server/v4.5.0/getting_started/getting_started_with_corese-server.html)
- [API Documentation](https://corese-stack.github.io/corese-server/v4.5.0/java_api/library_root.html)

## Contributions and Community

We welcome contributions to improve Corese-Server! Here’s how you can get involved:

- **Discussions:** If you have questions, ideas, or suggestions, please participate in our [discussion forum](https://github.com/orgs/corese-stack/discussions).
- **Issue Tracker:** Found a bug or want to request a new feature? Use our [issue tracker](https://github.com/corese-stack/corese-server/issues).
- **Pull Requests:** We accept pull requests. You can submit your changes [here](https://github.com/corese-stack/corese-server/pulls).

## Useful Links

- [Corese Official Website](https://corese-stack.github.io/corese-server/v4.5.0/index.html)
- **Mailing List:** <corese-users@inria.fr>
- **Join the Mailing List:** Send an email to <corese-users-request@inria.fr> with the subject: `subscribe`
