package com.matjazt.netmon2.dto;

/**
 * Generic DTO for account-network relationship data used in the service layer.
 *
 * <p>This is the domain model used between service and controller layers.
 */
public record AccountNetworkDto(Long id, Long accountId, Long networkId) {}
