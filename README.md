# NetMon2

A Spring Boot application for monitoring network devices via MQTT with automated alerting, user authentication, and a REST API.

## Overview

This application subscribes to MQTT topics that publish network device lists, detects when devices go online or offline, stores state changes in a PostgreSQL database, triggers email alerts for critical events, and provides an authenticated REST API to query device status. It supports multi-user access with role-based account management and configurable device monitoring policies (unauthorized, authorized, always-on).

## Technology Stack

- **Spring Boot 4.0.2**: Modern Java application framework
- **Spring Data JPA**: Database persistence with Hibernate
- **Spring Security**: Authentication and authorization with BCrypt
- **Spring Integration MQTT**: MQTT connectivity with Eclipse Paho client
- **Spring Mail**: Email notification support
- **Gradle**: Build tool and dependency management
- **PostgreSQL**: Relational database for storing device history
- **MapStruct**: DTO mapping framework
- **Docker**: Containerized deployment with Docker Compose

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
- **account_type**: Account role types (admin, user, device)
- **account_network**: User-network access mapping
- **alert_type**: Alert type reference data
- **device_operation_mode**: Device monitoring policy reference data

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

# Email/SMTP Configuration
ALERTER_SMTP_HOST=smtp.example.com
ALERTER_SMTP_PORT=587
ALERTER_SMTP_USERNAME=alerts@example.com
ALERTER_SMTP_PASSWORD=your-smtp-password
ALERTER_SMTP_START_TLS=true
ALERTER_SMTP_AUTH=true
ALERTER_FROM_EMAIL=alerts@example.com
```

These environment variables override the defaults in `application.yaml` and are automatically loaded when using Docker Compose or can be sourced in your shell before running the application.

### Application Configuration

Edit `src/main/resources/application-local.yaml` (or `application.yaml` for defaults):

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

**Email/SMTP Settings:**

```yaml
alerter:
  smtp-host: smtp.example.com         # SMTP server hostname
  smtp-port: 587                      # SMTP port (587 for TLS)
  smtp-username: your-email           # SMTP username
  smtp-password: your-password        # SMTP password
  smtp-start-tls: true                # Enable STARTTLS
  smtp-auth: true                     # Enable SMTP authentication
  from-email: alerts@example.com      # Sender email address
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

- Application: `http://127.0.0.1/netmon2/`
- REST API: `http://127.0.0.1/netmon2/api/devices`
- OpenAPI/Swagger UI: `http://127.0.0.1/netmon2/swagger-ui.html`

## REST API Endpoints

### Get All Devices (Paginated)

```text
GET /api/devices/paginated?page=0&size=20
```

Returns paginated list of device summaries.

### Get Device by ID

```text
GET /api/devices/{id}
```

Returns device details by ID.

### Get Devices by Network

```text
GET /api/devices/network/{networkId}
```

Returns all devices for a specific network.

### Get Online Devices

```text
GET /api/devices/network/{networkId}/online
```

Returns currently online devices for a network.

**Authentication**: API uses Spring Security with HTTP Basic Authentication. User credentials are validated against the `account` table with BCrypt password hashing.

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
- Account types: admin, user, device (for MQTT publishers)
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
├── network-scanners/                # Network scanner implementations
│   └── RouterOS/                    # MikroTik RouterOS scanner
│       ├── networkScan.rsc          # RouterOS script
│       ├── networkScan.json         # Configuration file
│       └── networkScan.RouterOS.md  # Installation guide
└── src/
    ├── main/
    │   ├── java/com/matjazt/netmon2/
    │   │   ├── config/              # Spring configuration classes
    │   │   ├── controller/          # REST controllers
    │   │   ├── dto/                 # Data Transfer Objects
    │   │   ├── entity/              # JPA entities
    │   │   ├── mapper/              # MapStruct mappers
    │   │   ├── repository/          # Spring Data repositories
    │   │   ├── security/            # Spring Security configuration
    │   │   └── service/             # Business logic services
    │   └── resources/
    │       ├── application.yaml     # Default configuration
    │       └── application-local.yaml  # Local overrides
    └── test/                        # Unit and integration tests
```

## Features

✅ **MQTT Integration**: Subscribe to device scan results from network scanners  
✅ **State Change Detection**: Track devices going online/offline  
✅ **Historical Tracking**: Store all state changes with timestamps  
✅ **Multi-Network Support**: Monitor multiple networks simultaneously  
✅ **Device Management**: Classify devices as unauthorized, authorized, or always-on  
✅ **Automated Alerting**: Email notifications for network/device issues  
✅ **User Authentication**: Secure API with Spring Security and BCrypt  
✅ **Role-Based Access**: Admin, user, and device account types  
✅ **REST API**: Query current status and historical data  
✅ **OpenAPI Documentation**: Auto-generated API docs via SpringDoc

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
