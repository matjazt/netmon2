package com.matjazt.netmon2.service;

import com.matjazt.netmon2.dto.NetworkDto;
import com.matjazt.netmon2.dto.request.SaveNetworkRequest;
import com.matjazt.netmon2.entity.NetworkEntity;
import com.matjazt.netmon2.mapper.NetworkMapper;
import com.matjazt.netmon2.repository.NetworkRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for managing Network entities.
 *
 * <p>Provides CRUD operations and business logic for monitored networks.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NetworkService {

    private final NetworkRepository networkRepository;
    private final NetworkMapper networkMapper;

    // ========== BASIC CRUD OPERATIONS ==========

    /** Find network by ID */
    @PreAuthorize(
            "hasAnyRole('admin', 'system') or"
                    + " @networkAuthorizationService.canAccessNetwork(authentication, #id)")
    public Optional<NetworkEntity> findNetworkById(Long id) {
        log.trace(
                "findNetworkById: apiUser={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return networkRepository.findById(id);
    }

    /** Get all networks */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<NetworkEntity> findAllNetworks() {
        log.trace(
                "findAllNetworks: apiUser={}",
                SecurityContextHolder.getContext().getAuthentication().getName());
        return networkRepository.findAll();
    }

    /**
     * Save a new network or update existing one
     *
     * <p>save() does INSERT if ID is null, UPDATE if ID exists.
     */
    @Transactional
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public NetworkEntity saveNetwork(NetworkEntity network) {
        log.trace(
                "saveNetwork: apiUser={}, name={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                network.getName());
        return networkRepository.save(network);
    }

    /** Delete a network */
    @Transactional
    @PreAuthorize("hasAnyRole('admin')")
    public void deleteNetwork(Long id) {
        log.trace(
                "deleteNetwork: apiUser={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        networkRepository.deleteById(id);
    }

    // ========== CUSTOM QUERY METHODS ==========

    /** Find network by name */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Optional<NetworkEntity> findNetworkByName(String name) {
        log.trace(
                "findNetworkByName: apiUser={}, name={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                name);
        return networkRepository.findByName(name);
    }

    /** Check if network exists by name */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public boolean networkExistsByName(String name) {
        log.trace(
                "networkExistsByName: apiUser={}, name={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                name);
        return networkRepository.existsByName(name);
    }

    // ========== DTO SUMMARY METHODS ==========

    /** Get all networks as DTOs */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<NetworkDto> findAllNetworkSummaries() {
        log.trace(
                "findAllNetworkSummaries: apiUser={}",
                SecurityContextHolder.getContext().getAuthentication().getName());
        List<NetworkEntity> entities = networkRepository.findAll();
        var dtos = networkMapper.toDtos(entities);
        log.trace("findAllNetworkSummaries: returning {} networks", dtos.size());
        return dtos;
    }

    // ========== DISPLAY CACHE METHODS ==========

    /**
     * Returns all networks indexed by their ID for fast display-name lookups.
     *
     * <p>Result is cached in networkDetailsCache. Called by log and history services to resolve
     * network names without additional SQL queries.
     */
    @Cacheable("networkDetailsCache")
    public Map<Long, NetworkDto> getAllNetworksAsMap() {
        log.trace("getAllNetworksAsMap: loading all networks into cache");
        return networkMapper.toDtos(networkRepository.findAll()).stream()
                .collect(Collectors.toMap(NetworkDto::id, Function.identity()));
    }

    // ========== DTO SINGLE-RECORD METHODS ==========

    @PreAuthorize(
            "hasAnyRole('admin', 'system') or"
                    + " @networkAuthorizationService.canAccessNetwork(authentication, #id)")
    public Optional<NetworkDto> findNetworkDtoById(Long id) {
        return findNetworkById(id).map(networkMapper::toDto);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public NetworkDto createNetworkAndReturnDto(SaveNetworkRequest request) {
        NetworkEntity network = networkMapper.toEntity(request);
        var now = LocalDateTime.now(ZoneOffset.UTC);
        network.setFirstSeen(now);
        network.setLastSeen(now);
        return networkMapper.toDto(saveNetwork(network));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public NetworkDto updateNetworkAndReturnDto(SaveNetworkRequest request, long id) {
        networkRepository.updateNameAndConfigurationById(
                id, request.name(), request.configuration());
        var dbEntity =
                findNetworkById(id)
                        .orElseThrow(() -> new RuntimeException("Network not found: " + id));

        return networkMapper.toDto(dbEntity);
    }
}
