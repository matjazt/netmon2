package com.matjazt.netmon2.service;

import com.matjazt.netmon2.entity.NetworkConfiguration;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

/**
 * Mapper for converting between NetworkConfiguration objects and JSON strings.
 *
 * <p>Handles deserialization with default values for missing fields and ignores unknown JSON
 * properties for forward compatibility.
 */
@Component
@RequiredArgsConstructor
public class NetworkConfigurationMapper {

    private final ObjectMapper objectMapper;

    /**
     * Deserialize JSON to NetworkConfiguration with defaults.
     *
     * @param json JSON string, may be null or blank
     * @return NetworkConfiguration with defaults applied for missing fields
     */
    public NetworkConfiguration fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new NetworkConfiguration();
        }
        try {
            return objectMapper.readValue(json, NetworkConfiguration.class);
        } catch (Exception ex) {
            // Safe fallback on malformed JSON
            // TODO: consider logging the error for debugging purposes, or don't catch and let it propagate if we want to fail fast on invalid data
            return new NetworkConfiguration();
        }
    }

    /**
     * Serialize NetworkConfiguration to JSON.
     *
     * @param config NetworkConfiguration object
     * @return JSON string
     * @throws IllegalArgumentException if serialization fails
     */
    public String toJson(NetworkConfiguration config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid network configuration", ex);
        }
    }
}
