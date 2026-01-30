package com.matjazt.netmon2.config;

import com.matjazt.tools.SvcWatchDogClient;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
public class WatchDogConfig {

    private static String MAIN_TASK_NAME = "SpringBootApp";

    // private static final Logger logger = LoggerFactory.getLogger(WatchDogConfig.class);

    @Autowired private HealthEndpoint healthEndpoint;

    // @Autowired private ApplicationContext context;

    @Bean
    public SvcWatchDogClient watchdogClient() {
        // Get the singleton instance
        return SvcWatchDogClient.getInstance();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        SvcWatchDogClient.getInstance().ping(MAIN_TASK_NAME, 20);
        SvcWatchDogClient.getInstance().start();
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS)
    public void checkHealthAndPing() {
        var wd = SvcWatchDogClient.getInstance();

        /* NOTE: this code works as expected, but it is a bit too aggressive in terminating the
        application upon detecting a timeout, especially without knowing if there's anyone to restart it.
        Therefore, for now, we just ping the watchdog if the app is healthy. If the app becomes unhealthy,
        the watchdog will not be pinged and will eventually time out, resulting in not pinging the external
        SvcWatchDog service, which can then take appropriate action (like restarting the whole service).

        // first check if watchdog has detected a timeout (freeze of some kind), and if it has, exit
        // the app.
        if (wd.isTimedOut()) {
            // Watchdog has timed out, meaning that at least one registered task has exceeded its
            // timeout. The application is therefore considered unhealthy.
            logger.error("SvcWatchDogClient has detected a timed out task - exiting application");

            // we need to exit in a separate thread to avoid blocking the current scheduled task
            new Thread(
                            () -> {
                                int exitCode = SpringApplication.exit(context, () -> 1);
                                System.exit(exitCode);
                            })
                    .start();
        }
        */
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
        SvcWatchDogClient.getInstance().stop();
    }
}
