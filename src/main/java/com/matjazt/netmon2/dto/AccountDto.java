package com.matjazt.netmon2.dto;

import java.time.Instant;

/**
 * Generic DTO for account data used in the service layer.
 *
 * <p>This is the domain model used between service and controller layers.
 */
public record AccountDto(
        Long id,
        String username,
        int accountTypeId,
        String accountTypeName,
        String fullName,
        String email,
        Instant createdAt,
        Instant lastSeen) {}
