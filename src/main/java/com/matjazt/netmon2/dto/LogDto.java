package com.matjazt.netmon2.dto;

import java.time.LocalDateTime;

/**
 * Generic DTO for log data used in the service layer.
 *
 * <p>This is the domain model used between service and controller layers.
 */
public record LogDto(
        Long id,
        LocalDateTime timestamp,
        Integer level,
        String origin,
        String message,
        Long networkId,
        Long deviceId,
        String networkName,
        String deviceNameOrVendor) {}
