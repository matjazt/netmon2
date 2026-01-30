package com.matjazt.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SvcWatchDogClient.
 *
 * <p>These tests verify the functionality of the watchdog client including: - Task registration and
 * timeout detection - UDP pinging - Shutdown event handling - TimeoutDetector usage - Disabled mode
 * behavior
 */
class SvcWatchDogClientTests {

    private static final String SHUTDOWN_EVENT_NAME = "shutDownEvent";
    private WinNT.HANDLE shutdownEventHandle;

    @BeforeEach
    void setUp() {
        // Clear any previous system properties
        System.clearProperty("svcwatchdog.enabled");
        System.clearProperty("svcwatchdog.udpPingInterval");
        System.clearProperty("svcwatchdog.timeSkewRecoveryInterval");
    }

    @AfterEach
    void tearDown() {
        // Clean up Win32 event handle if created
        if (shutdownEventHandle != null && Platform.isWindows()) {
            Kernel32.INSTANCE.CloseHandle(shutdownEventHandle);
            shutdownEventHandle = null;
        }

        // Clear environment variables (note: this doesn't actually clear them in Java,
        // but we can clear system properties)
        System.clearProperty("svcwatchdog.enabled");
        System.clearProperty("svcwatchdog.udpPingInterval");
        System.clearProperty("svcwatchdog.timeSkewRecoveryInterval");
    }

    /**
     * Simulates what external SvcWatchDog would do: sets "environment variables" and creates a
     * Win32 event for shutdown signaling.
     */
    private WinNT.HANDLE simulateExternalWatchdog() {
        // these should be environment variables, but since Java cannot set environment variables at
        // runtime,
        // we use system properties for testing purposes. In production, these would be set by the
        // external SvcWatchDog service.
        // To support testing, SvcWatchDogClient reads them via SimpleTools which checks env vars
        // first, then system properties.
        System.setProperty("SHUTDOWN_EVENT", SHUTDOWN_EVENT_NAME);
        System.setProperty("WATCHDOG_SECRET", "rubbish");
        System.setProperty("WATCHDOG_PORT", "12345");

        // Create Win32 event on Windows
        if (Platform.isWindows()) {
            return Kernel32.INSTANCE.CreateEvent(
                    null,
                    true, // Manual reset
                    false, // Initial state (not signaled)
                    SHUTDOWN_EVENT_NAME);
        }
        return null;
    }

    /**
     * Test a normally enabled SvcWatchDogClient. This test verifies: - Initial state - Starting the
     * client - Task registration and timeout - TimeoutDetector usage
     *
     * <p>NOTE: UDP pinging cannot be tested reliably because environment variables cannot be
     * modified after JVM start.
     */
    @Test
    void watchDogTest1() throws InterruptedException {
        System.out.println("Starting watchDogTest1");

        System.setProperty("svcwatchdog.enabled", "true");
        try (var wd = new SvcWatchDogClient()) {
            // Act & assert
            String task1 = "task1";
            String task2 = "task2";

            assertFalse(wd.isTimedOut());
            assertTrue(wd.getTaskList().isEmpty());

            wd.start();
            // Note: UDP pinging may or may not be active depending on whether WATCHDOG_PORT env var
            // is
            // set
            // This is set externally by SvcWatchDog when running as a Windows service
            assertTrue(wd.getTaskList().size() <= 1); // 0 or 1 (UDP ping task if env vars are set)
            assertFalse(wd.isTimedOut());

            wd.ping(task1, 5);
            Thread.sleep(1000);
            assertTrue(wd.getTaskList().size() >= 1); // At least task1
            assertFalse(wd.isTimedOut());

            try (SvcWatchDogClient.TimeoutDetector detector =
                    new SvcWatchDogClient.TimeoutDetector(task2, 2, wd)) {
                assertTrue(wd.getTaskList().size() >= 2); // At least task1 and task2
                Thread.sleep(1000);
            }

            assertTrue(wd.getTaskList().size() >= 1); // At least task1
            assertFalse(wd.isTimedOut());

            Thread.sleep(2000);
            // task2 should be removed (via try-with-resources)
            // task1 might still be active or timed out depending on timing
            assertFalse(wd.isTimedOut());

            Thread.sleep(3000);
            // Now task1 should have timed out
            assertTrue(wd.isTimedOut());

            // Cleanup
            wd.stop();
        }
    }

