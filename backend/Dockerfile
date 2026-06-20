# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first (cached layer, speeds up rebuilds)
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/rugby-planner-1.0.0.jar app.jar

# Render sets $PORT at runtime; Spring Boot reads server.port from the
# environment via SERVER_PORT, so we map it here.
ENV SERVER_PORT=${PORT:-8080}
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]
