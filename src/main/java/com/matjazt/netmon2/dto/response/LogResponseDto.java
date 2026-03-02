package com.matjazt.netmon2.dto.response;

import java.time.LocalDateTime;

/** Lightweight DTO for exposing log data via API without touching lazy proxies. */
public record LogResponseDto(
        Long id,
        LocalDateTime timestamp,
        Integer level,
        String origin,
        String message,
        Long networkId,
        Long deviceId) {}
