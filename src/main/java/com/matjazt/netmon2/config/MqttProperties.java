package com.matjazt.netmon2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for MQTT broker connection.
 *
 * <p>Binds to properties prefixed with "mqtt" in application.yaml. Provides type-safe access to
 * MQTT configuration including broker URL, credentials, TLS settings, and connection behavior.
 *
 * <p>Example configuration:
 *
 * <pre>
 * mqtt:
 *   url: ssl://broker.example.com:8883
 *   client-id: netmon2
 *   username: your-username
 *   password: your-password
 *   topic-template: network/{networkName}/scan
 *   truststore-path: /path/to/truststore.jks
 *   truststore-password: changeit
 *   automatic-reconnect: true
 *   clean-session: false
 *   qos: 1
 *   connection-timeout: 30
 *   keep-alive-interval: 60
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "mqtt")
@Getter
@Setter
public class MqttProperties {

    private String url;
    private String clientId;
    private String username;
    private String password;
    private String topicTemplate;
    private String truststorePath;
    private String truststorePassword;
    private boolean automaticReconnect = true;
    private boolean cleanSession = false;
    private int qos = 1;
    private int connectionTimeout = 30;
    private int keepAliveInterval = 60;
    private int completionTimeout = 30000;
    private boolean sslVerifyHostname = true;

}
