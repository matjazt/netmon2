package com.matjazt.netmon2.dto.response;

import java.time.LocalDateTime;

/** Lightweight DTO for exposing account data via API without touching lazy proxies. */
public record AccountResponseDto(
        Long id,
        String username,
        String accountTypeName,
        String fullName,
        String email,
        LocalDateTime createdAt,
        LocalDateTime lastSeen) {}
