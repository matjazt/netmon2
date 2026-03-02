package com.matjazt.netmon2.dto;

import java.time.LocalDateTime;

/**
 * Generic DTO for device status history data used in the service layer.
 *
 * <p>This is the domain model used between service and controller layers.
 */
public record DeviceStatusHistoryDto(
        Long id,
        Long networkId,
        Long deviceId,
        String ipAddress,
        Boolean online,
        LocalDateTime timestamp) {}
