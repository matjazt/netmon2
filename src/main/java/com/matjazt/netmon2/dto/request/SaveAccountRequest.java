package com.matjazt.netmon2.dto.request;

import java.time.LocalDateTime;

/** Request DTO for creating or updating an Account. */
public record SaveAccountRequest(
        String username,
        Integer accountTypeId,
        String password,
        String fullName,
        String email,
        LocalDateTime createdAt,
        LocalDateTime lastSeen) {}
