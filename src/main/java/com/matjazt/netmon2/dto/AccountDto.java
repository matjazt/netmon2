package com.matjazt.netmon2.dto;

import java.time.LocalDateTime;

/**
 * Generic DTO for account data used in the service layer.
 *
 * <p>This is the domain model used between service and controller layers.
 */
public record AccountDto(
        Long id,
        String username,
        String accountTypeName,
        String fullName,
        String email,
        LocalDateTime createdAt,
        LocalDateTime lastSeen) {}
