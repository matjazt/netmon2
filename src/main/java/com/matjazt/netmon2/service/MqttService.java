package com.matjazt.netmon2.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import com.matjazt.netmon2.dto.NetworkStatusMessageDto;
import com.matjazt.netmon2.entity.AlertType;
import com.matjazt.netmon2.entity.DeviceEntity;
import com.matjazt.netmon2.entity.DeviceOperationMode;
import com.matjazt.netmon2.entity.DeviceStatusHistoryEntity;
import com.matjazt.netmon2.entity.NetworkEntity;
import com.matjazt.netmon2.repository.AlertRepository;
import com.matjazt.netmon2.repository.DeviceRepository;
import com.matjazt.netmon2.repository.DeviceStatusHistoryRepository;
import com.matjazt.netmon2.repository.NetworkRepository;
import tools.jackson.databind.ObjectMapper;

@Service
public class MqttService {

    private static final Logger logger = LoggerFactory.getLogger(MqttService.class);

    private final DeviceRepository deviceRepository;
    private final NetworkRepository networkRepository;
    private final DeviceStatusHistoryRepository deviceStatusHistoryRepository;

    private final AlerterService alerterService;

    public MqttService(
            DeviceRepository deviceRepository,
            NetworkRepository networkRepository,
            DeviceStatusHistoryRepository deviceStatusHistoryRepository,
            AlertRepository alertRepository,
            AlerterService alerterService) {
        this.deviceRepository = deviceRepository;
        this.networkRepository = networkRepository;
        this.deviceStatusHistoryRepository = deviceStatusHistoryRepository;
        this.alerterService = alerterService;
        logger.info("initialized");
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handle(Message<String> messagex) {
        org.springframework.messaging.MessageHeaders headers = messagex.getHeaders();
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

        logger.info(
                "Received MQTT message: payload='{}', topic='{}', qos={}, retained={},"
                        + " duplicate={}, headers={}",
                messagex.getPayload(),
                topic,
                qos,
                retained,
                duplicate,
                headers);

        // if (topic != "testiram") {
        //    return;
        // }

        try {

            // Extract network name from topic
            // For "network/MaliGrdi" -> "MaliGrdi"
            String networkName = extractNetworkName(topic);

            // Parse JSON payload to Java object
            NetworkStatusMessageDto message = parseMessage(messagex.getPayload());

            var messageTimestamp = LocalDateTime.ofInstant(message.getTimestamp(), ZoneOffset.UTC);

            // Get or create network record
            NetworkEntity network = getOrCreateNetwork(networkName);
            network.setLastSeen(messageTimestamp);
            networkRepository.save(network);

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
            var previouslyOnlineDevices =
                    deviceStatusHistoryRepository.findCurrentlyOnlineDevices(network.getId());

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
                    logger.warn(
                            "Device with missing or empty MAC address reported on network: "
                                    + network.getName());
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

                    // in all cases, update device's current online status and last seen
                    device.setOnline(true);
                    device.setLastSeen(messageTimestamp);
                    device.setIpAddress(ip);

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
                        deviceRepository.save(device);
                    }

                    // check last known status - search in previouslyOnlineDevices
                    var deviceId = device.getId();
                    var lastOnlineStatus =
                            previouslyOnlineDevices.stream()
                                    .filter(d -> d.getDevice().getId() == deviceId)
                                    .findFirst();

                    if (lastOnlineStatus.isPresent()) {
                        // device was already online, no change, don't record
                        logger.info(
                                "Device is still online: "
                                            + device.getBasicInfo()
                                            + " on "
                                        + network.getName());

                    } else {
                        // The device was offline, now online
                        shouldRecord = true;
                        if (device.getDeviceOperationMode() == DeviceOperationMode.UNAUTHORIZED) {
                            logger.info(
                                    "Device "
                                            + device.getBasicInfo()
                                            + " is not allowed on network "
                                            + network.getName()
                                            + " but is online!");
                        } else {
                            logger.info(
                                    String.format(
                                            "Device came online: "
                                                    + device.getBasicInfo()
                                                    + " on "
                                                    + network.getName()));
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

                var mac = knownDevice.getMacAddress();
                var ip = knownDevice.getIpAddress();

                // in all cases, update device's current online status and last seen
                knownDevice.setOnline(false);
                deviceRepository.save(knownDevice);

                // check if the device was previously online
                var lastOnlineStatus =
                        previouslyOnlineDevices.stream()
                                .filter(d -> d.getDevice().getId() == knownDevice.getId())
                                .findFirst();

                if (lastOnlineStatus.isPresent()) {
                    // device went offline
                    logger.info(
                            "Device went offline: "
                                    + knownDevice.getBasicInfo()
                                    + " on "
                                    + network.getName());

                    // Record offline status with last known IP
                    var offlineStatus =
                            new DeviceStatusHistoryEntity(
                                    network, knownDevice, ip, false, messageTimestamp);
                    deviceStatusHistoryRepository.save(offlineStatus);
                }
            }

        } catch (Exception e) {
            logger.error("Error processing MQTT message from topic: {}", topic, e);
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

        logger.warn(
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
