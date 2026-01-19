package com.matjazt.netmon2.service;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
public class MqttService {

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handle(Message<String> message) {
        System.out.println("Received MQTT message: " + message.getPayload());
    }
}
