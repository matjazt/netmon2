# Docker Guide for NetMon2

This guide covers running the NetMon 2 application using Docker Compose for both development and production.

## Configuration Files Overview

The project uses three key files for Docker deployment:

### Dockerfile

Defines how to build the application image. Uses a multi-stage build:

- **Stage 1 (builder)**: Compiles the Spring Boot application using Gradle
- **Stage 2 (runtime)**: Creates minimal runtime image with JRE and compiled JAR

The final image (~300MB) runs as a non-root user and includes a health check endpoint.

### docker-compose.yml

Orchestrates the application container. References the Dockerfile for building and defines:

- Port mappings (8080:8080)
- Volume mounts for logs
- Environment variable references from `.env`
- Container restart policy

### .env

Stores environment‑specific configuration values that should not be hard‑coded into the application or committed to version control. Docker Compose loads these variables automatically and exposes them to the Spring Boot application, which then maps them to configuration properties using its relaxed binding rules

**Important:** Add `.env` to `.gitignore` to keep secrets out of version control. The `.env` file contains database credentials, MQTT credentials, SMTP passwords, and other environment-specific settings.

## Quick Start

### Prerequisites

- Docker and Docker Compose installed
- External PostgreSQL database accessible from container
- External MQTT broker accessible from container
- `.env` file configured with your credentials

### Running with Docker Compose

```powershell
# From project root
cd S:\matjazt\Src\netmon2

# Build and start in background
docker compose up -d

# View stdout 
docker compose logs -f netmon2

# Stop application
docker compose down
```

**Access the application:**

- Application: <http://localhost:8080>
- Health check: <http://localhost:8080/actuator/health> (no auth required)
- API: <http://localhost:8080/api/devices> (requires authentication)
- Swagger UI: <http://localhost:8080/swagger-ui.html>

## Environment Variables

The application requires several environment variables for database, MQTT, and email configuration. Store these in `.env` file in the project root (same directory as `docker-compose.yml`).

Docker Compose automatically loads `.env` and substitutes `${VARIABLE_NAME}` references in the compose file.

**Variable Categories:**

- Database connection (URL, username, password)
- MQTT broker settings (URL, credentials, topic pattern)
- Email/SMTP configuration (host, port, credentials)
- JVM options (memory settings)

See `.env` file for complete list of required and optional variables.

## Building and Rebuilding

### Initial Build

```powershell
# Build image from Dockerfile
docker compose build
```

### Rebuild Options

When you make changes to code or dependencies, you need to rebuild:

```powershell
# Rebuild and restart (keeps using cached layers when possible)
docker compose up -d --build

# Force complete rebuild (ignores all cache)
docker compose build --no-cache

# Rebuild and recreate containers even if image hasn't changed
docker compose up -d --build --force-recreate

# Nuclear option: clean everything and rebuild from scratch
docker compose down
docker compose build --no-cache
docker compose up -d
```

**When to use which:**

- `--build`: Code changes, dependency updates
- `--no-cache`: Gradle dependency issues, corrupted cached layers
- `--force-recreate`: Environment variable changes, volume mount issues
- `--pull`: Force pull base images (updates JDK from Docker Hub)

## Docker Compose Commands

### Basic Operations

```powershell
# Start application (foreground, see logs in terminal)
docker compose up

# Start in background (detached mode)
docker compose up -d

# Stop containers (keeps containers for restart)
docker compose stop

# Stop and remove containers (clean shutdown)
docker compose down

# Restart all services
docker compose restart

# Restart specific service
docker compose restart netmon2
```

### Viewing Status and Logs

```powershell
# List running services
docker compose ps

# View logs from all services
docker compose logs

# Follow logs in real-time
docker compose logs -f

# Follow logs for specific service
docker compose logs -f netmon2

# View last 100 lines
docker compose logs --tail=100 netmon2

# View logs with timestamps
docker compose logs -t netmon2
```

### Executing Commands

