package com.matjazt.netmon2.dto.request;

import java.time.LocalDateTime;

/** Request DTO for creating or updating a Network. */
public record SaveNetworkRequest(
        String name,
        LocalDateTime firstSeen,
        LocalDateTime lastSeen,
        Long activeAlertId,
        String configuration,
        LocalDateTime backOnlineTime) {}
