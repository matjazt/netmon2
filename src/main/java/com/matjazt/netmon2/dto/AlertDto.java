package com.matjazt.netmon2.dto;

import com.matjazt.netmon2.entity.AlertType;

import java.time.LocalDateTime;

/** DTO for transferring Alert data between service and controller layers. */
public record AlertDto(
        Long id,
        Long networkId,
        Long deviceId,
        AlertType alertType,
        String message,
        LocalDateTime timestamp,
        LocalDateTime closureTimestamp) {}
