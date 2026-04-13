# syntax=docker/dockerfile:1
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl registry/service-registry -am -f pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
# Install curl for health checks
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/registry/service-registry/target/service-registry-*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java","-jar","/app/app.jar"]
