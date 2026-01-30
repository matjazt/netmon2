package com.matjazt.netmon2;

import com.matjazt.tools.SpringBootTools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application class for Network Monitor 2.
 *
 * <p>This application monitors network devices via MQTT, tracks online/offline state changes,
 * stores historical data in PostgreSQL, and sends email alerts for network/device issues.
 *
 * <p>Key Features:
 *
 * <ul>
 *   <li>MQTT integration with TLS support for receiving device scan results
 *   <li>Spring Data JPA repositories for database access
 *   <li>Scheduled alert processing and email notifications
 *   <li>REST API with Spring Security authentication
 *   <li>Multi-network support with per-network configuration
 *   <li>Device operation modes: unauthorized, authorized, always-on
 * </ul>
 *
 * <p>Required Annotations:
 *
 * <ul>
 *   <li>{@code @SpringBootApplication}: Auto-configuration, component scanning, and configuration
 *   <li>{@code @EnableIntegration}: Activates Spring Integration for MQTT message handling
 *   <li>{@code @EnableScheduling}: Enables {@code @Scheduled} annotation for background tasks
 * </ul>
 */
@SpringBootApplication
@EnableIntegration
@EnableScheduling
public class Netmon2Application {

    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(Netmon2Application.class);

        // Add dotenv listener to load .env before environment is prepared
        // Keep in mind that dotenv will not override existing environment variables, and also that
        // it will include them in its list. It is therefore correct to inject dotenv variables into
        // Spring Environment with the addFirst method.
        app.addListeners(
                (ApplicationListener<ApplicationEnvironmentPreparedEvent>)
                        event -> {
                            SpringBootTools.loadDotenvIntoSpringEnvironment(event.getEnvironment());
                        });

        app.run(args);
    }
}