```powershell
# Open shell in running container
docker compose exec netmon2 bash

# Run single command
docker compose exec netmon2 ls -la /app

# Check Java version
docker compose exec netmon2 java -version

# Check environment variables
docker compose exec netmon2 env | grep SPRING
```

### Verifying Configuration

```powershell
# Show resolved configuration (with .env substitution)
docker compose config

# Validate compose file syntax
docker compose config --quiet
```

## Deploying to Production

### Using docker-compose.server.yml

If you have a separate production compose file:

```bash
# On production server
docker compose -f docker-compose.server.yml up -d

# View logs
docker compose -f docker-compose.server.yml logs -f

# Stop
docker compose -f docker-compose.server.yml down
```

However, you can also simply rename the server compose file to `docker-compose.yml` on the server.
The same applies to `.env` files.

### Transferring Image to Server

**Option 1: Docker Registry**

```powershell
# Tag image
docker tag netmon2:latest registry.example.com/netmon2:1.0.0

# Push to registry
docker push registry.example.com/netmon2:1.0.0

# On server: pull and run
docker pull registry.example.com/netmon2:1.0.0
docker compose up -d
```

**Option 2: Save/Load (no registry)**

```powershell
# On Windows: save image to file
docker save netmon2:latest | gzip > netmon2-latest.tar.gz

# Transfer to server
scp netmon2-latest.tar.gz user@server:/tmp/

# On server: load image
gunzip -c /tmp/netmon2-latest.tar.gz | docker load
docker compose up -d
```

## Troubleshooting

### Container Won't Start

```powershell
# Check logs for errors
docker compose logs netmon2

# Check container status
docker compose ps

# Common issues:
# - Database not reachable (check SPRING_DATASOURCE_URL in .env)
# - Wrong credentials in .env
# - Port already in use (change port mapping in docker-compose.yml)
# - Missing .env file (Docker Compose won't warn you!)
```

### Changes Not Reflected

```powershell
# If code changes don't appear:
docker compose up -d --build

# If .env changes don't apply:
docker compose down
docker compose up -d --force-recreate

# If dependencies won't update:
docker compose build --no-cache
```

### Build Fails

```powershell
# Build with verbose output
docker compose build --progress=plain

# Common issues:
# - Network timeout downloading Gradle dependencies (retry)
# - Gradle permission issues (check gradlew is executable)
# - Out of disk space (run docker system prune -a)
# - Base image pull fails (check internet connection)
```

### Database Connection Issues

```powershell
# Verify environment variables are set
docker compose config | grep DATASOURCE

# Test database connectivity from container
docker compose exec netmon2 ping your-db-host

# Check database logs in application
docker compose logs netmon2 | grep -i "database\|connection\|postgres"

# Common issues:
# - Database host unreachable (firewall, network, VPN)
# - Wrong URL format in .env
# - Database doesn't exist yet
# - Credentials invalid
```

### Application Logs Show Errors

```powershell
# MQTT connection errors
docker compose logs netmon2 | grep -i mqtt

# Email/SMTP errors
docker compose logs netmon2 | grep -i "mail\|smtp"

# Spring Boot startup errors
docker compose logs netmon2 | grep -i "error\|exception"
```

## Tips and Best Practices

### Managing Images and Resources

```powershell
# View all images
docker images

# Remove specific image
docker rmi netmon2:latest

# Remove unused images
docker image prune

# Remove all unused resources (images, containers, networks)
docker system prune -a

# Check disk usage
docker system df
```

### Secrets Management

**Never commit `.env` to version control!**

```gitignore
# Add to .gitignore
.env
.env.local
.env.production
```

Create `.env.example` as a template:

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/network_monitor
SPRING_DATASOURCE_USERNAME=changeme
SPRING_DATASOURCE_PASSWORD=changeme

# MQTT
MQTT_URL=ssl://broker.example.com:8883
# ... etc
```

### Using Different Environments

```powershell
# Development (uses .env)
docker compose up -d

# Production (uses .env.production)
docker compose --env-file .env.production up -d

