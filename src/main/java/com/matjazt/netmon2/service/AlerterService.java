package com.matjazt.netmon2.service;

import com.matjazt.netmon2.config.AlerterProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Background service for processing alerts and sending email notifications.
 *
 * <p>Uses @Scheduled for periodic execution (requires @EnableScheduling on application class). Uses
 * Spring's JavaMailSender for email delivery.
 */
@Service
public class AlerterService {

    private static final Logger logger = LoggerFactory.getLogger(AlerterService.class);
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlerterProperties properties;
    private final JavaMailSender mailSender;

    public AlerterService(AlerterProperties properties, JavaMailSender mailSender) {
        this.properties = properties;
        this.mailSender = mailSender;
    }

    /**
     * Scheduled method that runs at fixed rate based on configuration. @Scheduled uses SpEL (Spring
     * Expression Language) to read interval from properties. fixedRateString: runs every X seconds
     * (interval between start times) initialDelayString: waits X seconds before first execution
     *
     * <p>Alternative annotations: - @Scheduled(cron = "0 0 * * * *") - runs at the top of every
     * hour - @Scheduled(fixedDelay = 60000) - waits 60s between end of one execution and start of
     * next
     */
    @Scheduled(
            fixedRateString = "#{@alerterProperties.intervalSeconds * 1000}",
            initialDelayString = "#{@alerterProperties.initialDelaySeconds * 1000}",
            timeUnit = TimeUnit.MILLISECONDS)
    public void processAlerts() {
        logger.info("AlerterService: Running scheduled alert processing...");

        try {
            // TODO: Replace with actual alert logic
            // Example: query database for devices needing alerts, check conditions, etc.

            String currentTime = LocalDateTime.now().format(TIME_FORMATTER);
            String subject = "Test Alert - " + currentTime;
            String body =
                    String.format(
                            "This is a test alert sent at %s\n\n"
                                + "This framework can be used for:\n"
                                + "- Device offline notifications\n"
                                + "- Network status alerts\n"
                                + "- Periodic health reports\n"
                                + "- Any scheduled background processing\n\n"
                                + "Configure alerter.interval-seconds in application.yaml to change"
                                + " frequency.",
                            currentTime);

            sendEmail("mt.dev@gmx.com", subject, body);

            logger.info("AlerterService: Alert processing completed successfully");

        } catch (Exception e) {
            logger.error("AlerterService: Error during alert processing", e);
        }
    }

    /**
     * Send an email to a specific recipient (overrides configured default).
     *
     * @param to Recipient email address
     * @param subject Email subject
     * @param body Email body
     */
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(
                    String.format(
                            "\"%s\" <%s>", properties.getFromName(), properties.getFromEmail()));
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            logger.info("Email sent successfully to {}", to);

        } catch (Exception e) {
            logger.error("Failed to send email to {}", to, e);
            throw new RuntimeException("Email sending failed", e);
        }
    }
}
