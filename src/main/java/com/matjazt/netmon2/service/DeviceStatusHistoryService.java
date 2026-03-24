package com.matjazt.netmon2.service;

import com.matjazt.netmon2.dto.DeviceDto;
import com.matjazt.netmon2.dto.DeviceStatusHistoryDto;
import com.matjazt.netmon2.entity.DeviceStatusHistoryEntity;
import com.matjazt.netmon2.mapper.DeviceStatusHistoryMapper;
import com.matjazt.netmon2.repository.AccountNetworkRepository;
import com.matjazt.netmon2.repository.AccountRepository;
import com.matjazt.netmon2.repository.DeviceStatusHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Service for reading DeviceStatusHistory entities.
 *
 * <p>Provides read-only access to device status history with pagination and filtering.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceStatusHistoryService {

    private final DeviceStatusHistoryRepository deviceStatusHistoryRepository;
    private final DeviceStatusHistoryMapper deviceStatusHistoryMapper;
    private final NetworkService networkService;
    private final DeviceService deviceService;
    private final AccountRepository accountRepository;
    private final AccountNetworkRepository accountNetworkRepository;

    // ========== READ-ONLY OPERATIONS ==========

    /** Find device status history by ID */
    public Optional<DeviceStatusHistoryEntity> findById(Long id) {
        log.trace(
                "findById: apiUser={}, id={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return deviceStatusHistoryRepository.findById(id);
    }

    /**
     * Get all device status history with pagination
     *
     * <p>Pageable defines page number, size, and sorting
     */
    public Page<DeviceStatusHistoryDto> getAllPaginated(int page, int size) {
        log.trace(
                "getAllPaginated: apiUser={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                page,
                size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<DeviceStatusHistoryEntity> entityPage =
                deviceStatusHistoryRepository.findAll(pageable);
        var dtoPage = enrichPage(entityPage);
        log.trace("getAllPaginated: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * Get device status history by device with pagination
     *
     * <p>Retrieves all status changes for a specific device
     */
    public Page<DeviceStatusHistoryDto> getByDevice(Long deviceId, int page, int size) {
        log.trace(
                "getByDevice: apiUser={}, deviceId={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId,
                page,
                size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<DeviceStatusHistoryEntity> entityPage =
                deviceStatusHistoryRepository.findByDevice_Id(deviceId, pageable);
        var dtoPage = enrichPage(entityPage);
        log.trace("getByDevice: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * Get device status history by network with pagination
     *
     * <p>Retrieves all status changes for all devices on a network
     */
    public Page<DeviceStatusHistoryDto> getByNetwork(Long networkId, int page, int size) {
        log.trace(
                "getByNetwork: apiUser={}, networkId={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId,
                page,
                size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<DeviceStatusHistoryEntity> entityPage =
                deviceStatusHistoryRepository.findByNetwork_Id(networkId, pageable);
        var dtoPage = enrichPage(entityPage);
        log.trace("getByNetwork: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * Get device status history within a timestamp range with pagination
     *
     * <p>Retrieves status changes between minTimestamp and maxTimestamp
     */
    public Page<DeviceStatusHistoryDto> getByTimestampRange(
            LocalDateTime minTimestamp, LocalDateTime maxTimestamp, int page, int size) {
        log.trace(
                "getByTimestampRange: apiUser={}, minTimestamp={}, maxTimestamp={}, page={},"
                        + " size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                minTimestamp,
                maxTimestamp,
                page,
                size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<DeviceStatusHistoryEntity> entityPage =
                deviceStatusHistoryRepository.findByTimestampBetween(
                        minTimestamp, maxTimestamp, pageable);
        var dtoPage = enrichPage(entityPage);
        log.trace("getByTimestampRange: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * Get device status history by device and timestamp range with pagination
     *
     * <p>Retrieves status changes for a specific device within a timestamp range
     */
    public Page<DeviceStatusHistoryDto> getByDeviceAndTimestampRange(
            Long deviceId,
            LocalDateTime minTimestamp,
            LocalDateTime maxTimestamp,
            int page,
            int size) {
        log.trace(
                "getByDeviceAndTimestampRange: apiUser={}, deviceId={}, minTimestamp={},"
                        + " maxTimestamp={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId,
                minTimestamp,
                maxTimestamp,
                page,
                size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<DeviceStatusHistoryEntity> entityPage =
                deviceStatusHistoryRepository.findByDevice_IdAndTimestampBetween(
                        deviceId, minTimestamp, maxTimestamp, pageable);
        var dtoPage = enrichPage(entityPage);
        log.trace("getByDeviceAndTimestampRange: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * Get device status history by network and timestamp range with pagination
     *
     * <p>Retrieves status changes for all devices on a network within a timestamp range
     */
    public Page<DeviceStatusHistoryDto> getByNetworkAndTimestampRange(
            Long networkId,
            LocalDateTime minTimestamp,
            LocalDateTime maxTimestamp,
            int page,
            int size) {
        log.trace(
                "getByNetworkAndTimestampRange: apiUser={}, networkId={}, minTimestamp={},"
                        + " maxTimestamp={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId,
                minTimestamp,
                maxTimestamp,
                page,
                size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<DeviceStatusHistoryEntity> entityPage =
                deviceStatusHistoryRepository.findByNetwork_IdAndTimestampBetween(
                        networkId, minTimestamp, maxTimestamp, pageable);
        var dtoPage = enrichPage(entityPage);
        log.trace("getByNetworkAndTimestampRange: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    /** Count status changes for a device */
    public long countByDevice(Long deviceId) {
        log.trace(
                "countByDevice: apiUser={}, deviceId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId);
        return deviceStatusHistoryRepository.countByDevice_Id(deviceId);
    }

    /** Count status changes in date range */
    public long countByTimestampRange(LocalDateTime start, LocalDateTime end) {
        log.trace(
                "countByTimestampRange: apiUser={}, start={}, end={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                start,
                end);
        return deviceStatusHistoryRepository.countByTimestampBetween(start, end);
    }

    /**
     * Get device status history paginated, filtered to networks accessible by the given user.
     *
     * <p>Network IDs are resolved in one query; then a single paginated IN-query fetches the
     * records. Returns an empty page when the user has no accessible networks.
     */
    public Page<DeviceStatusHistoryDto> getHistoryForUserNetworks(
            String username, int page, int size) {
        log.trace("getHistoryForUserNetworks: username={}, page={}, size={}", username, page, size);
        var account = accountRepository.findByUsername(username).orElse(null);
        if (account == null) {
            return Page.empty(PageRequest.of(page, size));
        }
        List<Long> networkIds =
                accountNetworkRepository.findByAccount_Id(account.getId()).stream()
                        .map(an -> an.getNetwork().getId())
                        .toList();
        if (networkIds.isEmpty()) {
            return Page.empty(PageRequest.of(page, size));
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<DeviceStatusHistoryEntity> entityPage =
                deviceStatusHistoryRepository.findByNetwork_IdIn(networkIds, pageable);
        var dtoPage = enrichPage(entityPage);
        log.trace("getHistoryForUserNetworks: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    public Optional<DeviceStatusHistoryDto> findDtoById(Long id) {
        return findById(id)
                .map(
                        entity ->
                                deviceStatusHistoryMapper.toDto(
                                        entity,
                                        networkService.getAllNetworksAsMap(),
                                        buildDeviceMap(List.of(entity))));
    }

    // ========== PRIVATE HELPERS ==========

    private Page<DeviceStatusHistoryDto> enrichPage(Page<DeviceStatusHistoryEntity> entityPage) {
        var networkMap = networkService.getAllNetworksAsMap();
        var deviceMap = buildDeviceMap(entityPage.getContent());
        return deviceStatusHistoryMapper.toDtoPage(entityPage, networkMap, deviceMap);
    }

    private Map<Long, DeviceDto> buildDeviceMap(Collection<DeviceStatusHistoryEntity> entities) {
        Map<Long, DeviceDto> result = new HashMap<>();
        entities.stream()
                .map(e -> e.getNetwork() != null ? e.getNetwork().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(nid -> result.putAll(deviceService.getNetworkDevicesAsMap(nid)));
        return result;
    }
}
