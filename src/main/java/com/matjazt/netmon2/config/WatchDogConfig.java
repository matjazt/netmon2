package com.matjazt.netmon2.config;

import com.matjazt.tools.SvcWatchDogClient;

import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
@Slf4j
public class WatchDogConfig {

    private static String MAIN_TASK_NAME = "WatchDogConfig";

    private Thread monitoringThread;
    private volatile boolean running = false;

    @Autowired(required = false)
    private HealthEndpoint healthEndpoint;

    @Autowired private ApplicationContext context;

    @Bean
    public SvcWatchDogClient watchdogClient() {
        // Get the singleton instance
        return SvcWatchDogClient.getInstance();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (healthEndpoint == null) {
            logger.warn(
                    "HealthEndpoint bean not found — WatchDog will assume the application is"
                            + " healthy unless a timeout or freeze is detected.");
        }
        SvcWatchDogClient.getInstance().ping(MAIN_TASK_NAME, 20);
        SvcWatchDogClient.getInstance().start();
        startMonitoringThread();
    }

    private void startMonitoringThread() {
        running = true;
        monitoringThread =
                new Thread(
                        () -> {
                            logger.info("Monitoring thread started");
                            while (running) {
                                checkHealthAndPing();
                            }
                            logger.info("Monitoring thread stopped");
                        },
                        "HealthMonitorThread");
        monitoringThread.start();
    }

    private void checkHealthAndPing() {
        var wd = SvcWatchDogClient.getInstance();

        // First check if watchdog has detected a shutdown request.
        // This is a slow polling operation, so we use a timeout of 5 seconds
        if (wd.waitForShutdownEvent(5000)) {
            logger.error(
                    "SvcWatchDogClient has detected an external shutdown request - exiting"
                            + " application");
            running = false;
            startShutdownProcedure();
            return;
        }

        // ping only if application is healthy
        if (isAppHealthy()) {
            wd.ping(MAIN_TASK_NAME, 20);
        }

        // check watchdog status, and exit if watchdog has marked the application as unhealthy
        // AND external watchdog is detected
        if (wd.isTimedOut() && wd.isExternalWatchdogDetected()) {
            logger.error(
                    "SvcWatchDogClient has marked the application as unhealthy - exiting"
                            + " application, since it will be restarted by the external watchdog");
            running = false;
            startShutdownProcedure();
        }
    }

    private void startShutdownProcedure() {
        // we need to exit in a separate thread to avoid blocking the current task
        new Thread(
                        () -> {
                            int exitCode = SpringApplication.exit(context, () -> 1);
                            System.exit(exitCode);
                        })
                .start();
    }

    private boolean isAppHealthy() {
        if (healthEndpoint == null) {
            // HealthEndpoint not available - assuming application is healthy
            return true;
        }

        var descriptor = healthEndpoint.healthForPath("liveness");
        return descriptor != null && descriptor.getStatus().getCode().equalsIgnoreCase("UP");
    }

    @PreDestroy
    public void onShutdown() {
        running = false;
        if (monitoringThread != null) {
            monitoringThread.interrupt();
            try {
                monitoringThread.join(2000); // Wait up to 2 seconds for thread to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for monitoring thread to stop");
            }
        }
        SvcWatchDogClient.getInstance().stop();
    }
}
