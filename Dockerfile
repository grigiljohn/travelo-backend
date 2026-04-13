# syntax=docker/dockerfile:1
# Repo root Dockerfile: Railway’s default detector looks here first. Kept in sync with
# services/api-gateway/Dockerfile; for other services point railway.toml at services/<name>/Dockerfile.
#
# Default build: API gateway (port 8080). For social-service, copy services/social-service/Dockerfile here
# or point Railway "Dockerfile path" to services/social-service/Dockerfile.

FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl services/api-gateway -am -f pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/api-gateway/target/api-gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
