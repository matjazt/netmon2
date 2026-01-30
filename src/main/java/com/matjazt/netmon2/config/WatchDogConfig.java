package com.matjazt.netmon2.config;

import com.matjazt.tools.SvcWatchDogClient;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class WatchDogConfig {

    private static String MAIN_TASK_NAME = "WatchDogConfig";

    private static final Logger logger = LoggerFactory.getLogger(WatchDogConfig.class);

    private Thread monitoringThread;
    private volatile boolean running = false;

    @Autowired private HealthEndpoint healthEndpoint;

    @Autowired private ApplicationContext context;

    @Bean
    public SvcWatchDogClient watchdogClient() {
        // Get the singleton instance
        return SvcWatchDogClient.getInstance();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
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

            // we need to exit in a separate thread to avoid blocking the current scheduled task
            new Thread(
                            () -> {
                                int exitCode = SpringApplication.exit(context, () -> 1);
                                System.exit(exitCode);
                            })
                    .start();
        }

        // ping only if application is healthy
        if (isAppHealthy()) {
            wd.ping(MAIN_TASK_NAME, 20);
        }
    }

    private boolean isAppHealthy() {
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
