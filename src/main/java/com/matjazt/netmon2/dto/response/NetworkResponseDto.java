package com.matjazt.netmon2.dto.response;

import java.time.LocalDateTime;

/** Lightweight DTO for exposing network data via API without touching lazy proxies. */
public record NetworkResponseDto(
        Long id,
        String name,
        LocalDateTime firstSeen,
        LocalDateTime lastSeen,
        Long activeAlertId,
        String configuration,
        LocalDateTime backOnlineTime) {}
