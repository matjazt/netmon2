# SvcWatchDogClient for Java

This is a Java implementation of the SvcWatchDogClient, compatible with the [SvcWatchDog](https://github.com/matjazt/SvcWatchDog) Windows service utility.

## Overview

SvcWatchDogClient provides a watchdog mechanism for monitoring and managing timeouts of registered tasks in Java applications. It integrates with the external SvcWatchDog utility to:

1. **Send periodic UDP heartbeat pings** to indicate the application is alive
2. **Detect shutdown signals** via Win32 events (on Windows)
3. **Monitor task timeouts** to detect frozen threads or hung operations
4. **Automatically stop UDP pings** when timeouts are detected

## Features

- ✅ **Java 17+ compatible** (tested with Java 21)
- ✅ **No Spring Boot or JEE dependency** - pure Java implementation
- ✅ **Cross-platform support** - works on Windows and Linux
- ✅ **Thread-safe** - all public methods can be called from multiple threads
- ✅ **Win32 event support** via JNA (Windows only)
- ✅ **UDP heartbeat pinging** to SvcWatchDog
- ✅ **Time skew detection** (handles sleep/hibernation)
- ✅ **Automatic resource cleanup** with try-with-resources

## Requirements

- Java 17 or higher
- JNA library (for Win32 event support on Windows)
  - `net.java.dev.jna:jna:5.13.0`
  - `net.java.dev.jna:jna-platform:5.13.0`

## Installation

Add the following dependencies to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("net.java.dev.jna:jna:5.13.0")
    implementation("net.java.dev.jna:jna-platform:5.13.0")
}
```

Or for Maven:

```xml
<dependency>
    <groupId>net.java.dev.jna</groupId>
    <artifactId>jna</artifactId>
    <version>5.13.0</version>
</dependency>
<dependency>
    <groupId>net.java.dev.jna</groupId>
    <artifactId>jna-platform</artifactId>
    <version>5.13.0</version>
</dependency>
```

## Usage

### Basic Example

```java
public class MyApplication {
    public static void main(String[] args) {
        // Get the singleton instance and start the watchdog client
        SvcWatchDogClient watchdog = SvcWatchDogClient.getInstance();

        // Register a timeout (Ping) first, then Start the watchdog's background thread.
        // Doing it the other way around could result in a (theoretical) freeze between Start and Ping going unnoticed.
        watchdog.ping("mainLoop", 30);
        watchdog.start();
        
        try {
            // Main application loop
            while (!watchdog.isTimedOut()) {
                // Do your work here
                doWork();
                
                // Ping the watchdog to indicate the application is alive
                watchdog.ping("mainLoop", 30); // 30 seconds timeout
                
                // Check for shutdown signal (waits 1 second)
                if (watchdog.waitForShutdownEvent(1000)) {
                    System.out.println("Shutdown requested");
                    break;
                }
            }
            
            if (watchdog.isTimedOut()) {
                System.err.println("Application timed out!");
            }
        } finally {
            // Clean up
            watchdog.stop();
        }
    }
    
    private static void doWork() {
        // Your application logic here
    }
}
```

### Using TimeoutDetector

For operations that should complete within a specific timeout:

```java
try (SvcWatchDogClient.TimeoutDetector detector = 
        new SvcWatchDogClient.TimeoutDetector("databaseQuery", 60)) {
    // Perform database query
    // If this takes longer than 60 seconds, the watchdog will detect it
    performLongRunningOperation();
} // Timeout is automatically closed when leaving the try block
```

### Spring Boot Integration

```java
@Configuration
public class WatchdogConfig {
    
    @Bean
    public SvcWatchDogClient watchdogClient() {
        // Get the singleton instance
        return SvcWatchDogClient.getInstance();
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        SvcWatchDogClient.getInstance().start();
    }
    
    @PreDestroy
    public void onShutdown() {
        SvcWatchDogClient.getInstance().stop();
    }
}
```

Then in your services:

```java
@Service
public class MyService {
    
