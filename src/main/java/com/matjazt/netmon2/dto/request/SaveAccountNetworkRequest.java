package com.matjazt.netmon2.dto.request;

/** Request DTO for creating or updating an AccountNetwork relationship. */
public record SaveAccountNetworkRequest(Long accountId, Long networkId) {}
