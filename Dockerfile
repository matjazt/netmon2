# Multi-stage build for Spring Boot application
# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src src

# Build the application (skip tests for faster builds)
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Create non-root user for security
RUN groupadd -r netmon && useradd -r -g netmon netmon

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Create log directory with proper permissions
RUN mkdir -p /app/log && chown -R netmon:netmon /app

# Switch to non-root user
USER netmon

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
