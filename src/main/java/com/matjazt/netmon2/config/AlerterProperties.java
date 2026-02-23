package com.matjazt.netmon2.config;

import lombok.Getter;
import lombok.Setter;

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
@Getter
@Setter
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
}