    /**
     * Test a disabled SvcWatchDogClient. When disabled, the client should not perform any
     * monitoring or UDP pinging.
     */
    @Test
    void watchDogTest2() throws InterruptedException {
        System.out.println("Starting watchDogTest2");

        // Arrange
        shutdownEventHandle = simulateExternalWatchdog();

        System.setProperty("svcwatchdog.enabled", "false");
        try (var wd = new SvcWatchDogClient()) {
            // Act & assert
            String task1 = "task1";
            String task2 = "task2";

            assertFalse(wd.isUdpPingingActive());
            assertFalse(wd.isTimedOut());
            assertTrue(wd.getTaskList().isEmpty());

            wd.start();
            assertTrue(wd.getTaskList().isEmpty());
            assertFalse(wd.isUdpPingingActive());
            assertFalse(wd.isTimedOut());

            wd.ping(task1, 1);
            assertTrue(wd.getTaskList().isEmpty());

            Thread.sleep(1200);
            assertTrue(wd.getTaskList().isEmpty());
            assertFalse(wd.isUdpPingingActive());
            assertFalse(wd.isTimedOut());

            try (SvcWatchDogClient.TimeoutDetector detector =
                    new SvcWatchDogClient.TimeoutDetector(task2, 3, wd)) {
                Thread.sleep(1000);
            }

            assertTrue(wd.getTaskList().isEmpty());
            assertFalse(wd.isUdpPingingActive());
            assertFalse(wd.isTimedOut());

            // Cleanup
            wd.stop();
        }
    }

    /**
     * Test an enabled SvcWatchDogClient without external SvcWatchDog. This verifies that the client
     * works correctly even without the external watchdog process.
     */
    @Test
    void watchDogTest3() throws InterruptedException {
        System.out.println("Starting watchDogTest3");

        // Arrange
        System.clearProperty("WATCHDOG_SECRET");
        System.clearProperty("WATCHDOG_PORT");

        System.setProperty("svcwatchdog.enabled", "true");
        try (var wd = new SvcWatchDogClient()) {
            // Act & assert
            String task1 = "task1";
            String task2 = "task2";

            assertFalse(wd.isUdpPingingActive());
            assertFalse(wd.isTimedOut());
            assertTrue(wd.getTaskList().isEmpty());

            wd.start();
            assertTrue(wd.getTaskList().isEmpty());
            assertFalse(wd.isUdpPingingActive());
            assertFalse(wd.isTimedOut());

            System.out.println("Pinging task1");

            wd.ping(task1, 1);
            assertEquals(1, wd.getTaskList().size());

            System.out.println("Sleeping 1200 ms");
            Thread.sleep(1200);
            System.out.println("End Sleeping 1200 ms");
            assertTrue(wd.getTaskList().isEmpty());
            assertFalse(wd.isUdpPingingActive());
            assertTrue(wd.isTimedOut());

            try (SvcWatchDogClient.TimeoutDetector detector =
                    new SvcWatchDogClient.TimeoutDetector(task2, 1, wd)) {
                Thread.sleep(1200);
            }

            assertTrue(wd.getTaskList().isEmpty());
            assertFalse(wd.isUdpPingingActive());
            assertTrue(wd.isTimedOut());

            // Cleanup
            wd.stop();
        }
    }

