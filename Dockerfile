# syntax=docker/dockerfile:1
# Default Dockerfile at repo root for hosts that only look for ./Dockerfile (e.g. some Railway setups).
# Prefer configuring `dockerfilePath` in railway.toml to services/<name>/Dockerfile to avoid drift.
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
