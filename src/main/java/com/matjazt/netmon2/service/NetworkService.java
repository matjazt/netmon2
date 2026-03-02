package com.matjazt.netmon2.service;

import com.matjazt.netmon2.dto.NetworkDto;
import com.matjazt.netmon2.entity.NetworkEntity;
import com.matjazt.netmon2.mapper.NetworkMapper;
import com.matjazt.netmon2.repository.NetworkRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
                    + " @networkAuthorizationService.canAccess(authentication, #id)")
    public Optional<NetworkEntity> findNetworkById(Long id) {
        logger.trace(
                "findNetworkById: user={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return networkRepository.findById(id);
    }

    /** Get all networks */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<NetworkEntity> findAllNetworks() {
        logger.trace(
                "findAllNetworks: user={}",
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
        logger.trace(
                "saveNetwork: user={}, name={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                network.getName());
        return networkRepository.save(network);
    }

    /** Delete a network */
    @Transactional
    @PreAuthorize("hasAnyRole('admin')")
    public void deleteNetwork(Long id) {
        logger.trace(
                "deleteNetwork: user={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        networkRepository.deleteById(id);
    }

    // ========== CUSTOM QUERY METHODS ==========

    /** Find network by name */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Optional<NetworkEntity> findNetworkByName(String name) {
        logger.trace(
                "findNetworkByName: user={}, name={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                name);
        return networkRepository.findByName(name);
    }

    /** Check if network exists by name */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public boolean networkExistsByName(String name) {
        logger.trace(
                "networkExistsByName: user={}, name={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                name);
        return networkRepository.existsByName(name);
    }

    /** Find all networks with active alerts */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<NetworkEntity> findNetworksWithActiveAlerts() {
        logger.trace(
                "findNetworksWithActiveAlerts: user={}",
                SecurityContextHolder.getContext().getAuthentication().getName());
        var networks = networkRepository.findByActiveAlertIdIsNotNull();
        logger.trace("findNetworksWithActiveAlerts: returning {} networks", networks.size());
        return networks;
    }

    /** Find all networks without active alerts */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<NetworkEntity> findNetworksWithoutActiveAlerts() {
        logger.trace(
                "findNetworksWithoutActiveAlerts: user={}",
                SecurityContextHolder.getContext().getAuthentication().getName());
        var networks = networkRepository.findByActiveAlertIdIsNull();
        logger.trace("findNetworksWithoutActiveAlerts: returning {} networks", networks.size());
        return networks;
    }

    /** Find networks by partial name match */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<NetworkEntity> findNetworksByNameContaining(String namePart) {
        logger.trace(
                "findNetworksByNameContaining: user={}, namePart={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                namePart);
        var networks = networkRepository.findByNameContainingIgnoreCase(namePart);
        logger.trace("findNetworksByNameContaining: returning {} networks", networks.size());
        return networks;
    }

    // ========== DTO SUMMARY METHODS ==========

    /** Get all networks as DTOs */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<NetworkDto> findAllNetworkSummaries() {
        logger.trace(
                "findAllNetworkSummaries: user={}",
                SecurityContextHolder.getContext().getAuthentication().getName());
        List<NetworkEntity> entities = networkRepository.findAll();
        var dtos = networkMapper.toDtos(entities);
        logger.trace("findAllNetworkSummaries: returning {} networks", dtos.size());
        return dtos;
    }

    /**
     * Pagination - get networks page by page
     *
     * <p>Pageable defines page number, size, and sorting. Page contains results + metadata (total
     * pages, total elements, etc.)
     */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Page<NetworkDto> getNetworkSummariesPaginated(int page, int size) {
        logger.trace(
                "getNetworkSummariesPaginated: user={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                page,
                size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<NetworkEntity> entityPage = networkRepository.findAll(pageable);
        var dtoPage = networkMapper.toDtoPage(entityPage);
        logger.trace("getNetworkSummariesPaginated: returning {} networks", dtoPage.getSize());
        return dtoPage;
    }
}
