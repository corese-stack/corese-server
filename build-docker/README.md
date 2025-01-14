<!-- markdownlint-configure-file { "MD004": { "style": "consistent" } } -->
<!-- markdownlint-disable MD033 -->

# Corese

<p align="center">
    <a href="https://project.inria.fr/corese/">
        <img src="https://user-images.githubusercontent.com/5692787/151987397-316a61f0-8098-4d37-a4e8-69180e33261a.svg" width="300" height="149" alt="Corese-logo">
    </a>
    <br>
    <strong>Software platform for the Semantic Web of Linked Data</strong>
</p>
<!-- markdownlint-enable MD033 -->

This is a Docker image for hosting the [Corese Semantic Web Server](https://project.inria.fr/corese/) that implements RDF, RDFS, SPARQL 1.1 Query & Update, OWL-RL, SHACL.
Corese also implements the LDScript and STTL SPARQL extensions.

The Docker image tag includes the Corese version installed in the image. The following versions are currently available:

- corese:4.5.1
- corese:4.5.0
- corese:4.4.1
- corese:4.4.0
- corese:4.3.0
- corese:4.2.4

## Running the Corese Docker image

### Running with the `docker run` command

The simplest way to run Corese is to execute the following command. It starts a fresh empty Corese server ready to execute queries submitted to <http://localhost:8080/sparql>.

```sh
docker run --name my-corese \
    -p 8080:8080 \
    -d wimmics/corese
```

To load data when Corese starts up, place your RDF files in the `data` directory and mount it as follows:

```sh
docker run --name my-corese \
    -p 8080:8080 \
    -v /my/local/path/data:/usr/local/corese/data \
    -d wimmics/corese
```

Additionally, you can control configuration parameters and check log files by mounting the `config` and `log` directories respectively:

```sh
docker run --rm -d --name my-corese \
    -p 8080:8080 \
    -v /my/local/path/log:/usr/local/corese/log \
    -v /my/local/path/data:/usr/local/corese/data \
    -v /my/local/path/config:/usr/local/corese/config \
    -d wimmics/corese
```

### Running with Docker Compose

Alternatively, you can run the image using Docker Compose.
Create the `docker-compose.yml` file as follows:

```yml
version: "3"
services:
  corese:
    image: wimmics/corese
    build: ./corese
    ports:
      - "8080:8080"
    volumes:
      - "/my/local/path/log:/usr/local/corese/log"
      - "/my/local/path/data:/usr/local/corese/data"
      - "/my/local/path/config:/usr/local/corese/config"
```

Then run: `docker-compose up -d`

## Loading data at start-up

To load data into Corese at start-up, place your data files in the mounted `data` folder.

Supported extensions are: `.ttl`, `.jsonld`, `.rdf` (for RDF/XML), `.csv`, `.tsv`, `.html` (for rdfa).

Then **delete the `config/corese-profile.ttl` file and restart the Docker container**.
A new `corese-profile.ttl` file will be created, listing the files to be loaded from `data`.
Alternatively, you may edit a previously created `corese-profile.ttl` file and change the list of data files to be loaded.

## Configuration

### Corese configuration files

When it starts, the container will look for three files and create them if they do not exist:

- `config/corese-properties.properties`: This file allows you to tune various parameters in Corese, such as configuring the [storage system](https://github.com/Wimmics/corese/blob/master/docs/storage/Configuring%20and%20Connecting%20to%20Different%20Storage%20Systems%20in%20Corese.md). [Learn more](https://github.com/Wimmics/corese/blob/master/corese-server/build-docker/corese/corese-default-properties.properties).
- `config/corese-profile.ttl`: This file defines a standard server and instructs Corese to load files found in `data`. It can be used to create multiple endpoints, restrict access to external endpoints, and more. [Learn more](https://github.com/Wimmics/corese/blob/master/docs/getting%20started/Getting%20Started%20With%20Corese-server.md).
- `config/log4j2.xml`: This is the configuration file for the logging system used by Corese, which is based on [log4j](https://logging.apache.org/log4j/2.x/manual/configuration.html). You can modify this file to change the way logs are generated by the application. You can control aspects like the level of detail in logs (e.g., INFO, WARN, DEBUG, ERROR), the formatting of log messages, and the destination of log messages (console, file, etc.).

Note: To apply changes, you must edit the files and restart the container.

### Changing the JVM heap size

To change the memory allocated to the JVM that runs Corese, provide the environment variable `$JVM_XMX` with the value of the `-Xmx` JVM parameter.

Example:

- Add the option `-e JVM_XMX=1024m` to the `docker run` command.
- Alternatively, add the following to the `docker-compose.yml` file:

```yml
environment:
  JVM_XMX: "1024m"
```

## Passing Custom Options to Corese

You can pass custom options to the Corese-Server by using the `OPTIONS` environment variable when running the `wimmics/corese` Docker image.

To specify custom options when running the container:

- Add the `-e OPTIONS="<your_options>"` option to the `docker run` command.
- For example, to use the `-su` option, you can run:

```shell
docker run -e OPTIONS="-su" wimmics/corese
```

or

```shell
docker run -e OPTIONS="-protect" wimmics/corese
```

If you prefer using a `docker-compose.yml` file to define your container, you can also specify the `OPTIONS` environment variable within it. Here's an example:

```yml
environment:
  OPTIONS: "<your_options>"
```

You can include this `environment` section within your `docker-compose.yml` file, replacing `<your_options>` with the desired options.

## Testing the container

To test if the container runs properly, simply execute the script below, which submits the query `select * where {?s ?p ?o} limit 100`:

```sh
QUERY=select%20*%20where%20%7B%3Fs%20%3Fp%20%3Fo%7D%20limit%20100
curl --header "Accept: application/sparql-results+json" "http://localhost:8080/sparql?query=$QUERY"
```

To test a SPARQL update request, run the script below:

```sh
QUERY='PREFIX dc: <http://purl.org/dc/elements/1.1/> INSERT DATA { <http://example/book1> dc:title "A new book" . }'
curl \
  -X POST \
  --header "Content-Type: application/sparql-update" \
  -d "$QUERY" \
  "http://localhost:8080/sparql"
```

## General Information

- [Corese website](https://project.inria.fr/corese)
- [Source code](https://github.com/Wimmics/corese)
- [Corese server demo](http://corese.inria.fr/)
- [Changelog](https://github.com/Wimmics/corese/blob/master/CHANGELOG.md)
- **Mailing list:** corese-users at inria.fr
- **Subscribe to mailing list:** corese-users-request at inria.fr **subject:** subscribe
