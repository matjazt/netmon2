package com.matjazt.netmon2.dto.response;

/** Lightweight DTO for exposing account-network relationship via API. */
public record AccountNetworkResponseDto(Long id, Long accountId, Long networkId) {}
