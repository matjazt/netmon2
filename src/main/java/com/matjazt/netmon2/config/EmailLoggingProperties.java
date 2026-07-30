package com.matjazt.netmon2.config;

import lombok.Data;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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

    /** Application name for log emails. */
    @Value("${spring.application.name}")
    private String applicationName = "my-application";

    /** Maximum number of buffered logs before sending email. */
    private int maxCount = 50;

    /** Maximum size of buffered logs before sending email. */
    private int maxSize = 100000;

    /** Maximum seconds to buffer logs before sending email. */
    private int maxDelaySeconds = 300;

    /** Minimum log level to buffer (ERROR, WARN, INFO, DEBUG, TRACE). */
    private String minLevel = "WARN";

    /** Regular expressions used to exclude matching log entries from email delivery. */
    private List<String> excludePatterns = new ArrayList<>();

    /**
     * Maximum number of emails that can be sent within {@code emailRatePeriodSeconds}. 0 =
     * unlimited.
     */
    private int maxEmailsPerPeriod = 10;

    /** Length of the rate-limiting window in seconds. */
    private int emailRatePeriodSeconds = 3600;

    public @NonNull String[] getEmailToArray() {
        if (emailTo == null || emailTo.isEmpty()) {
            return new String[0];
        }
        return emailTo.split(",\\s*");
    }
}
