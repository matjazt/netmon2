package com.matjazt.netmon2.dto.response;

import java.time.LocalDateTime;

import com.matjazt.netmon2.entity.DeviceOperationMode;

/**
 * Lightweight DTO for exposing device data via API without touching lazy
 * proxies.
 */
public record DeviceResponseDto(
        Long id,
        Long networkId,
        String name,
        String macAddress,
        String ipAddress,
        Boolean online,
        LocalDateTime lastSeen,
        DeviceOperationMode deviceOperationMode) {
}
