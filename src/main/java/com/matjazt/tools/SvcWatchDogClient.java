package com.matjazt.tools;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;

import java.io.Closeable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a watchdog client for monitoring and managing timeouts of registered tasks. The {@code
 * SvcWatchDogClient} class allows tasks to be registered with a timeout and detects when they
 * exceed their allowed execution time. If external SvcWatchDog is present, it also periodically
 * pings it via UDP and also detects its shutdown signaling.
 *
 * <p>IMPORTANT NOTE: SvcWatchDogClient is designed to detect if application or thread freezes. As
 * soon as such condition is detected, SvcWatchDogClient does everything it can to shut down the
 * application: it stops sending UDP pings and the isTimedOut starts returning true, possibly ending
 * the programs main loop.
 *
 * <p>It is therefore extremely important to set rather large timeout values, since you probably
 * don't want to restart the service for each minor performance hiccup.
 */
public class SvcWatchDogClient implements Closeable {

    private static final Logger logger = Logger.getLogger(SvcWatchDogClient.class.getName());

    // Runtime fields
    private final Object lock = new Object();
    private Thread backgroundThread;
    private final Object trigger = new Object();
    private final AtomicLong nextCheck = new AtomicLong(Long.MAX_VALUE);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final String udpPingTaskName = "_udpPing." + System.currentTimeMillis();
    private final Map<String, Long> tasks = new ConcurrentHashMap<>();
    private final Set<String> timedOutTasks = Collections.synchronizedSet(new HashSet<>());

    // Configuration fields
    private boolean enabled = true;
    private int udpPingInterval;
    private String shutdownEvent;
    private byte[] watchdogSecret;
    private InetAddress udpAddress;
    private int udpPort;
    private long timeSkewRecoveryInterval = 60; // seconds

    // Win32 event handle (only on Windows)
    private WinNT.HANDLE shutdownEventHandle;

