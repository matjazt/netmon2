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

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void processMqttMessage(Message<String> mqttMessage) {
        long startTime = System.currentTimeMillis();
        mqttService.processMqttMessage(mqttMessage);
        long endTime = System.currentTimeMillis();
        logger.info("Processed MQTT message in {} ms", (endTime - startTime));
    }

    @Scheduled(
            fixedRateString = "#{@alerterProperties.intervalSeconds * 1000}",
            initialDelayString = "#{@alerterProperties.initialDelaySeconds * 1000}",
            timeUnit = TimeUnit.MILLISECONDS)
    public void processAlerts() {

        // process networks one by one
        for (NetworkEntity network : networkRepository.findAll()) {
            // time each network separately
            long startTime = System.currentTimeMillis();
            alerterService.processNetworkAlerts(network);
            long endTime = System.currentTimeMillis();
            logger.info(
                    "Processed alerts for network '{}' in {} ms",
                    network.getName(),
                    (endTime - startTime));
        }
    }
}
