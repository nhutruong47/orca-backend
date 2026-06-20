# Render builds this repository from the root. This Dockerfile packages the
# Spring Boot backend now that the backend source lives under ./backend.
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Optimize JVM settings for Maven in low-memory container (512MB on Render Free)
ENV MAVEN_OPTS="-XX:+UseSerialGC -Xss512k -XX:MaxRAMPercentage=75.0"

COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

COPY backend/src ./src
# Build package using offline mode to avoid remote checking and save build time
RUN mvn package -o -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=75.0", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
