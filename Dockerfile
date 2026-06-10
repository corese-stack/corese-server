FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /build
COPY . .
RUN ./gradlew shadowJar --no-daemon -q

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=build /build/build/libs/corese-server-4.6.4-app.jar server.jar

RUN mkdir -p /data

ENV PORT=8080
ENV CORESE_DATA_PATH=""
ENV CORESE_DUMP_PATH=/data/dump.nt
ENV CORESE_AUTH_ENABLED=false
ENV CORESE_DUMP_INTERVAL=300

EXPOSE 8080
VOLUME ["/data"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD wget -q -O- http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "server.jar"]