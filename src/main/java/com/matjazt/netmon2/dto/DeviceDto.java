package com.matjazt.netmon2.dto;

import com.matjazt.netmon2.entity.DeviceOperationMode;

import java.time.LocalDateTime;

/**
 * Generic DTO for device data used in the service layer.
 *
 * <p>This is the domain model used between service and controller layers.
 */
public record DeviceDto(
        Long id,
        Long networkId,
        String name,
        String macAddress,
        String ipAddress,
        Boolean online,
        LocalDateTime lastSeen,
        DeviceOperationMode deviceOperationMode,
        Long activeAlertId) {}
