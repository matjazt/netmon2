package com.matjazt.tools;

/**
 * Example usage of SvcWatchDogClient.
 *
 * <h2>Basic Usage</h2>
 *
 * <pre>{@code
 * // 1. Create and start the watchdog client
 * SvcWatchDogClient watchdog = new SvcWatchDogClient();
 * SvcWatchDogClient.main = watchdog;
 * watchdog.start();
 *
 * // 2. In your main loop, ping the watchdog periodically
 * while (!watchdog.isTimedOut()) {
 *     // Do your work here
 *
 *     // Ping the watchdog to indicate the application is alive
 *     watchdog.ping("mainLoop", 30); // 30 seconds timeout
 *
 *     // Check for shutdown signal
 *     if (watchdog.waitForShutdownEvent(1000)) {
 *         break; // Shutdown requested
 *     }
 * }
 *
 * // 3. Clean up
 * watchdog.stop();
 * }</pre>
 *
 * <h2>Using TimeoutDetector</h2>
 *
 * <pre>{@code
 * // For operations that should complete within a specific timeout
 * try (SvcWatchDogClient.TimeoutDetector detector =
 *         new SvcWatchDogClient.TimeoutDetector("databaseQuery", 60)) {
 *     // Perform database query
 *     // If this takes longer than 60 seconds, the watchdog will detect it
 *     performLongRunningOperation();
 * } // Timeout is automatically closed when leaving the try block
 * }</pre>
 *
 * <h2>Configuration</h2>
 *
 * <p>The watchdog client can be configured using system properties:
 *
 * <ul>
 *   <li>{@code svcwatchdog.udpPingInterval} - UDP ping interval in seconds (default: 10)
 *   <li>{@code svcwatchdog.timeSkewRecoveryInterval} - Time skew recovery interval in seconds
 *       (default: 60)
 * </ul>
 *
 * <p>Environment variables used by SvcWatchDog:
 *
 * <ul>
 *   <li>{@code SHUTDOWN_EVENT} - Name of the Win32 event for shutdown signaling (Windows only)
 *   <li>{@code WATCHDOG_PORT} - UDP port for sending watchdog pings
 *   <li>{@code WATCHDOG_SECRET} - Secret string to include in UDP pings
 * </ul>
 *
 * <h2>Platform Support</h2>
 *
 * <p>The watchdog client works on both Windows and Linux:
 *
 * <ul>
 *   <li><b>Windows:</b> Full support including Win32 event-based shutdown signaling and UDP pinging
 *   <li><b>Linux:</b> UDP pinging and timeout detection work normally. Shutdown event waiting falls
 *       back to simple sleep (no event support)
 * </ul>
 *
 * <h2>Thread Safety</h2>
 *
 * <p>All public methods of SvcWatchDogClient are thread-safe and can be called from multiple
 * threads.
 */
public class SvcWatchDogClientExample {

    private SvcWatchDogClientExample() {
        // Example class, not meant to be instantiated
    }

    /** Example: Simple application with watchdog monitoring. */
    public static void simpleExample() {
        // Create and start the watchdog
        SvcWatchDogClient watchdog = SvcWatchDogClient.getInstance();
        watchdog.ping("mainLoop", 10);
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

    /** Example: Using TimeoutDetector for monitoring specific operations. */
    public static void timeoutDetectorExample() {
        // Assume watchdog is already started

        // Monitor a specific operation with a timeout
        try (SvcWatchDogClient.TimeoutDetector detector =
                new SvcWatchDogClient.TimeoutDetector("databaseQuery", 60)) {
            // Perform database query
            // If this takes longer than 60 seconds, the watchdog will detect it
            performDatabaseQuery();
        } // Timeout is automatically closed when leaving the try block

        // Monitor another operation
        try (SvcWatchDogClient.TimeoutDetector detector =
                new SvcWatchDogClient.TimeoutDetector("fileProcessing", 120)) {
            processLargeFile();
        }
    }

    /** Example: Spring Boot application with watchdog. */
    public static void springBootExample() {
        // In a Spring Boot application, you might want to create the watchdog
        // as a bean and start it during application startup

        // @Configuration
        // public class WatchdogConfig {
        //
        //     @Bean
        //     public SvcWatchDogClient watchdogClient() {
        //         SvcWatchDogClient client = new SvcWatchDogClient();
        //         SvcWatchDogClient.main = client;
        //         return client;
        //     }
        //
        //     @EventListener(ApplicationReadyEvent.class)
        //     public void onApplicationReady() {
        //         SvcWatchDogClient.main.start();
        //     }
        //
        //     @PreDestroy
        //     public void onShutdown() {
        //         SvcWatchDogClient.main.stop();
        //     }
        // }

        // Then in your services, you can use it:
        // try (SvcWatchDogClient.TimeoutDetector detector =
        //         new SvcWatchDogClient.TimeoutDetector("apiCall", 30)) {
        //     callExternalApi();
        // }
    }

    private static void doWork() {
        // Simulate work
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void performDatabaseQuery() {
        // Simulate database query
    }

    private static void processLargeFile() {
        // Simulate file processing
    }
}