    public void performOperation() {
        try (SvcWatchDogClient.TimeoutDetector detector = 
                new SvcWatchDogClient.TimeoutDetector("apiCall", 30)) {
            callExternalApi();
        }
    }
}
```

## Configuration

### System Properties and Environment Variables

The client uses `SimpleTools.getConfigValue()` which checks **environment variables first**, then falls back to **system properties**. Environment variable names are derived from property names by converting to uppercase and replacing dots with underscores.

**Configuration Properties:**

| Property Name | Env Var Name | Type | Default | Description |
| ------------ | ------------ | ---- | ------- | ----------- |
| `svcwatchdog.enabled` | `SVCWATCHDOG_ENABLED` | Boolean | `true` | Enable/disable the watchdog client |
| `svcwatchdog.udpPingInterval` | `SVCWATCHDOG_UDPPINGINTERVAL` | Integer | `10` | UDP ping interval in seconds |
| `svcwatchdog.timeSkewRecoveryInterval` | `SVCWATCHDOG_TIMESKEWRECOVERYINTERVAL` | Long | `60` | Time skew recovery interval in seconds |
| **`SHUTDOWN_EVENT`** | **`SHUTDOWN_EVENT`** | String | `null` | **Environment variable only** (not really a property)<br>Win32 event name for shutdown signaling (Windows only) |
| **`WATCHDOG_PORT`** | **`WATCHDOG_PORT`** | String | `null` | **Environment variable only** (not really a property)<br>UDP port for sending watchdog pings |
| **`WATCHDOG_SECRET`** | **`WATCHDOG_SECRET`** | String | `null` | **Environment variable only** (not really a property)<br>Secret string to include in UDP pings |

**Examples:**

Using environment variables (production deployment):

```bash
export SVCWATCHDOG_ENABLED=true
export SVCWATCHDOG_UDPPINGINTERVAL=15
export WATCHDOG_PORT=12345
export WATCHDOG_SECRET=mySecret
export SHUTDOWN_EVENT=myShutdownEvent
```

Using system properties (unit testing only):

```bash
java -Dsvcwatchdog.enabled=true \
     -Dsvcwatchdog.udpPingInterval=15 \
     -DWATCHDOG_PORT=12345 \
     -DWATCHDOG_SECRET=mySecret \
     -DSHUTDOWN_EVENT=myShutdownEvent \
     -jar myapp.jar
```

**Important Notes:**

- **`WATCHDOG_PORT`, `WATCHDOG_SECRET`, and `SHUTDOWN_EVENT` should always be environment variables** in production. They are typically set by the external SvcWatchDog tool when it starts your application.
- **System property fallback exists solely for unit testing purposes.** The code reads these as system properties only when they are not present in the environment, allowing tests to configure them without modifying environment variables (which cannot be changed after JVM startup).
- Configuration properties like `svcwatchdog.enabled` can legitimately use either environment variables or system properties.

## Platform Support

### Windows

- ✅ Full support including Win32 event-based shutdown signaling
- ✅ UDP heartbeat pinging
- ✅ Timeout detection
- ✅ Time skew detection

### Linux

- ✅ UDP heartbeat pinging
- ✅ Timeout detection
- ✅ Time skew detection
- ⚠️ Shutdown event waiting falls back to simple sleep (no event support)

## How It Works

1. **Singleton Initialization**: The client uses the Bill Pugh singleton pattern (`SingletonHolder`) for thread-safe lazy initialization
2. **Configuration**: On construction, reads `svcwatchdog.enabled` property (default: `true`)
3. **Start**: Call `getInstance().start()` to begin monitoring
4. **Background Loop**: A daemon thread monitors registered tasks and sends UDP pings if configured
5. **Task Registration**: Call `ping(taskName, timeoutSeconds)` to register or refresh a task
6. **Timeout Detection**: If a task doesn't ping within its timeout, it's marked as timed out and UDP pinging stops
7. **Shutdown Detection**: On Windows, monitors Win32 events via JNA; on Linux, uses simple sleep
8. **UDP Pinging**: Sends periodic UDP packets to SvcWatchDog if `WATCHDOG_PORT` and `WATCHDOG_SECRET` are configured
9. **Time Skew Handling**: Detects system sleep/hibernation (time jumps > 5 seconds) and ignores timeouts during recovery period

## Architecture

- **Singleton Pattern**: Uses Bill Pugh holder pattern for thread-safe lazy initialization
- **Configuration**: Environment variables take precedence over system properties via `SimpleTools.getConfigValue()`
- **Thread Safety**: Uses `ConcurrentHashMap` for tasks, synchronized sets for timed-out tasks, and atomic variables for state
- **Resource Management**: Implements `Closeable` for automatic cleanup with try-with-resources

## Important Notes

### Timeout Values

⚠️ **Set large timeout values!** The watchdog is designed to detect frozen applications or threads, not minor performance hiccups. Use timeouts of at least 30-60 seconds for production systems to avoid false positives during:

- Garbage collection pauses
- Temporary system overload
- Virtualized environment delays
- Network latency spikes

### UDP Ping Interval

The UDP ping interval should be **about half** the watchdog timeout configured in SvcWatchDog. For example:

- If SvcWatchDog `watchdogTimeout` is 30000 ms (30 seconds)
- Set `svcwatchdog.udpPingInterval` to 10-15 seconds

### Time Skew Recovery

When the system wakes from sleep or hibernation, the watchdog detects the time jump and ignores timeouts for a configurable period (default: 60 seconds). This prevents false positives.

## Thread Safety

All public methods of `SvcWatchDogClient` are thread-safe and can be called from multiple threads simultaneously.

## License

This implementation follows the same license as the SvcWatchDog project (MIT License).

## See Also

- [SvcWatchDog](https://github.com/matjazt/SvcWatchDog) - The main Windows service utility
- [C# Implementation](https://github.com/matjazt/NotificationServer/blob/main/NotificationServer/SmoothLib/SvcWatchDogClient.cs) - Original C# implementation
