package com.matjazt.netmon2.service;

import com.matjazt.netmon2.entity.NetworkConfiguration;
import com.matjazt.netmon2.entity.NetworkEntity;
import com.matjazt.netmon2.repository.NetworkRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

/**
 * Service for managing network configurations with caching.
 *
 * <p>Provides read-only access to network configurations with automatic caching. Configurations are
 * immutable and cached by network ID for fast repeated access.
 */
@Service
@RequiredArgsConstructor
public class NetworkConfigurationService {

    private final NetworkRepository networkRepository;
    private final ObjectMapper objectMapper;

    /**
     * Validate a raw configuration JSON string without touching the database.
     *
     * <p>Parses the JSON and calls {@link NetworkConfiguration#IsValid()}. Throws {@link
     * IllegalArgumentException} with a descriptive message on any failure.
     *
     * @param json the JSON string to validate
     * @throws IllegalArgumentException if the JSON is blank, unparseable, or fails IsValid()
     */
    public void validateConfigurationJson(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Network configuration cannot be null or blank");
        }
        try {
            NetworkConfiguration cfg = objectMapper.readValue(json, NetworkConfiguration.class);
            if (!cfg.IsValid()) {
                throw new IllegalArgumentException("Invalid network configuration: " + json);
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Unable to parse network configuration: " + json, ex);
        }
    }

    /**
     * Get network configuration by network ID (cached).
     *
     * <p>Returns a NetworkConfiguration object. Results are cached for 10 minutes.
     *
     * @param networkId the network ID
     * @return immutable NetworkConfiguration with defaults applied
     * @throws IllegalArgumentException if network not found
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "networkConfigCache", key = "#networkId", sync = true)
    public NetworkConfiguration getByNetworkId(Long networkId) {
        NetworkEntity entity =
                networkRepository
                        .findById(networkId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Network not found: " + networkId));

        String json = entity.getConfiguration();
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException(
                    "Network configuration JSON cannot be null or blank");
        }

        try {
            NetworkConfiguration cfg = objectMapper.readValue(json, NetworkConfiguration.class);
            if (cfg.IsValid()) {
                return cfg;
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Unable to parse network configuration: " + json, ex);
        }

        throw new IllegalArgumentException("Invalid network configuration: " + json);
    }

    /**
     * Update network configuration and evict cache.
     *
     * @param networkId the network ID
     * @param config the new configuration
     * @throws IllegalArgumentException if network not found
     */
    @Transactional
    @CacheEvict(cacheNames = "networkConfigCache", key = "#networkId")
    public void update(Long networkId, NetworkConfiguration config) {
        NetworkEntity entity =
                networkRepository
                        .findById(networkId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Network not found: " + networkId));
        entity.setConfiguration(objectMapper.writeValueAsString(config));
        // not needed due to the @Transactional annotation: networkRepository.save(entity);
    }
}