    /**
     * Test shutdown event signaling on Windows. This test only runs on Windows platform and
     * verifies that the client correctly detects shutdown signals.
     *
     * <p>NOTE: This test requires SHUTDOWN_EVENT environment variable to be set before JVM starts.
     * In production, this is set by the external SvcWatchDog service.
     */
    @Test
    void shutdownEventTest() throws InterruptedException {
        if (!Platform.isWindows()) {
            System.out.println("Skipping shutdownEventTest - Windows only");
            return;
        }

        System.out.println("Starting shutdownEventTest");

        System.setProperty("svcwatchdog.enabled", "true");
        try (var wd = new SvcWatchDogClient()) {
            // Create the shutdown event
            WinNT.HANDLE eventHandle =
                    Kernel32.INSTANCE.CreateEvent(
                            null,
                            true, // Manual reset
                            false, // Initial state (not signaled)
                            SHUTDOWN_EVENT_NAME);

            if (eventHandle == null) {
                System.out.println("Skipping shutdownEventTest - Could not create Win32 event");
                return;
            }

            try {
                // Act & assert
                wd.start();

                // Test that waiting without signal times out
                long start = System.currentTimeMillis();
                assertFalse(wd.waitForShutdownEvent(500));
                long elapsed = System.currentTimeMillis() - start;
                assertTrue(
                        elapsed >= 450 && elapsed <= 750,
                        "Wait should take ~500ms, took: " + elapsed);

                // Signal the event
                Kernel32.INSTANCE.SetEvent(eventHandle);

                // Test that waiting with signal returns immediately
                start = System.currentTimeMillis();
                assertTrue(wd.waitForShutdownEvent(5000));
                elapsed = System.currentTimeMillis() - start;
                assertTrue(elapsed < 200, "Wait should return immediately, took: " + elapsed);

                // Cleanup
                wd.stop();

                System.out.println("ShutdownEventTest completed");
            } finally {
                // Clean up the event handle
                Kernel32.INSTANCE.CloseHandle(eventHandle);
            }
        }
    }

    /**
     * Test TimeoutDetector with named task (no postfix). This verifies that tasks can be created
     * with specific names without UUID postfixes.
     */
    @Test
    void timeoutDetectorNamedTest() throws InterruptedException {
        System.out.println("Starting timeoutDetectorNamedTest");

        System.setProperty("svcwatchdog.enabled", "true");
        try (var wd = new SvcWatchDogClient()) {
            // Act & assert
            wd.start();

            String taskName = "namedTask";

            try (SvcWatchDogClient.TimeoutDetector detector =
                    new SvcWatchDogClient.TimeoutDetector(taskName, 2, false, wd)) {
                assertEquals(taskName, detector.getName());
                assertTrue(wd.getTaskList().size() >= 1); // At least the named task
                assertTrue(wd.getTaskList().contains(taskName));
            }

            // After closing, task should be removed
            Thread.sleep(100);
            assertFalse(wd.getTaskList().contains(taskName));

            // Cleanup
            wd.stop();
        }
    }

    /**
     * Test multiple simultaneous TimeoutDetectors. This verifies that multiple tasks can be
     * monitored concurrently.
     */
    @Test
    void multipleTimeoutDetectorsTest() throws InterruptedException {
        System.out.println("Starting multipleTimeoutDetectorsTest");

        System.setProperty("svcwatchdog.enabled", "true");
        try (var wd = new SvcWatchDogClient()) {
            // Act & assert
            wd.start();

            try (SvcWatchDogClient.TimeoutDetector detector1 =
                            new SvcWatchDogClient.TimeoutDetector("task1", 10, wd);
                    SvcWatchDogClient.TimeoutDetector detector2 =
                            new SvcWatchDogClient.TimeoutDetector("task2", 10, wd);
                    SvcWatchDogClient.TimeoutDetector detector3 =
                            new SvcWatchDogClient.TimeoutDetector("task3", 10, wd)) {

                // Should have at least 3 tasks (the 3 timeout detectors, maybe + UDP ping)
                assertTrue(wd.getTaskList().size() >= 3);
                assertFalse(wd.isTimedOut());
            }

            // After closing all, tasks should be removed
            Thread.sleep(100);
            assertTrue(
                    wd.getTaskList().size() <= 1); // Should have 0 or 1 (UDP ping if env vars set)
            assertFalse(wd.isTimedOut());

            // Cleanup
            wd.stop();
        }
    }
}