    public static SvcWatchDogClient getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        static final SvcWatchDogClient INSTANCE = new SvcWatchDogClient();
    }

    /** Initializes a new instance of the {@code SvcWatchDogClient} class. */
    public SvcWatchDogClient() {
        enabled = SimpleTools.getConfigBoolean("svcwatchdog.enabled", true);
        logger.info("SvcWatchDogClient initialized, enabled=" + enabled);
    }

    /**
     * Initiates the background loop for monitoring and UDP pinging. NOTE: The ping method can be
     * called before starting the background loop. It is recommended to do so for the program's main
     * loop (or multiple loops), as this ensures there is no gap between starting UDP pings and
     * beginning thread monitoring.
     */
    public void start() {
        if (stopped.get()) {
            throw new IllegalStateException(
                    "SvcWatchDogClient is already stopped, not allowed to start it again.");
        }

        // for normal use, we're only interested in environment variable SHUTDOWN_EVENT, but since
        // Java can not set env vars at runtime, we also support system property for testing
        // purposes
        shutdownEvent = SimpleTools.getConfigString("SHUTDOWN_EVENT", null);

        if (!enabled) {
            logger.info("SvcWatchDogClient not enabled");
            return;
        }

        logger.info("Starting SvcWatchDogClient");

        udpPingInterval = SimpleTools.getConfigInteger("svcwatchdog.udpPingInterval", 10) * 1000;

        // see the shutdownEvent initialization comments above
        String watchdogSecretStr = SimpleTools.getConfigString("WATCHDOG_SECRET", null);
        watchdogSecret =
                watchdogSecretStr != null
                        ? watchdogSecretStr.getBytes(StandardCharsets.UTF_8)
                        : new byte[0];

        // see the shutdownEvent initialization comments above
        String watchdogPortStr = SimpleTools.getConfigString("WATCHDOG_PORT", null);
        if (watchdogPortStr != null && !watchdogPortStr.isEmpty()) {
            try {
                udpPort = Integer.parseInt(watchdogPortStr);
                if (udpPort > 0 && udpPingInterval > 0) {
                    udpAddress = InetAddress.getByName("127.0.0.1");
                    // Schedule the first immediate ping
                    tasks.put(udpPingTaskName, 1L);
                    logger.fine("UDP pinging configured");
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to configure UDP pinging", e);
            }
        }

        timeSkewRecoveryInterval =
                SimpleTools.getConfigLong("svcwatchdog.timeSkewRecoveryInterval", 60L);

        backgroundThread = new Thread(this::backgroundLoop, "SvcWatchDogClient-BackgroundLoop");
        backgroundThread.setDaemon(true);
        backgroundThread.start();

        logger.info("SvcWatchDogClient started");
    }

    /** Stops the background monitoring loop and releases resources. */
    public void stop() {
        logger.info("Stopping SvcWatchDogClient");
        stopped.set(true);

        synchronized (trigger) {
            trigger.notifyAll();
        }

        if (backgroundThread != null) {
            while (backgroundThread.isAlive()) {
                synchronized (trigger) {
                    trigger.notifyAll();
                }
                try {
                    backgroundThread.join(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Close Win32 event handle if on Windows
        if (shutdownEventHandle != null && Platform.isWindows()) {
            Kernel32.INSTANCE.CloseHandle(shutdownEventHandle);
            shutdownEventHandle = null;
        }

        logger.info("SvcWatchDogClient stopped");
    }

    /**
     * Waits for a named shutdown event to be signaled, or times out after the specified interval.
     * On Windows, this uses Win32 events. On other platforms, it simply sleeps.
     *
     * @param millisecondsTimeout The maximum time to wait, in milliseconds
     * @return True if the shutdown event was signaled; otherwise, false
     */
    public boolean waitForShutdownEvent(int millisecondsTimeout) {
        if (shutdownEvent == null || shutdownEvent.isEmpty() || !Platform.isWindows()) {
            try {
                Thread.sleep(millisecondsTimeout);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return false;
        }

        // On Windows, use Win32 events

        try {
            // Create or open a global event
            if (shutdownEventHandle == null) {
                shutdownEventHandle =
                        Kernel32.INSTANCE.CreateEvent(
                                null,
                                true, // Manual reset
                                false, // Initial state (not signaled)
                                shutdownEvent);

                if (shutdownEventHandle == null) {
                    logger.warning("Failed to create/open shutdown event: " + shutdownEvent);
                    Thread.sleep(millisecondsTimeout);
                    return false;
                }
            }

            int result =
                    Kernel32.INSTANCE.WaitForSingleObject(shutdownEventHandle, millisecondsTimeout);
            boolean shutdownRequested = (result == WinBase.WAIT_OBJECT_0);

            if (shutdownRequested) {
                logger.info("Shutdown requested via Win32 event");
            }

            return shutdownRequested;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error waiting for shutdown event", e);
            try {
                Thread.sleep(millisecondsTimeout);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    /**
     * True if at least one task has timed out and timeouts are not ignored; otherwise, false.
     *
     * @return true if timed out
     */
    public boolean isTimedOut() {
        return enabled && !timedOutTasks.isEmpty();
    }

    /**
     * True if UDP pinging is active at the moment.
     *
     * @return true if UDP pinging is active
     */
    public boolean isUdpPingingActive() {
        return tasks.containsKey(udpPingTaskName);
    }

    /**
     * List of all currently registered tasks. Mostly useful for testing purposes.
     *
     * @return list of task names
     */
    public List<String> getTaskList() {
        return new ArrayList<>(tasks.keySet());
    }

    /**
     * Registers or updates a task with a timeout. If the task already exists, its timeout is
     * refreshed.
     *
     * @param taskName The unique name of the task
     * @param timeoutSeconds The timeout duration in seconds
     */
    public void ping(String taskName, int timeoutSeconds) {
        logger.finest("ping: taskName=" + taskName + ", timeoutSeconds=" + timeoutSeconds);

        if (!enabled) {
            return;
        }

        long taskCheckTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        boolean doTrigger;

        synchronized (lock) {
            tasks.put(taskName, taskCheckTime);
            doTrigger = taskCheckTime < nextCheck.get();
        }

        // If needed, trigger the background thread to recheck the tasks and recalculate the next
        // check time
        if (doTrigger) {
            synchronized (trigger) {
                trigger.notifyAll();
            }
        }
    }

    /**
     * Removes a task from monitoring, closing its timeout.
     *
     * @param taskName The name of the task to remove
     */
    public void closeTimeout(String taskName) {
        logger.finest("closeTimeout: taskName=" + taskName);
        tasks.remove(taskName);
    }

    /**
     * The main background loop that checks for task timeouts and sends UDP pings at configured
     * intervals.
     */
    private void backgroundLoop() {
        logger.fine("Background loop starting");

        try {
            // Ignore timeouts for the initial half second
            long timeSkewRecoveryTime = 0;
            long expectedLoopTime = System.currentTimeMillis();

            while (!stopped.get()) {
                // Check all tasks
                long now = System.currentTimeMillis();
                boolean timeoutDetected = false;
                boolean udpPingNeeded = false;

                synchronized (lock) {
                    // Detect if time changes unexpectedly, most likely when computer wakes up from
                    // sleep mode or hibernation
                    if (timeSkewRecoveryTime < now) {
                        if ((expectedLoopTime + 5000) < now) {
                            logger.info(
                                    "Time skew detected, ignoring timeouts for the next "
                                            + timeSkewRecoveryInterval
                                            + " seconds");
                            timeSkewRecoveryTime = now + (timeSkewRecoveryInterval * 1000);
                        } else if (timeSkewRecoveryTime > 0) {
                            timeSkewRecoveryTime = 0;
                            logger.info(
                                    "TimeSkewRecoveryInterval is over, monitoring timeouts"
                                            + " normally");
                        }
                    }

                    nextCheck.set(Long.MAX_VALUE);

                    // Create a copy of the task names to allow modifying the collection while
                    // iterating
                    List<String> taskNames = new ArrayList<>(tasks.keySet());

                    for (String name : taskNames) {
                        if (timeoutDetected && name.equals(udpPingTaskName)) {
                            // Skip the internal ping task if a timeout has already been detected
                            continue;
                        }

                        Long timeout = tasks.get(name);
                        if (timeout == null) {
                            continue;
                        }

                        if (timeout <= now) {
                            if (name.equals(udpPingTaskName)) {
                                // This is the internal ping task; we need to send a ping unless a
                                // timeout has been detected
                                if (!timeoutDetected) {
                                    timeout = now + udpPingInterval;
                                    tasks.put(udpPingTaskName, timeout);
                                    udpPingNeeded = true;
                                }
                            } else if (now > timeSkewRecoveryTime && timedOutTasks.add(name)) {
                                // A new timed-out task has been detected
                                timeoutDetected = true;
                                tasks.remove(name);
                                // Prevent future UDP pings
                                tasks.remove(udpPingTaskName);
                            }
                        }

                        if (timeout > now && timeout < nextCheck.get()) {
                            // When the loop ends, nextCheck contains the nearest future timeout.
                            // This way we can determine optimal wait time.
                            nextCheck.set(timeout);
                        }
                    }
                }

                // Perform logging and UDP ping outside the critical section
                if (timeoutDetected) {
                    logger.severe("Timed out tasks: " + String.join(", ", timedOutTasks));
                } else if (udpPingNeeded) {
                    sendUdpPing();
                }

                // Wait for the next timeout or a trigger, with a 50 ms buffer to avoid premature
                // detection attempts
                // 60 seconds maximum is just a safety measure, as well as 100 ms minimum.
                int waitTime = (int) Math.min(Math.max(nextCheck.get() - now + 50, 100), 60000);
                expectedLoopTime = now + waitTime;

                synchronized (trigger) {
                    try {
                        trigger.wait(waitTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } catch (Exception ex) {
            // This should never happen, but if it does, we need to know about it
            logger.log(Level.SEVERE, "Exception/bug in background loop, PLEASE CHECK AND FIX", ex);
        }

        logger.fine("Background loop done");
    }

    /** Sends a UDP ping to the watchdog. */
    private void sendUdpPing() {
        if (udpAddress == null || udpPort <= 0 || watchdogSecret == null) {
            return;
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket packet =
                    new DatagramPacket(watchdogSecret, watchdogSecret.length, udpAddress, udpPort);
            socket.send(packet);
            logger.finest("UDP ping sent");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to send UDP ping", e);
        }
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * Represents a timeout detector that registers a task with the {@code SvcWatchDogClient} and
     * automatically closes it when disposed. This is useful for monitoring operations that should
     * complete within a specified timeout period.
     */
    public static class TimeoutDetector implements Closeable {

        private final String name;
        private boolean closed = false;
        private final SvcWatchDogClient client;

        /**
         * Initializes a new instance of the {@code TimeoutDetector} class and registers it with the
         * watchdog client.
         *
         * @param name The base name of the task to monitor
         * @param timeoutSeconds The timeout in seconds for the task
         * @param namePostfix If true, appends a unique UUID to the task name to ensure uniqueness
         * @param client The SvcWatchDogClient instance to use
         */
        public TimeoutDetector(
                String name, int timeoutSeconds, boolean namePostfix, SvcWatchDogClient client) {
            this.name = namePostfix ? name + "_" + UUID.randomUUID() : name;
            this.client = client;
            client.ping(this.name, timeoutSeconds);
        }

        /**
         * Initializes a new instance of the {@code TimeoutDetector} class with a unique name
         * postfix.
         *
         * @param name The base name of the task to monitor
         * @param timeoutSeconds The timeout in seconds for the task
         * @param client The SvcWatchDogClient instance to use
         */
        public TimeoutDetector(String name, int timeoutSeconds, SvcWatchDogClient client) {
            this(name, timeoutSeconds, true, client);
        }

        /**
         * Initializes a new instance of the {@code TimeoutDetector} class and registers it with the
         * watchdog client.
         *
         * @param name The base name of the task to monitor
         * @param timeoutSeconds The timeout in seconds for the task
         * @param namePostfix If true, appends a unique UUID to the task name to ensure uniqueness
         */
        public TimeoutDetector(String name, int timeoutSeconds, boolean namePostfix) {
            this(name, timeoutSeconds, namePostfix, SvcWatchDogClient.getInstance());
        }

        /**
         * Initializes a new instance of the {@code TimeoutDetector} class with a unique name
         * postfix.
         *
         * @param name The base name of the task to monitor
         * @param timeoutSeconds The timeout in seconds for the task
         */
        public TimeoutDetector(String name, int timeoutSeconds) {
            this(name, timeoutSeconds, true, SvcWatchDogClient.getInstance());
        }

        /**
         * Gets the name of the task within SvcWatchDogClient.
         *
         * @return the task name
         */
        public String getName() {
            return name;
        }

        /**
         * Gets information if the timeout has already been closed.
         *
         * @return true if closed
         */
        public boolean isClosed() {
            return closed;
        }

        /** Closes the timeout (removes the task from SvcWatchDogClient). */
        public void closeTimeout() {
            if (!closed) {
                client.closeTimeout(name);
                closed = true;
            }
        }

        @Override
        public void close() {
            closeTimeout();
        }
    }
}
