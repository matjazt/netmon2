package com.matjazt.netmon2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for alert processing and email notifications.
 *
 * <p>Binds to properties prefixed with "alerter" in application.yaml. Provides type-safe access to
 * SMTP server configuration and alert check scheduling parameters.
 *
 * <p>Example configuration:
 *
 * <pre>
 * alerter:
 *   smtp-host: smtp.example.com
 *   smtp-port: 587
 *   smtp-username: your-email
 *   smtp-password: your-password
 *   smtp-start-tls: true
 *   smtp-auth: true
 *   from-email: alerts@example.com
 *   from-name: Network Monitor
 *   interval-seconds: 120
 *   initial-delay-seconds: 10
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "alerter")
public class AlerterProperties {

    private String smtpHost;
    private int smtpPort = 587;
    private String smtpUsername;
    private String smtpPassword;
    private boolean smtpStartTls = true;
    private boolean smtpAuth = true;
    private String fromEmail;
    private String fromName;

    private long intervalSeconds = 20;
    private long initialDelaySeconds = 30;

    // Getters and setters

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(int smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public void setSmtpUsername(String smtpUsername) {
        this.smtpUsername = smtpUsername;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public boolean isSmtpStartTls() {
        return smtpStartTls;
    }

    public void setSmtpStartTls(boolean smtpStartTls) {
        this.smtpStartTls = smtpStartTls;
    }

    public boolean isSmtpAuth() {
        return smtpAuth;
    }

    public void setSmtpAuth(boolean smtpAuth) {
        this.smtpAuth = smtpAuth;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public long getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(long intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public long getInitialDelaySeconds() {
        return initialDelaySeconds;
    }

    public void setInitialDelaySeconds(long initialDelaySeconds) {
        this.initialDelaySeconds = initialDelaySeconds;
    }
}
