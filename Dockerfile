# Multi-stage build for Spring Boot application
# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy Maven build file
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -q || true

# Copy source code
COPY src src

# Build the application (skip tests for faster builds)
RUN mvn package -q -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Create non-root user for security
RUN groupadd -r netmon && useradd -r -g netmon netmon

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create log directory with proper permissions
RUN mkdir -p /app/log && chown -R netmon:netmon /app

# Switch to non-root user
USER netmon

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/netmon2/actuator/health/ || exit 1

# Run the application
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]