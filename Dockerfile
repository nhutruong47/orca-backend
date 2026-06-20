# Render builds this repository from the root. Since building Spring Boot inside 
# Render's 512MB RAM Docker container is extremely slow, we copy the locally 
# compiled JAR directly to achieve instant deployments.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY app-bin/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=75.0", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
