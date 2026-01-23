package com.matjazt.netmon2.service;

import com.matjazt.netmon2.config.AlerterProperties;
import com.matjazt.netmon2.entity.NetworkEntity;
import com.matjazt.netmon2.repository.NetworkRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Proxy service for measuring execution time of transactional operations.
 *
 * <p>This class wraps calls to other services to measure their execution time without affecting
 * transactional behavior. Timing code must be implemented in a separate class rather than within
 * the transactional methods themselves for the following reasons:
 *
 * <ol>
 *   <li><b>Transaction Boundary Integrity:</b> When a method is annotated with
 *       {@code @Transactional}, Spring creates an AOP proxy that manages transaction lifecycle
 *       (begin, commit, rollback). If timing code is inside the transactional method, the measured
 *       time includes transaction management overhead, not just business logic execution.
 *   <li><b>Accurate Performance Metrics:</b> By measuring time outside the transaction boundary, we
 *       capture only the actual business logic execution time, providing accurate performance
 *       metrics for monitoring and optimization.
 *   <li><b>AOP Proxy Behavior:</b> Spring's transaction management uses proxies. When a
 *       transactional method calls another method in the same class, the call bypasses the proxy
 *       and transactions may not work correctly. Separating timing into a different class ensures
 *       proper proxy behavior.
 *   <li><b>Separation of Concerns:</b> Timing is a cross-cutting concern that should be separate
 *       from core business logic, following clean architecture principles.
 * </ol>
 */
@Service
public class TimingProxy {

    private static final Logger logger = LoggerFactory.getLogger(TimingProxy.class);

    private final AlerterProperties alerterProperties;
    private final NetworkRepository networkRepository;
    private final AlerterService alerterService;
    private final MqttService mqttService;

    public TimingProxy(
            AlerterProperties alerterProperties,
            NetworkRepository networkRepository,
            AlerterService alerterService,
            MqttService mqttService) {
        this.alerterProperties = alerterProperties;
        this.networkRepository = networkRepository;
        this.alerterService = alerterService;
        this.mqttService = mqttService;
    }

    /**
     * Processes incoming MQTT messages with execution time measurement.
     *
     * <p>The {@code @ServiceActivator} annotation registers this method as the entry point for
     * messages arriving on the {@code mqttInputChannel}. This proxy method wraps the actual message
     * processing logic in {@link MqttService#processMqttMessage(Message)} to measure execution time
     * without interfering with transaction management.
     *
     * <p>The timing measurement happens outside the transactional boundary, ensuring accurate
     * performance metrics that reflect only the business logic execution time.
     *
     * @param mqttMessage Spring Integration message containing MQTT payload and headers
     * @see MqttService#processMqttMessage(Message)
     */
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void processMqttMessage(Message<String> mqttMessage) {
        long startTime = System.currentTimeMillis();
        mqttService.processMqttMessage(mqttMessage);
        long endTime = System.currentTimeMillis();
        logger.info("Processed MQTT message in {} ms", (endTime - startTime));
    }

    /**
     * Periodically processes alerts for all networks with execution time measurement.
     *
     * <p>This scheduled task runs at intervals defined by {@link AlerterProperties#intervalSeconds}
     * after an initial delay of {@link AlerterProperties#initialDelaySeconds}. It processes alerts
     * for each network sequentially, measuring execution time per network.
     *
     * <p>The timing is implemented at this proxy level rather than within {@link
     * AlerterService#processNetworkAlerts(NetworkEntity)} because:
     *
     * <ul>
     *   <li>The alert processing method is transactional - timing outside the transaction provides
     *       accurate business logic performance metrics
     *   <li>Each network is processed in its own transaction, and timing at this level captures the
     *       true per-network processing time
     *   <li>This approach maintains clean separation between timing concerns and core business
     *       logic with proper transaction boundaries
     * </ul>
     *
     * @see AlerterService#processNetworkAlerts(NetworkEntity)
     */
    @Scheduled(
            fixedRateString = "#{@alerterProperties.intervalSeconds * 1000}",
            initialDelayString = "#{@alerterProperties.initialDelaySeconds * 1000}",
            timeUnit = TimeUnit.MILLISECONDS)
    public void processAlerts() {

        // Process networks one by one, each in its own transaction
        for (NetworkEntity network : networkRepository.findAll()) {
            // Time each network separately to identify performance bottlenecks
            long startTime = System.currentTimeMillis();
            // we should not pass the entire network entity because it was loaded outside the
            // (future) transaction
            alerterService.processNetworkAlerts(network.getId());
            long endTime = System.currentTimeMillis();
            logger.info(
                    "Processed alerts for network '{}' in {} ms",
                    network.getName(),
                    (endTime - startTime));
        }
    }
}
