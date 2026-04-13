# syntax=docker/dockerfile:1
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl services/platform-service -am -f pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/platform-service/target/platform-service-*.jar app.jar
EXPOSE 8099
ENTRYPOINT ["java","-jar","/app/app.jar"]
