# syntax=docker/dockerfile:1
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl services/commerce-service -am -f pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/commerce-service/target/commerce-service-*.jar app.jar
EXPOSE 8097
ENTRYPOINT ["java","-jar","/app/app.jar"]
