package com.matjazt.netmon2.service;

import com.matjazt.netmon2.config.AlerterProperties;
import com.matjazt.netmon2.entity.AlertEntity;
import com.matjazt.netmon2.entity.AlertType;
import com.matjazt.netmon2.entity.DeviceEntity;
import com.matjazt.netmon2.entity.DeviceOperationMode;
import com.matjazt.netmon2.entity.NetworkEntity;
import com.matjazt.netmon2.repository.AlertRepository;
import com.matjazt.netmon2.repository.DeviceRepository;
import com.matjazt.netmon2.repository.DeviceStatusHistoryRepository;
import com.matjazt.netmon2.repository.NetworkRepository;
import com.matjazt.tools.SimpleTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
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

    private final DeviceRepository deviceRepository;
    private final NetworkRepository networkRepository;
    private final DeviceStatusHistoryRepository deviceStatusHistoryRepository;
    private final AlertRepository alertRepository;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlerterProperties properties;
    private final JavaMailSender mailSender;

    private static final Map<AlertType, String> ALERT_TYPE_MESSAGES =
            Map.ofEntries(
                    Map.entry(AlertType.NETWORK_DOWN, "Network is unavailable"),
                    Map.entry(AlertType.DEVICE_DOWN, "Device is offline"),
                    Map.entry(AlertType.DEVICE_UNAUTHORIZED, "Unauthorized device detected"));

    public AlerterService(
            AlerterProperties properties,
            JavaMailSender mailSender,
            DeviceRepository deviceRepository,
            NetworkRepository networkRepository,
            DeviceStatusHistoryRepository deviceStatusHistoryRepository,
            AlertRepository alertRepository) {
        this.properties = properties;
        this.mailSender = mailSender;
        this.deviceRepository = deviceRepository;
        this.networkRepository = networkRepository;
        this.deviceStatusHistoryRepository = deviceStatusHistoryRepository;
        this.alertRepository = alertRepository;
    }

    private void sendAlert(
            AlertEntity alert,
            boolean closure,
            NetworkEntity network,
            DeviceEntity device,
            String message) {

        String baseMessage = ALERT_TYPE_MESSAGES.get(alert.getAlertType());
        if (baseMessage == null) {
            throw new IllegalArgumentException("Unsupported alert type: " + alert.getAlertType());
        }

        var subject = "[" + network.getName() + "] ";

        var fullMessageEntries = new ArrayList<String>();

        if (alert.getAlertType() == AlertType.NETWORK_DOWN) {
            subject += "network";
        } else {
            subject += "device";
        }

        if (closure) {
            fullMessageEntries.add("ALERT CLOSED");
            subject += " alert closure";
        } else {
            fullMessageEntries.add("ALERT TRIGGERED");
            subject += " alert";
        }
        fullMessageEntries.add(""); // empty line

        if (network != null) {
            fullMessageEntries.add("Network: " + network.getName());
        }

        if (device != null) {
            fullMessageEntries.add("Device: " + device.getBasicInfo());
        }
        fullMessageEntries.add(
                "UTC time: " + SimpleTools.formatDefault(LocalDateTime.now(ZoneOffset.UTC)));
        fullMessageEntries.add("Alert Type: " + alert.getAlertType());
        fullMessageEntries.add("Alert Id: " + alert.getId());

        fullMessageEntries.add(""); // empty line

        if (!closure) {
            fullMessageEntries.add(baseMessage + ".");
        }

        if (message != null && !message.isBlank()) {
            fullMessageEntries.add(""); // empty line
            fullMessageEntries.add("Additional info: " + message);
            fullMessageEntries.add("Original description: " + baseMessage + ".");
        }

        var fullMessage = String.join(System.lineSeparator(), fullMessageEntries);
        logger.warn("fullMessage:\n{}", fullMessage);

        // Send email if network has an email address configured
        if (network != null
                && network.getEmailAddress() != null
                && !network.getEmailAddress().isEmpty()) {
            // figure out email subject

            if (device != null) {
                subject += " for " + device.getNameOrMac();
            }

            try {
                sendEmail(network.getEmailAddress(), subject, fullMessage);
                logger.info("Alert email sent to: {}", network.getEmailAddress());
            } catch (Exception e) {
                logger.error("Failed to send alert email", e);
                throw new RuntimeException(
                        "Failed to send alert email to " + network.getEmailAddress(), e);
            }
        }
    }

    public AlertEntity openAlert(
            AlertType alertType, NetworkEntity network, DeviceEntity device, String message) {

        logger.info(
                "alertType={}, network={}, device={}, message={}",
                alertType,
                network.getName(),
                device != null ? device.getBasicInfo() : "N/A",
                message);

        // load latest alert for this network/device and check if it's closed
        var latestAlertOpt = alertRepository.findLatestAlert(network, device);
        if (latestAlertOpt.isPresent() && latestAlertOpt.get().getClosureTimestamp() == null) {
            throw new IllegalStateException(
                    "There's already an open alert for this network/device");
        }

        // store alert in database
        var alert =
                alertRepository.save(
                        new AlertEntity(
                                LocalDateTime.now(ZoneOffset.UTC),
                                network,
                                device,
                                alertType,
                                message));

        // ensure INSERT is executed and ID is available
        // entityManager.flush();

        // store it also in the entity
        if (device == null) {
            network.setActiveAlertId(alert.getId());
            networkRepository.save(network);
        } else {
            device.setActiveAlertId(alert.getId());
            deviceRepository.save(device);
        }

        // send alert notification
        sendAlert(alert, false, network, device, message);

        // return created alert (including its ID)
        return alert;
    }

    public AlertEntity closeAlert(NetworkEntity network, DeviceEntity device, String message) {

        logger.info(
                "network={}, device={}, message={}",
                network.getName(),
                device != null ? device.getBasicInfo() : "N/A",
                message);

        // load latest alert for this network/device and check if it's closed
        var latestAlertOpt = alertRepository.findLatestAlert(network, device);
        if (!latestAlertOpt.isPresent() || latestAlertOpt.get().getClosureTimestamp() != null) {
            throw new IllegalStateException("There's no open alert for this network/device");
        }

        var alert = latestAlertOpt.get();

        // close alert in database
        alert.setClosureTimestamp(LocalDateTime.now(ZoneOffset.UTC));
        alertRepository.save(alert);

        // close it also in the entity
        if (device == null) {
            network.setActiveAlertId(null);
            networkRepository.save(network);
        } else {
            device.setActiveAlertId(null);
            deviceRepository.save(device);
        }

        // append the information about the alert we are closing to the message: alert
        // timestamp and duration
        var duration =
                java.time.Duration.between(alert.getTimestamp(), alert.getClosureTimestamp());
        String durationInfo =
                "Alert opened at: "
                        + SimpleTools.formatDefault(alert.getTimestamp())
                        + " UTC\nDuration: "
                        + String.format(
                                "%d days, %d hours, %d minutes, %d seconds",
                                duration.toDaysPart(),
                                duration.toHoursPart(),
                                duration.toMinutesPart(),
                                duration.toSecondsPart());
        message = (message != null ? message.trim() : "") + "\n" + durationInfo;

        // send alert notification
        sendAlert(alert, true, network, device, message);

        // return closed alert
        return alert;
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

        // process networks one by one
        for (NetworkEntity network : networkRepository.findAll()) {
            processNetworkAlerts(network);
        }
        /*
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
            */
    }

    @Transactional
    private void processNetworkAlerts(NetworkEntity network) {

        // see if the entire network is down or up

        var now = LocalDateTime.now(ZoneOffset.UTC);
        var alertingThreshold = now.minusSeconds(network.getAlertingDelay());
        var closureThreshold =
                alertingThreshold.plusSeconds(Math.min(30, network.getAlertingDelay() / 10));

        if (network.getLastSeen().isBefore(alertingThreshold)) {
            // network is down
            if (network.getActiveAlertId() == null) {
                // network is down, alert hasn't been sent yet
                openAlert(AlertType.NETWORK_DOWN, network, null, null);
            }
            // there's nothing else to do if the entire network is down
            return;
        }

        // network is up
        if (network.getActiveAlertId() != null) {
            // network was down, now it's back up - send recovery alert
            closeAlert(network, null, null);
        }

        // now check individual devices
        for (DeviceEntity device : deviceRepository.findByNetwork_Id(network.getId())) {

            if (device.getDeviceOperationMode() == DeviceOperationMode.UNAUTHORIZED) {
                // the device is not allowed on the network
                // alerts for such cases are sent when the device first appears, so here we can
                // just check if it's gone
                if (device.getActiveAlertId() != null
                        && device.getLastSeen().isBefore(alertingThreshold)) {
                    // device is gone, clear alert
                    closeAlert(network, device, null);
                }
            } else if (device.getDeviceOperationMode() == DeviceOperationMode.AUTHORIZED) {
                // the device is allowed, no alerts needed, but we can clear any active alerts
                // in case they were set before (e.g., if the device was previously
                // UNAUTHORIZED)
                if (device.getActiveAlertId() != null) {
                    closeAlert(network, device, "device is now authorized");
                }
            } else if (device.getDeviceOperationMode() == DeviceOperationMode.ALWAYS_ON) {
                // the device should always be online, check its status
                if (device.getLastSeen().isBefore(alertingThreshold)) {
                    // device is down, alert hasn't been sent yet
                    if (device.getActiveAlertId() == null) {
                        openAlert(AlertType.DEVICE_DOWN, network, device, null);
                    }
                } else {
                    // device is up
                    if (device.getActiveAlertId() != null
                            && deviceStatusHistoryRepository
                                    .findLatestHistoryEntryByDevice(network.getId(), device.getId())
                                    .getTimestamp()
                                    .isBefore(closureThreshold)) {
                        // device was down, now it's back up and has been up for long enough - send
                        // recovery alert
                        closeAlert(network, device, null);
                    }
                }
            }
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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(
                String.format("\"%s\" <%s>", properties.getFromName(), properties.getFromEmail()));
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }
}
