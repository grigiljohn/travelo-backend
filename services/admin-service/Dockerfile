# syntax=docker/dockerfile:1.6
FROM maven:3.9.6-eclipse-temurin-21 AS deps
WORKDIR /workspace
COPY pom.xml ./
COPY libs/commons/pom.xml libs/commons/
COPY libs/security/pom.xml libs/security/
COPY libs/events/pom.xml libs/events/
COPY config/config-server/pom.xml config/config-server/
COPY registry/service-registry/pom.xml registry/service-registry/
COPY services/api-gateway/pom.xml services/api-gateway/
COPY services/identity-service/pom.xml services/identity-service/
COPY services/social-service/pom.xml services/social-service/
COPY services/media-service/pom.xml services/media-service/
COPY services/discovery-service/pom.xml services/discovery-service/
COPY services/realtime-service/pom.xml services/realtime-service/
COPY services/commerce-service/pom.xml services/commerce-service/
COPY services/platform-service/pom.xml services/platform-service/
COPY services/admin-service/pom.xml services/admin-service/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl services/admin-service -am -f pom.xml dependency:go-offline -DskipTests \
 && cp -a /root/.m2 /opt/m2-offline

FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY --from=deps /opt/m2-offline /root/.m2
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl services/admin-service -am -f pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl tini ca-certificates \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system --gid 1001 travelo \
 && useradd  --system --uid 1001 --gid travelo --home-dir /app --shell /sbin/nologin travelo \
 && chown travelo:travelo /app
COPY --from=build --chown=travelo:travelo /workspace/services/admin-service/target/admin-service-*.jar /app/app.jar
USER travelo
EXPOSE 8190
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE="docker"
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fsS http://127.0.0.1:8190/actuator/health || exit 1
ENTRYPOINT ["/usr/bin/tini","--"]
CMD ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
