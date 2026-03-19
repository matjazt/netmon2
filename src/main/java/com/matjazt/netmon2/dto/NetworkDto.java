package com.matjazt.netmon2.dto;

import java.time.Instant;

/**
 * Generic DTO for network data used in the service layer.
 *
 * <p>This is the domain model used between service and controller layers.
 */
public record NetworkDto(
        Long id,
        String name,
        Instant firstSeen,
        Instant lastSeen,
        Long activeAlertId,
        String configuration,
        Instant backOnlineTime) {}
