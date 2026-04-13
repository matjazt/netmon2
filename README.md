# NetMon2

A Spring Boot application for monitoring network devices via MQTT with automated alerting, user authentication, and a REST API.

## Frontend GUI

A dedicated web GUI is available for this backend:

- <https://github.com/matjazt/netmon2Gui>

There is also a live public demo available, so check it out.

## Overview

This application subscribes to MQTT topics that publish network device lists, detects when devices go online or offline, stores state changes in a PostgreSQL database, triggers email alerts for critical events, and provides an authenticated REST API to query device status. It supports multi-user access with role-based account management and configurable device monitoring policies (unauthorized, authorized, always-on).

## Technology Stack

- **Spring Boot 4.0.3**: Modern Java application framework
- **Spring Data JPA**: Database persistence with Hibernate
- **Spring Security**: Authentication and authorization with BCrypt
- **Spring Integration MQTT**: MQTT connectivity with Eclipse Paho client
- **Spring Mail**: Email notification support
- **Logbook (Zalando)**: HTTP request/response logging
- **Caffeine**: In-memory caching
- **Gradle**: Build tool and dependency management
- **PostgreSQL**: Relational database for storing device history
- **MapStruct**: DTO mapping framework
- **Lombok**: Boilerplate reduction
- **Docker**: Containerized deployment with Docker Compose
- **SvcWatchDog**: Windows service watchdog integration (optional)

## Prerequisites

- JDK 21 (Adoptium recommended)
- Gradle (wrapper included)
- PostgreSQL server

## Database Setup

### 1. Create Database

```powershell
# Create database
psql -U postgres -c "CREATE DATABASE network_monitor;"
```

### 2. Run Schema Script

Execute the SQL script in `database/schema.sql` to create tables and seed reference data:

```powershell
psql -U postgres -d network_monitor -f database/schema.sql
```

This creates the following tables:

- **network**: Monitored networks
- **device**: Devices and their current state
- **device_status_history**: Historical state changes
- **alert**: Generated alerts (network down, device down, unauthorized devices)
- **account**: User accounts for API access
- **account_type**: Account role types (admin, user, device, viewer)
- **account_network**: User-network access mapping
- **alert_type**: Alert type reference data
- **device_operation_mode**: Device monitoring policy reference data
- **log**: Application log entries persisted to the database (linked to network/device)
- **log_level**: Log level reference data

**Note**: `database/NetworkMonitor.sql` contains development queries and should NOT be executed.

## Configuration

### Environment Variables (.env file)

The application supports configuration via environment variables using a `.env` file in the project root. This file is excluded from version control (`.gitignore`) to protect sensitive credentials.

Create a `.env` file with the following structure:

```bash
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://your-server:5432/your-database?currentSchema=your-schema&tcpKeepAlive=true
SPRING_DATASOURCE_USERNAME=your-db-username
SPRING_DATASOURCE_PASSWORD=your-db-password

# MQTT Configuration
MQTT_URL=ssl://broker.example.com:8883
MQTT_CLIENT_ID=netmon2
MQTT_USERNAME=your-mqtt-username
MQTT_PASSWORD=your-mqtt-password

# Email delivery (SMTP) configuration
EMAIL_DELIVERY_SMTP_HOST=smtp.example.com
EMAIL_DELIVERY_SMTP_PORT=587
EMAIL_DELIVERY_SMTP_USERNAME=alerts@example.com
EMAIL_DELIVERY_SMTP_PASSWORD=your-smtp-password
EMAIL_DELIVERY_SMTP_STARTTLS=true
EMAIL_DELIVERY_SMTP_AUTH=true

# Buffered email logging (sends aggregated log entries via email)
EMAIL_LOGGING_EMAIL_FROM=netmon2@example.com
EMAIL_LOGGING_EMAIL_TO=admin@example.com
EMAIL_LOGGING_MAX_COUNT=100
EMAIL_LOGGING_MAX_DELAY_SECONDS=600
EMAIL_LOGGING_MIN_LEVEL=WARN

# Alerter
ALERTER_FROM_EMAIL=alerts@example.com

# CORS (comma-separated list of allowed origins)
NETMON_CORS_ALLOWED_ORIGINS=https://app.example.com
```

These environment variables override the defaults in `application.yaml` and are automatically loaded when using Docker Compose or can be sourced in your shell before running the application.

### Application Configuration

**MQTT Settings:**

