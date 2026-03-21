package com.matjazt.netmon2.config;

import lombok.Data;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for buffered email logging.
 *
 * <p>Properties are bound from {@code application.yml} using Spring's relaxed binding
 */
@Data
@Component
@ConfigurationProperties(prefix = "email-logging")
public class EmailLoggingProperties {
    /** Email 'from' address for log emails. */
    private String emailFrom = "";

    /** Email 'to' address for log emails (can be a distribution list). */
    private String emailTo = "";

    /** Subject line prefix for log emails. */
    private String emailSubjectPrefix = "[App Logs]";

    /** Maximum number of buffered logs before sending email. */
    private int maxCount = 50;

    /** Maximum seconds to buffer logs before sending email. */
    private int maxDelaySeconds = 300;

    /** Minimum log level to buffer (ERROR, WARN, INFO, DEBUG, TRACE). */
    private String minLevel = "WARN";

    public @NonNull String[] getEmailToArray() {
        if (emailTo == null || emailTo.isEmpty()) {
            return new String[0];
        }
        return emailTo.split(",\\s*");
    }
}
