package com.matjazt.netmon2.service;

import com.matjazt.netmon2.entity.NetworkConfiguration;
import com.matjazt.netmon2.entity.NetworkEntity;
import com.matjazt.netmon2.repository.NetworkRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final NetworkConfigurationMapper mapper;

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
    @Cacheable(cacheManager = "networkConfigurationCacheManager", cacheNames = "networkConfigById", key = "#networkId")
    public NetworkConfiguration getByNetworkId(Long networkId) {
        NetworkEntity entity =
                networkRepository
                        .findById(networkId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Network not found: " + networkId));
        return mapper.fromJson(entity.getConfiguration());
    }

    /**
     * Update network configuration and evict cache.
     *
     * @param networkId the network ID
     * @param config the new configuration
     * @throws IllegalArgumentException if network not found
     */
    @Transactional
    @CacheEvict(cacheManager = "networkConfigurationCacheManager", cacheNames = "networkConfigById", key = "#networkId")
    public void update(Long networkId, NetworkConfiguration config) {
        NetworkEntity entity =
                networkRepository
                        .findById(networkId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Network not found: " + networkId));
        entity.setConfiguration(mapper.toJson(config));
        // not needed due to the @Transactional annotation: networkRepository.save(entity);
    }
}
