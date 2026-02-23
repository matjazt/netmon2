package com.matjazt.netmon2.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Data Transfer Object (DTO) representing the JSON structure of MQTT messages.
 *
 * <p>DTOs are simple data containers used for transferring data between layers. This matches the
 * MQTT payload structure exactly.
 *
 * <p>Jakarta JSON-B (JSON Binding) automatically maps JSON to these objects, similar to
 * System.Text.Json in .NET.
 */
@Getter
@Setter
@NoArgsConstructor
public class NetworkStatusMessageDto {

    /** Network hostname from MQTT message. */
    private String hostname;

    /** Timestamp from the message (format: "2025-12-03 10:18:56"). */
    private Instant timestamp;

    /** List of currently online devices. */
    private List<DeviceInfo> devices;

    /** Nested class for device information in the JSON. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class DeviceInfo {
        private String ip;
        private String mac;
    }
}
