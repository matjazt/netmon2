package com.matjazt.netmon2.service;

import com.matjazt.netmon2.config.EmailLoggingProperties;
import com.matjazt.tools.SimpleTools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Sends a daily heartbeat email to the addresses configured in the {@code email-logging} section.
 *
 * <p>The schedule is controlled by {@code email-logging.daily-email-time}, which accepts a standard
 * 6-field Spring cron expression (e.g. {@code "0 0 8 * * *"} for 08:00 every day). Setting the
 * property to {@code "-"} or leaving it blank disables the feature.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DailyHeartbeatEmailService {

    private final EmailLoggingProperties emailLoggingProperties;

    /**
     * ObjectProvider is used to avoid a hard startup failure when the mail sender is not
     * configured.
     */
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Scheduled(cron = "${email-logging.daily-email-time:-}")
    public void sendDailyHeartbeat() {
        var cfg = emailLoggingProperties;

        String[] recipients = cfg.getEmailToArray();
        if (recipients.length == 0) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Daily heartbeat email skipped: JavaMailSender not available");
            return;
        }

        String hostname = SimpleTools.getLocalHostname();

        String subject = cfg.getEmailSubjectPrefix() + " Daily Heartbeat - " + hostname;

        String body =
                String.join(
                        System.lineSeparator(),
                        "This is an automated daily heartbeat message from netmon2.",
                        "It confirms that the application is running and the email delivery system"
                                + " is operational.",
                        "",
                        "Timestamp : " + SimpleTools.formatDefault(LocalDateTime.now()),
                        "Host      : " + hostname,
                        "",
                        "No action is required. If you did NOT receive this message at the expected"
                                + " time,",
                        "please check the application status.");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(cfg.getEmailFrom());
            message.setTo(recipients);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Daily heartbeat email sent to {} recipient(s)", recipients.length);
        } catch (Exception e) {
            log.error("Failed to send daily heartbeat email: {}", e.getMessage(), e);
        }
    }
}
