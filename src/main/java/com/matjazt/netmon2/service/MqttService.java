package com.matjazt.netmon2.service;

import com.matjazt.netmon2.dto.NetworkStatusMessageDto;
import com.matjazt.netmon2.entity.AlertType;
import com.matjazt.netmon2.entity.DeviceEntity;
import com.matjazt.netmon2.entity.DeviceOperationMode;
import com.matjazt.netmon2.entity.DeviceStatusHistoryEntity;
import com.matjazt.netmon2.entity.NetworkConfiguration;
import com.matjazt.netmon2.entity.NetworkEntity;
import com.matjazt.netmon2.repository.DeviceRepository;
import com.matjazt.netmon2.repository.DeviceStatusHistoryRepository;
import com.matjazt.netmon2.repository.NetworkRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for processing MQTT messages containing network device scan results.
 *
 * <p>This service handles the core business logic for processing device status updates. The actual
 * MQTT message reception and performance timing is managed by {@link TimingProxy}, which serves as
 * the {@code @ServiceActivator} entry point and wraps calls to {@link #processMqttMessage(Message)}
 * to measure execution time outside the transactional boundary.
 *
 * <p>Message Flow:
 *
 * <ol>
 *   <li>MQTT broker publishes device scan results to topics
 *   <li>MqttPahoMessageDrivenChannelAdapter receives messages
 *   <li>Messages delivered to mqttInputChannel
 *   <li>{@link TimingProxy#processMqttMessage(Message)} receives and times the processing
 *   <li>This service processes messages and records device state changes in database
 * </ol>
 *
 * <p>Expected message format:
 *
 * <pre>
 * {
 *   "hostname": "Scanner",
 *   "timestamp": "2026-01-20T11:45:40+01:00",
 *   "devices": [
 *     {"ip": "192.168.1.1", "mac": "AA:BB:CC:DD:EE:FF"}
 *   ]
 * }
 * </pre>
 *
 * <p>Only state changes are stored - if a device was online and is still online, no record is
 * created. This minimizes database writes while preserving complete state history.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MqttService {

    private final DeviceRepository deviceRepository;
    private final NetworkRepository networkRepository;
    private final DeviceStatusHistoryRepository deviceStatusHistoryRepository;
    private final NetworkConfigurationService networkConfigurationService;
    private final AlerterService alerterService;
    private final MacVendorLookupService macVendorLookupService;

    /**
     * Handles incoming MQTT messages containing device scan results.
     *
     * <p>This method contains the core business logic for processing MQTT messages. It is called by
     * {@link TimingProxy#processMqttMessage(Message)}, which serves as the actual
     * {@code @ServiceActivator} and measures execution time outside the transactional boundary.
     *
     * <p>Processes device scan results: extracts network name from topic, parses JSON payload,
     * updates network last-seen timestamp, records device state changes (online/offline), and
     * triggers alerts for unauthorized devices.
     *
     * @param mqttMessage Spring Integration message containing MQTT payload and headers
     * @see TimingProxy#processMqttMessage(Message)
     */
    @Transactional
    public void processMqttMessage(Message<String> mqttMessage) {
        org.springframework.messaging.MessageHeaders headers = mqttMessage.getHeaders();
        String topic =
                headers.get(
                        org.springframework.integration.mqtt.support.MqttHeaders.RECEIVED_TOPIC,
                        String.class);
        Integer qos =
                headers.get(
                        org.springframework.integration.mqtt.support.MqttHeaders.RECEIVED_QOS,
                        Integer.class);
        Boolean retained =
                headers.get(
                        org.springframework.integration.mqtt.support.MqttHeaders.RECEIVED_RETAINED,
                        Boolean.class);
        Boolean duplicate =
                headers.get(
                        org.springframework.integration.mqtt.support.MqttHeaders.DUPLICATE,
                        Boolean.class);

        log.info(
                "Received MQTT message: payload='{}', topic='{}', qos={}, retained={},"
                        + " duplicate={}, headers={}",
                mqttMessage.getPayload(),
                topic,
                qos,
                retained,
                duplicate,
                headers);

        // if (topic != "rubbish") {
        //    return;
        // }

        try {

            // Extract network name from topic
            // For "network/MaliGrdi" -> "MaliGrdi"
            String networkName = extractNetworkName(topic);

            // Parse JSON payload to Java object
            NetworkStatusMessageDto message = parseMessage(mqttMessage.getPayload());
            var now = LocalDateTime.now(ZoneOffset.UTC);

            // obtain network information
            NetworkEntity network = getOrCreateNetwork(networkName);
            NetworkConfiguration networkConfig =
                    networkConfigurationService.getByNetworkId(network.getId());

            var messageTimestamp = LocalDateTime.ofInstant(message.getTimestamp(), ZoneOffset.UTC);

            if (messageTimestamp.isAfter(now.plusSeconds(networkConfig.getReportingInterval()))) {
                log.info(
                        "Message timestamp is too far in the future: {}, ignoring entire message"
                                + " for network {}",
                        messageTimestamp,
                        network);
                return;
            }

            if (messageTimestamp.isAfter(now)) {
                // adjust the timestamp silently, as this can happen if the device sending the
                // message has a slightly incorrect clock.
                messageTimestamp = now;
            }

            network.setLastSeen(messageTimestamp);
            // if the network was previously down (backOnlineTime is null) and now it's back up, set
            // backOnlineTime to current time. This will be used to determine when to close the
            // alert - we want to wait until the network has been back up for a certain threshold to
            // avoid closing the alert too quickly if the network is flapping.
            if (network.getBackOnlineTime() == null) {
                log.info("Network {} is back online, setting backOnlineTime to {}", network, now);
                network.setBackOnlineTime(now);
            }

            // Hibernate will auto-UPDATE at commit: networkRepository.save(network);

            /*
             * // Get list of currently online MACs from message
             * Set<String> currentlyOnlineMacs = new HashSet<>();
             * for (NetworkStatusMessage.DeviceInfo deviceStatus : message.getDevices()) {
             * currentlyOnlineMacs.add(deviceStatus.getMac());
             * }
             */

            // load all devices from the device repository for this network
            var knownDevices = deviceRepository.findByNetwork_Id(network.getId());

            // load all previously online devices for this network
            // var previouslyOnlineDevices =
            //        deviceStatusHistoryRepository.findCurrentlyOnlineDevices(network.getId());

            List<Long> processedDevices = new ArrayList<>();

            // Process each device in the message (all are online)
            for (var deviceStatus : message.getDevices()) {

                // Determine if we need to record a state change
                boolean shouldRecord = false;

                // possible scenarios:
                // 1. device is known and was online -> no change
                // 2. device is known and was offline -> record online, log change if alwaysOn
                // is true
                // 3. device is unknown -> record online, add to device repository, log new
                // device

                var mac = deviceStatus.getMac();
                if (mac == null || mac.isBlank()) {
                    log.warn(
                            "Device with missing or empty MAC address reported on network {}",
                            network);
                    continue; // skip devices with missing MAC
                }

                var ip = deviceStatus.getIp();

                // find the mac in the known devices list
                var knownDeviceOpt =
                        knownDevices.stream()
                                .filter(d -> d.getMacAddress().equals(mac))
                                .findFirst();

                DeviceEntity device = null;

                if (knownDeviceOpt.isEmpty()) {
                    // new device, add to repository
                    device = new DeviceEntity();
                    device.setNetwork(network);
                    device.setMacAddress(mac);
                    device.setIpAddress(ip);
                    device.setDeviceOperationMode(
                            DeviceOperationMode.UNAUTHORIZED); // default for new devices
                    device.setOnline(true); // currently online, obviously
                    device.setFirstSeen(messageTimestamp);
                    device.setLastSeen(messageTimestamp);
                    device.setVendor(macVendorLookupService.lookupVendor(mac));
                    // persist the new device before using it in the alert
                    deviceRepository.save(device);

                    alerterService.openAlert(
                            AlertType.DEVICE_UNAUTHORIZED,
                            network,
                            device,
                            "device detected for the first time");

                    // also add to device history
                    shouldRecord = true;
                } else {
                    // known device
                    device = knownDeviceOpt.get();
                    processedDevices.add(device.getId());

                    boolean wasOnline = device.getOnline();

                    // in all cases, update device's current online status and last seen
                    device.setOnline(true);
                    device.setLastSeen(messageTimestamp);
                    device.setIpAddress(ip);
                    if (device.getVendor() == null) {
                        device.setVendor(macVendorLookupService.lookupVendor(mac));
                    }

                    // see if alert needs to be sent for unauthorized device
                    if (device.getDeviceOperationMode() == DeviceOperationMode.UNAUTHORIZED
                            && device.getActiveAlertId() == null) {
                        // device is not allowed and no alert has been sent yet
                        alerterService.openAlert(
                                AlertType.DEVICE_UNAUTHORIZED,
                                network,
                                device,
                                "device was seen before");
                    } else {
                        // openAlert saves the device, so only save if no alert was opened
                        // Hibernate will auto-UPDATE at commit: deviceRepository.save(device);
                    }

                    // check last known status - search in previouslyOnlineDevices
                    // var deviceId = device.getId();
                    // var lastOnlineStatus =
                    //         previouslyOnlineDevices.stream()
                    //                 .filter(d -> d.getDevice().getId() == deviceId)
                    //                 .findFirst();

                    // if (lastOnlineStatus.isPresent()) {
                    if (wasOnline) {
                        // device was already online, no change, don't record
                        // NOTE: we don't want these logs in the database, so we use
                        // toString() explicitly and this way prevent detection as a "network log".
                        log.debug(
                                "Device {} is still online on network {}",
                                device.toString(),
                                network.toString());

                    } else {
                        // The device was offline, now online
                        shouldRecord = true;
                        if (device.getDeviceOperationMode() == DeviceOperationMode.UNAUTHORIZED) {
                            log.info(
                                    "Device {} is not allowed on network {} but is online!",
                                    device,
                                    network);

                        } else {
                            log.info("Device {} came online on network {}", device, network);
                        }
                    }
                }

                if (shouldRecord) {
                    DeviceStatusHistoryEntity status =
                            new DeviceStatusHistoryEntity(
                                    network, device, ip, true, messageTimestamp);
                    deviceStatusHistoryRepository.save(status);
                }
            }

            // now process known devices that were not in the current message
            for (var knownDevice : knownDevices) {
                if (processedDevices.contains(knownDevice.getId())) {
                    continue; // already processed
                }

                // if the device is online, according to our database, it went offline
                if (!knownDevice.getOnline()) {
                    continue; // already offline in the database, no change
                }

                knownDevice.setOnline(false);
                // Hibernate will auto-UPDATE at commit: deviceRepository.save(knownDevice);

                // check if the device was previously online
                // var lastOnlineStatus =
                //         previouslyOnlineDevices.stream()
                //                 .filter(d -> d.getDevice().getId() == knownDevice.getId())
                //                 .findFirst();

                // if (lastOnlineStatus.isPresent()) {
                // device went offline
                log.info("Device {} went offline on network {}", knownDevice, network);

                // Record offline status with last known IP
                var offlineStatus =
                        new DeviceStatusHistoryEntity(
                                network,
                                knownDevice,
                                knownDevice.getIpAddress(),
                                false,
                                messageTimestamp);
                deviceStatusHistoryRepository.save(offlineStatus);
                // }
            }

        } catch (Exception e) {
            log.error("Error processing MQTT message from topic: {}", topic, e);
        }
    }

    /**
     * Extract network name from MQTT topic. The topic is expected to be in format
     * "something/maybeSomethingElse/AndSoOn/NetworkName/operationName".
     */
    private String extractNetworkName(String topic) {

        int rightSlashIndex = topic.lastIndexOf('/');
        if (rightSlashIndex > 0) {
            int leftSlashIndex = topic.lastIndexOf('/', rightSlashIndex - 1);
            if (leftSlashIndex >= 0) {
                return topic.substring(leftSlashIndex + 1, rightSlashIndex);
            }
        }

        log.warn(
                "Topic does not follow expected format, using entire topic as network name: {}",
                topic);
        return topic;
    }

    /**
     * Parse JSON string to NetworkStatusMessage object.
     *
     * <p>JSON-B (Jakarta JSON Binding) is the standard JSON library in Jakarta EE. Similar to
     * System.Text.Json in .NET.
     */
    private NetworkStatusMessageDto parseMessage(String payload) {
        try {
            return new ObjectMapper().readValue(payload, NetworkStatusMessageDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON message", e);
        }
    }

    /** Get existing network or create a new one. */
    private NetworkEntity getOrCreateNetwork(String networkName) {
        return networkRepository
                .findByName(networkName)
                .orElseGet(
                        () -> {
                            NetworkEntity newNetwork = new NetworkEntity(networkName);
                            return networkRepository.save(newNetwork);
                        });
    }
}
