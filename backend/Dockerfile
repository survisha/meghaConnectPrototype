# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline

COPY libs ./libs
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp clean package

FROM eclipse-temurin:17.0.12_7-jre-jammy AS runtime
ARG BUILD_VERSION=unknown
ARG BUILD_REVISION=unknown
LABEL org.opencontainers.image.title="MeghaConnect Backend" \
      org.opencontainers.image.version="${BUILD_VERSION}" \
      org.opencontainers.image.revision="${BUILD_REVISION}"

RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 meghaconnect \
    && useradd --uid 10001 --gid meghaconnect --system --home-dir /app --shell /usr/sbin/nologin meghaconnect \
    && mkdir -p /app /opt/meghaconnect/uploads /opt/meghaconnect/logs /tmp/meghaconnect \
    && chown -R meghaconnect:meghaconnect /app /opt/meghaconnect /tmp/meghaconnect

WORKDIR /app
COPY --from=build --chown=meghaconnect:meghaconnect /workspace/target/meghaconnect-*.jar /app/app.jar

ENV BUILD_VERSION="${BUILD_VERSION}" \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0 -Djava.io.tmpdir=/tmp/meghaconnect -Dfile.encoding=UTF-8"

USER 10001:10001
EXPOSE 8080 9091
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD curl --fail --silent --show-error http://127.0.0.1:9091/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
