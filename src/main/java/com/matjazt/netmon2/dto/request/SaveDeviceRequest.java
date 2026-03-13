package com.matjazt.netmon2.dto.request;

import com.matjazt.netmon2.entity.DeviceOperationMode;

import java.time.LocalDateTime;

/** Request DTO for creating or updating a Device. */
public record SaveDeviceRequest(
        Long networkId,
        String name,
        String macAddress,
        String ipAddress,
        Boolean online,
        LocalDateTime firstSeen,
        LocalDateTime lastSeen,
        Long activeAlertId,
        String vendor,
        DeviceOperationMode deviceOperationMode) {}