```yaml
mqtt:
  url: ssl://broker.example.com:8883  # MQTT broker URL
  client-id: netmon2                  # Unique client identifier
  username: your-username             # MQTT authentication
  password: your-password             # MQTT password
  topic-template: network/{networkName}/scan  # Topic pattern
  truststore-path: /path/to/truststore.jks    # Optional: for self-signed CAs
  truststore-password: changeit       # Optional: truststore password
  automatic-reconnect: true           # Enable automatic reconnection
  clean-session: false                # Persistent session
  qos: 1                              # Quality of Service (0, 1, or 2)
  connection-timeout: 30              # Connection timeout in seconds
  keep-alive-interval: 60             # Keep-alive interval in seconds
  completion-timeout: 5000            # Completion timeout in milliseconds
  ssl-verify-hostname: true           # Verify SSL hostname
```

**Email Delivery (SMTP) Settings:**

```yaml
email-delivery:
  smtp-host: smtp.example.com         # SMTP server hostname
  smtp-port: 587                      # SMTP port (587 for TLS)
  smtp-username: your-email           # SMTP username
  smtp-password: your-password        # SMTP password
  smtp-starttls: true                 # Enable STARTTLS
  smtp-auth: true                     # Enable SMTP authentication
```

**Buffered Email Logging Settings:**

```yaml
email-logging:
  email-from: netmon2@example.com     # Sender address for log emails
  email-to: admin@example.com         # Recipient address for log emails
  max-count: 100                      # Send when buffer reaches this many entries
  max-delay-seconds: 300              # Send after this many seconds regardless
  min-level: WARN                     # Minimum log level to buffer (ERROR, WARN, INFO, etc.)
```

**Alerter Settings:**

```yaml
alerter:
  from-email: alerts@example.com      # Sender email address for alert emails
  from-name: Network Monitor          # Sender display name
  interval-seconds: 20                # Alert check interval
  initial-delay-seconds: 30           # Initial delay before first check
```

### Database Connection

Configure in `src/main/resources/application-local.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/network_monitor
    username: postgres
    password: your-password
```

## Building

Build the application using Gradle:

```powershell
./gradlew build
```

This creates `build/libs/netmon2-0.0.1-SNAPSHOT.jar` - an executable JAR file.

## Running

### Run with Gradle

```powershell
./gradlew bootRun
```

### Run JAR Directly

```powershell
java -jar build/libs/netmon2-0.0.1-SNAPSHOT.jar
```

### Run with Docker

The project includes Docker configuration for containerized deployment. See [docs/Docker.md](docs/Docker.md) for complete Docker setup and usage instructions.

Quick start:

```powershell
# Build and run with Docker Compose
docker compose up -d

# View logs
docker compose logs -f
```

### Application URLs

- REST API base: `http://127.0.0.1:8080/netmon2/api/`
- Swagger UI: `http://127.0.0.1:8080/netmon2/swagger-ui.html`
- OpenAPI specification: `http://127.0.0.1:8080/netmon2/v3/api-docs`
- Health check: `http://127.0.0.1:8080/netmon2/actuator/health`

## REST API

The application provides a comprehensive REST API documented via OpenAPI/Swagger. The full API reference — including all endpoints, request/response schemas, and authentication requirements — is available in the Swagger UI at:

```
http://127.0.0.1:8080/netmon2/swagger-ui.html
```

The machine-readable OpenAPI specification is available at:

```
http://127.0.0.1:8080/netmon2/v3/api-docs
```

**Authentication**: All API endpoints (except `/actuator/health`) require HTTP Basic Authentication. Credentials are validated against the `account` table with BCrypt password hashing.

**Available resource groups** (all under `/api/`):

| Path prefix | Description |
|---|---|
| `/api/accounts` | User account management (admin only) |
| `/api/account-networks` | User–network access mapping |
| `/api/networks` | Monitored network management |
| `/api/devices` | Device management and status |
| `/api/device-status-history` | Historical device state changes |
| `/api/alerts` | Alert records |
| `/api/logs` | Application log entries (persisted to DB) |

## How It Works

### MQTT Message Processing

1. Application starts and connects to MQTT broker
2. Subscribes to configured topics based on topic template and networks in database
3. Receives JSON messages with device lists:

```json
{
  "hostname": "Scanner",
  "timestamp": "2026-01-20T11:45:40+01:00",
  "devices": [
    {"ip": "192.168.1.1", "mac": "AA:BB:CC:DD:EE:FF"}
  ]
}
```

1. For each device in message (online devices):
   - Checks previous status in database
   - If new or was offline: records "online" event
   - Creates/updates device record

