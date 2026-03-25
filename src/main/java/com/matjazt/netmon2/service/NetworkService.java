package com.matjazt.netmon2.service;

import com.matjazt.netmon2.dto.NetworkDto;
import com.matjazt.netmon2.dto.request.SaveNetworkRequest;
import com.matjazt.netmon2.entity.NetworkEntity;
import com.matjazt.netmon2.mapper.NetworkMapper;
import com.matjazt.netmon2.repository.NetworkRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
    private final NetworkConfigurationService networkConfigurationService;

    // ========== BASIC CRUD OPERATIONS ==========

    /** Find network by ID */
    public Optional<NetworkEntity> findNetworkById(Long id) {
        log.trace(
                "findNetworkById: apiUser={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return networkRepository.findById(id);
    }

    /** Get all networks */
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
    public NetworkEntity saveNetwork(NetworkEntity network) {
        log.trace(
                "saveNetwork: apiUser={}, name={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                network.getName());
        return networkRepository.save(network);
    }

    /** Delete a network */
    @Transactional
    public void deleteNetwork(Long id) {
        if (!networkRepository.existsById(id)) {
            log.warn("deleteNetwork: network with id={} does not exist, cannot delete", id);
            throw new NoSuchElementException("Network not found: " + id);
        }
        log.trace(
                "deleteNetwork: apiUser={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        networkRepository.deleteById(id);
    }

    // ========== CUSTOM QUERY METHODS ==========

    /** Find network by name */
    public Optional<NetworkEntity> findNetworkByName(String name) {
        log.trace(
                "findNetworkByName: apiUser={}, name={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                name);
        return networkRepository.findByName(name);
    }

    /** Check if network exists by name */
    public boolean networkExistsByName(String name) {
        log.trace(
                "networkExistsByName: apiUser={}, name={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                name);
        return networkRepository.existsByName(name);
    }

    // ========== DTO SUMMARY METHODS ==========

    /** Get all networks as DTOs */
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

    public Optional<NetworkDto> findNetworkDtoById(Long id) {
        return findNetworkById(id).map(networkMapper::toDto);
    }

    @Transactional
    public NetworkDto createNetworkAndReturnDto(SaveNetworkRequest request) {
        networkConfigurationService.validateConfigurationJson(request.configuration() + "TESTIRAM");
        NetworkEntity network = networkMapper.toEntity(request);
        var now = LocalDateTime.now(ZoneOffset.UTC);
        network.setFirstSeen(now);
        network.setLastSeen(now);
        return networkMapper.toDto(saveNetwork(network));
    }

    @Transactional
    public NetworkDto updateNetworkAndReturnDto(SaveNetworkRequest request, long id) {
        networkConfigurationService.validateConfigurationJson(request.configuration());
        networkRepository.updateNameAndConfigurationById(
                id, request.name(), request.configuration());
        var dbEntity =
                findNetworkById(id)
                        .orElseThrow(() -> new RuntimeException("Network not found: " + id));

        return networkMapper.toDto(dbEntity);
    }
}