# Staging (different compose file)
docker compose -f docker-compose.staging.yml up -d
```

### Health Checks

Monitor container health status:

```powershell
# Check health in compose
docker compose ps

# Inspect health check details
docker inspect netmon2-app | grep -A 10 Health

# Direct health endpoint access (no auth required)
curl http://localhost:8080/actuator/health
```

### Log Management

Prevent logs from consuming disk space:

```yaml
# In docker-compose.yml
services:
  netmon2:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### Resource Limits

Prevent container from consuming all system resources:

```yaml
# In docker-compose.yml
services:
  netmon2:
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 1024M
        reservations:
          cpus: '0.5'
          memory: 512M
```

### Version Tagging

Use semantic versioning instead of `latest`:

```powershell
# Build with version
docker compose build
docker tag netmon2:latest netmon2:1.0.0
docker tag netmon2:latest netmon2:stable

# In production, reference specific versions in compose file:
# image: netmon2:1.0.0
```

### Monitoring Performance

```powershell
# Real-time resource usage
docker stats netmon2-app

# Container resource limits and current usage
docker stats --no-stream

# JVM memory settings
# Adjust JAVA_OPTS in docker-compose.yml:
# JAVA_OPTS: "-Xms512m -Xmx1024m"
```

### Quick Debugging

```powershell
# Check if container is running
docker compose ps

# Get container IP address
docker inspect netmon2-app | grep IPAddress

# View environment variables inside container
docker compose exec netmon2 env

# Test network connectivity
docker compose exec netmon2 ping google.com

# Check listening ports
docker compose exec netmon2 netstat -tlnp
```

### Cleanup Commands

```powershell
# Stop and remove everything for this project
docker compose down --volumes --remove-orphans

# Remove all stopped containers
docker container prune

# Remove all unused volumes
docker volume prune

# Remove dangling images
docker image prune

# Nuclear option: clean everything on system
docker system prune -a --volumes
```

## Common Workflow Examples

### Development Workflow

```powershell
# 1. Make code changes
# ... edit Java files ...

# 2. Rebuild and restart
docker compose up -d --build

# 3. Watch logs
docker compose logs -f netmon2

# 4. Test changes
curl http://localhost:8080/actuator/health
```

### Updating Dependencies

```powershell
# 1. Update build.gradle.kts
# ... add/update dependencies ...

# 2. Force rebuild without cache
docker compose build --no-cache

# 3. Restart
docker compose up -d --force-recreate
```

### Changing Environment Variables

```powershell
# 1. Edit .env file
# ... update values ...

# 2. Recreate containers (rebuild not needed)
docker compose down
docker compose up -d

# 3. Verify new values
docker compose exec netmon2 env | grep SPRING
```

### Production Deployment

```powershell
# 1. On build machine: create versioned image
docker build -t netmon2:1.0.0 .
docker save netmon2:1.0.0 | gzip > netmon2-1.0.0.tar.gz

# 2. Transfer to server
scp netmon2-1.0.0.tar.gz server:/tmp/

# 3. On server: load and run
gunzip -c /tmp/netmon2-1.0.0.tar.gz | docker load
docker compose -f docker-compose-prod.yml up -d

# 4. Verify deployment
docker compose -f docker-compose-prod.yml ps
docker compose -f docker-compose-prod.yml logs --tail=50
curl http://localhost:8080/actuator/health
```

## Additional Resources

- **Docker Documentation**: <https://docs.docker.com/>
- **Docker Compose Reference**: <https://docs.docker.com/compose/compose-file/>
- **Spring Boot Docker Guide**: <https://spring.io/guides/topicals/spring-boot-docker/>

## Additional Resources

umentation**: <<https://www.postgresql.org/docs/>

- **Docker Documentation**: <https://docs.docker.com/>
- **Docker Compose Reference**: <https://docs.docker.com/compose/compose-file/>
- **Spring Boot Docker Guide**: <https://spring.io/guides/topicals/spring-boot-docker/>
- **PostgreSQL Docker Hub**: <https://hub.docker.com/_/postgres>