2. For known devices not in message:
   - Records "offline" event

3. Only state changes are stored

### Network Scanners

The application receives device data from network scanner scripts deployed on routers or dedicated devices. These scripts scan local networks and publish results to MQTT.

Available scanner implementations in the [network-scanners/](network-scanners/) folder:

- **RouterOS 7**: Script for MikroTik routers with ARP scanning (see [network-scanners/RouterOS/](network-scanners/RouterOS/))

### Device Operation Modes

Devices can be configured with three operation modes:

- **UNAUTHORIZED (0)**: Device is not allowed on the network - triggers alerts
- **AUTHORIZED (1)**: Device is allowed but not actively monitored
- **ALWAYS_ON (2)**: Device should always be online - triggers alerts when offline

### Alert System

The `AlerterService` runs on a configurable schedule (default: every 20 seconds) and checks for:

1. **NETWORK_DOWN**: Network hasn't sent data within configured `alerting_delay` period
2. **DEVICE_DOWN**: An ALWAYS_ON device is offline
3. **DEVICE_UNAUTHORIZED**: An UNAUTHORIZED device appears online

When triggered:

- Alert record created in database
- Email notification sent to configured address (if set on network)
- Alert remains active until condition clears
- Closure email sent when alert resolves

### Account Management

Users authenticate via Spring Security:

- Passwords stored as BCrypt hashes
- Account types: admin, user, device (for MQTT publishers), viewer (read-only)
- User-network access control via `account_network` junction table
- Security context available throughout application

## Project Structure

```txt
netmon2/
├── build.gradle.kts                 # Gradle project configuration
├── settings.gradle.kts              # Gradle settings
├── Dockerfile                       # Docker image configuration
├── docker-compose.yml               # Docker Compose configuration
├── .dockerignore                    # Docker build exclusions
├── .env                             # Environment variables (not in git)
├── database/                        # Database scripts
│   └── schema.sql                   # Database schema DDL
├── docs/                            # Documentation
│   ├── MqttMessageFormat.md         # MQTT message format and examples
│   └── Docker.md                    # Docker setup and usage guide
├── log/                             # Application log files (not in git)
├── network-scanners/                # Network scanner implementations
│   └── RouterOS/                    # MikroTik RouterOS scanner
│       ├── networkScan.rsc          # RouterOS script
│       ├── networkScan.json         # Configuration file
│       └── networkScan.RouterOS.md  # Installation guide
└── src/
    ├── main/
    │   ├── java/com/matjazt/
    │   │   ├── netmon2/
    │   │   │   ├── config/          # Spring configuration classes
    │   │   │   ├── controller/      # REST controllers
    │   │   │   ├── dto/             # Data Transfer Objects
    │   │   │   ├── entity/          # JPA entities
    │   │   │   ├── mapper/          # MapStruct mappers
    │   │   │   ├── repository/      # Spring Data repositories
    │   │   │   ├── security/        # Spring Security configuration
    │   │   │   └── service/         # Business logic services
    │   │   └── tools/               # Shared utilities (SvcWatchDogClient)
    │   └── resources/
    │       ├── application.yaml     # Default configuration
    │       └── application-local.yaml  # Local overrides (not in git, create manually)
    └── test/                        # Unit and integration tests
```

## Features

✅ **MQTT Integration**: Subscribe to device scan results from network scanners  
✅ **State Change Detection**: Track devices going online/offline  
✅ **Historical Tracking**: Store all state changes with timestamps  
✅ **Multi-Network Support**: Monitor multiple networks simultaneously  
✅ **Device Management**: Classify devices as unauthorized, authorized, or always-on  
✅ **MAC Vendor Lookup**: Automatically resolves device vendor names from the IEEE OUI registry  
✅ **Automated Alerting**: Email notifications for network/device issues  
✅ **Email Log Buffering**: Aggregated log entries sent via email on a configurable schedule  
✅ **Persistent Log Storage**: Application log entries persisted to the database (linked to network/device)  
✅ **User Authentication**: Secure API with Spring Security and BCrypt  
✅ **Role-Based Access**: Admin, user, device, and viewer account types  
✅ **CORS Configuration**: Configurable allowed origins via properties or environment variable  
✅ **HTTP Request Logging**: Full HTTP request/response logging via Logbook  
✅ **REST API**: Query current status and historical data  
✅ **OpenAPI Documentation**: Auto-generated API docs via SpringDoc  
✅ **SvcWatchDog Support**: Optional Windows service watchdog integration

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
