package com.matjazt.netmon2.dto.response;

import java.time.LocalDateTime;

/** Lightweight DTO for exposing device status history data via API. */
public record DeviceStatusHistoryResponseDto(
        Long id,
        Long networkId,
        Long deviceId,
        String ipAddress,
        Boolean online,
        LocalDateTime timestamp) {}
