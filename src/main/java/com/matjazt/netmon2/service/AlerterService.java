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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;

/**
 * Background service for processing alerts and sending email notifications.
 *
 * <p>This service handles the core business logic for alert management. The actual scheduled
 * execution and performance timing is managed by {@link TimingProxy}, which wraps calls to {@link
 * #processNetworkAlerts(NetworkEntity)} to measure execution time outside the transactional
 * boundary.
 *
 * <p>Uses Spring's {@link JavaMailSender} for email delivery.
 *
 * @see TimingProxy#processAlerts()
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlerterService {

    private final AlerterProperties properties;
    private final JavaMailSender mailSender;

    private final NetworkRepository networkRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceStatusHistoryRepository deviceStatusHistoryRepository;
    private final AlertRepository alertRepository;
    private final NetworkConfigurationService networkConfigurationService;
    private final MacVendorLookupService macVendorLookupService;

    // private static final DateTimeFormatter TIME_FORMATTER =
    //        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Map<AlertType, String> ALERT_TYPE_MESSAGES =
            Map.ofEntries(
                    Map.entry(AlertType.NETWORK_DOWN, "Network is unavailable"),
                    Map.entry(AlertType.DEVICE_DOWN, "Device is offline"),
                    Map.entry(AlertType.DEVICE_UNAUTHORIZED, "Unauthorized device detected"));

    private void sendAlert(
            AlertEntity alert,
            boolean closure,
            boolean reminder,
            NetworkEntity network,
            DeviceEntity device,
            String message) {

        var now = LocalDateTime.now(ZoneOffset.UTC);

        String baseMessage = ALERT_TYPE_MESSAGES.get(alert.getAlertType());
        if (baseMessage == null) {
            throw new IllegalArgumentException("Unsupported alert type: " + alert.getAlertType());
        }

        var networkConfig = networkConfigurationService.getByNetworkId(network.getId());

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
        } else if (reminder) {
            fullMessageEntries.add("ALERT REMINDER");
            subject += " alert reminder";
        } else {
            fullMessageEntries.add("ALERT TRIGGERED");
            subject += " alert";
        }
        fullMessageEntries.add(""); // empty line

        fullMessageEntries.add("Network: " + network.getName());

        if (device != null) {
            fullMessageEntries.add("Device: " + device.getBasicInfo());
            subject += " for " + device.getNameOrMac();
        }
        fullMessageEntries.add(
                "Current time: "
                        + SimpleTools.formatDefaultWithTimeZone(now, networkConfig.getTimezone()));
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
        log.info("alert message for network {}:\n{}", network, fullMessage, device);

        // Send email if network has an email address configured
        var notificationEmailAddress = networkConfig.getNotificationEmailAddress();

        if (notificationEmailAddress != null
                && !notificationEmailAddress.isBlank()
                && !notificationEmailAddress.toLowerCase().endsWith("@example.com")) {
            try {
                sendEmail(notificationEmailAddress, subject, fullMessage);
                log.info(
                        "Alert email for network {} sent to {}",
                        network,
                        notificationEmailAddress,
                        device);
                alert.setLastNotificationTimestamp(now);
            } catch (Exception e) {
                log.error("Failed to send alert email", network, device, e);
                throw new RuntimeException(
                        "Failed to send alert email to " + notificationEmailAddress, e);
            }
        }
    }

    public AlertEntity openAlert(
            AlertType alertType, NetworkEntity network, DeviceEntity device, String message) {

        log.info(
                "opening alert: alertType={}, network={}, device={}, message={}",
                alertType,
                network.getName(),
                device != null ? device.getBasicInfo() : "N/A",
                message,
                network,
                device);

        // load latest alert for this network/device and check if it's closed
        var latestAlertOpt = alertRepository.findLatestAlert(network, device);
        if (latestAlertOpt.isPresent() && latestAlertOpt.get().getClosureTimestamp() == null) {
            throw new IllegalStateException(
                    "There's already an open alert for this network/device");
        }

        // store alert in database
        var alert =
                alertRepository.saveAndFlush(
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
            // Hibernate will auto-UPDATE at commit: networkRepository.save(network);
        } else {
            device.setActiveAlertId(alert.getId());
            // Hibernate will auto-UPDATE at commit: deviceRepository.save(device);
        }

        // send alert notification
        sendAlert(alert, false, false, network, device, message);

        // return created alert (including its ID)
        return alert;
    }

    public AlertEntity closeAlert(NetworkEntity network, DeviceEntity device, String message) {

        log.info(
                "closing alert: network={}, device={}, message={}",
                network.getName(),
                device != null ? device.getBasicInfo() : "N/A",
                message,
                network,
                device);

        // load latest alert for this network/device and check if it's closed
        var latestAlertOpt = alertRepository.findLatestAlert(network, device);
        if (!latestAlertOpt.isPresent() || latestAlertOpt.get().getClosureTimestamp() != null) {
            throw new IllegalStateException("There's no open alert for this network/device");
        }

        var alert = latestAlertOpt.get();

        // close alert in database
        alert.setClosureTimestamp(LocalDateTime.now(ZoneOffset.UTC));
        // Hibernate will auto-UPDATE at commit: alertRepository.save(alert);

        // close it also in the entity
        if (device == null) {
            network.setActiveAlertId(null);
            // Hibernate will auto-UPDATE at commit: networkRepository.save(network);
        } else {
            device.setActiveAlertId(null);
            // Hibernate will auto-UPDATE at commit: deviceRepository.save(device);
        }

        // append the information about the alert we are closing to the message: alert
        // timestamp and duration
        var duration =
                java.time.Duration.between(alert.getTimestamp(), alert.getClosureTimestamp());

        message =
                (message != null ? message.trim() : "")
                        + "\n"
                        + getInfoForExistingAlert(alert, duration);

        // send alert notification
        sendAlert(alert, true, false, network, device, message);

        // return closed alert
        return alert;
    }

    @Transactional
    public void processNetworkAlerts(long networkId) {

        SimpleTools.checkTransactionStatus(true);
        // see if the entire network is down or up

        var network =
                networkRepository
                        .findById(networkId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Network with ID " + networkId + " not found"));

        var now = LocalDateTime.now(ZoneOffset.UTC);
        var networkConfig = networkConfigurationService.getByNetworkId(networkId);
        var alertingThreshold = now.minusSeconds(networkConfig.getAlertingDelay());

        if (network.getLastSeen().isBefore(alertingThreshold)) {
            // network is down
            if (network.getActiveAlertId() == null) {
                // network is down, alert hasn't been sent yet
                openAlert(AlertType.NETWORK_DOWN, network, null, null);
            }
            // there's nothing else to do if the entire network is down
            return;
        }

        if (network.getBackOnlineTime() == null) {
            // network has been already detected as failed to report regularly
            // there's nothing more we should do here, because if the network didn't report lately,
            // we shouldn't check the devices
            return;
        }
        // let's use a threshold of 1.5 times the reporting interval to determine if the network
        // reported regularly
        var onlineThreshold = now.minusSeconds(networkConfig.getReportingInterval() * 3 / 2);
        if (network.getLastSeen().isBefore(onlineThreshold)) {
            // network failed to report regularly, so we should reset the backOnlineTime to reflect
            // that
            log.info(
                    "Network {} failed to report regularly (lastSeen: {}, onlineThreshold: {},"
                            + " backOnlineTime: {}), resetting backOnlineTime",
                    network,
                    network.getLastSeen(),
                    onlineThreshold,
                    network.getBackOnlineTime());
            network.setBackOnlineTime(null);
            // again, there is nothing more we should do here, because if the network didn't report
            // regularly, we shouldn't check the devices
            return;
        }

        // network is up, but it might have been down recently, so we should check if we need to
        // close the alert
        if (network.getActiveAlertId() != null) {
            if (network.getBackOnlineTime().isBefore(alertingThreshold)) {
                // network was down, now it's back up and has been up for long enough, so we can
                // close
                // the alert
                closeAlert(network, null, null);
            } else {
                // network does seem to be online, but the alert is still open and the network
                // hasn't been back online for long enough, so we won't check the devices yet,
                // because the results might be inaccurate while the network is still stabilizing
                // after coming back online
                return;
            }
        }

        // now check individual devices
        var closureThreshold =
                alertingThreshold.plusSeconds(Math.min(30, networkConfig.getAlertingDelay() / 10));

        for (DeviceEntity device : deviceRepository.findByNetwork_Id(network.getId())) {

            if (device.getVendor() == null) {
                device.setVendor(macVendorLookupService.lookupVendor(device.getMacAddress()));
            }
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

        var reminderTimeOfDay = networkConfig.getReminderTimeOfDay();
        var reminderIntervalDays = networkConfig.getReminderIntervalDays();

        // var nowTimeOfDay = LocalDateTime.now(ZoneOffset.UTC).toLocalTime();
        var localNow =
                SimpleTools.convertTimeZone(now, ZoneOffset.UTC, networkConfig.getTimezone());
        String hhmm = localNow.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (reminderTimeOfDay != null
                && reminderIntervalDays != null
                && hhmm.compareTo(reminderTimeOfDay) >= 0) {
            // it's time to send reminders for any open alerts that have been open long enough - for
            // example, if reminderIntervalDays is 1,
            // we will send reminders for alerts that have been open since yesterday (regardless of
            // the time). We basically count midnights, not days.
            // NOTE: all the timestamp math should be done in the network's local timezone, not UTC,
            // hence the conversions.
            //

            var reminderThreshold = localNow.toLocalDate().minusDays(reminderIntervalDays - 1);
            for (AlertEntity alert : alertRepository.findOpenAlertsByNetworkId(networkId)) {
                if (SimpleTools.convertTimeZone(
                                alert.getLastNotificationTimestamp(),
                                ZoneOffset.UTC,
                                networkConfig.getTimezone())
                        .toLocalDate()
                        .isBefore(reminderThreshold)) {
                    // send reminder email

                    // append information about the existing alert to the message: alert timestamp
                    // and duration

                    var duration = java.time.Duration.between(alert.getTimestamp(), now);

                    sendAlert(
                            alert,
                            false,
                            true,
                            network,
                            alert.getDevice(),
                            "\n" + getInfoForExistingAlert(alert, duration));
                }
            }
        }
    }

    private String getInfoForExistingAlert(AlertEntity alert, java.time.Duration duration) {

        return "Alert opened at: "
                + SimpleTools.formatDefaultWithTimeZone(
                        alert.getTimestamp(),
                        networkConfigurationService
                                .getByNetworkId(alert.getNetwork().getId())
                                .getTimezone())
                + "\nDuration: "
                + String.format(
                        "%d days, %d hours, %d minutes, %d seconds",
                        duration.toDaysPart(),
                        duration.toHoursPart(),
                        duration.toMinutesPart(),
                        duration.toSecondsPart());
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
