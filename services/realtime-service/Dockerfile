# syntax=docker/dockerfile:1
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl services/realtime-service -am -f pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/realtime-service/target/realtime-service-*.jar app.jar
EXPOSE 8098
ENTRYPOINT ["java","-jar","/app/app.jar"]
