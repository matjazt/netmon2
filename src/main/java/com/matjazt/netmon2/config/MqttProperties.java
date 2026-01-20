package com.matjazt.netmon2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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

    // getters and setters
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTopicTemplate() {
        return topicTemplate;
    }

    public void setTopicTemplate(String topicTemplate) {
        this.topicTemplate = topicTemplate;
    }

    public String getTruststorePath() {
        return truststorePath;
    }

    public void setTruststorePath(String truststorePath) {
        this.truststorePath = truststorePath;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }

    public boolean isAutomaticReconnect() {
        return automaticReconnect;
    }

    public void setAutomaticReconnect(boolean automaticReconnect) {
        this.automaticReconnect = automaticReconnect;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public void setKeepAliveInterval(int keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    public int getCompletionTimeout() {
        return completionTimeout;
    }

    public void setCompletionTimeout(int completionTimeout) {
        this.completionTimeout = completionTimeout;
    }

    public boolean isSslVerifyHostname() {
        return sslVerifyHostname;
    }

    public void setSslVerifyHostname(boolean sslVerifyHostname) {
        this.sslVerifyHostname = sslVerifyHostname;
    }
}
