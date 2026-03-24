package com.matjazt.netmon2.service;

import com.matjazt.netmon2.dto.DeviceDto;
import com.matjazt.netmon2.dto.LogDto;
import com.matjazt.netmon2.entity.LogEntity;
import com.matjazt.netmon2.mapper.LogMapper;
import com.matjazt.netmon2.repository.AccountNetworkRepository;
import com.matjazt.netmon2.repository.AccountRepository;
import com.matjazt.netmon2.repository.LogRepository;

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
 * Service for reading Log entities.
 *
 * <p>Provides read-only access to log entries with pagination and filtering.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;
    private final LogMapper logMapper;
    private final NetworkService networkService;
    private final DeviceService deviceService;
    private final AccountRepository accountRepository;
    private final AccountNetworkRepository accountNetworkRepository;

    // ========== READ-ONLY OPERATIONS ==========

    /** Find log by ID */
    public Optional<LogEntity> findLogById(Long id) {
        log.trace(
                "findLogById: apiUser={}, logId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return logRepository.findById(id);
    }

    /**
     * Get all logs with pagination
     *
     * <p>Pageable defines page number, size, and sorting
     */
    public Page<LogDto> getAllLogsPaginated(int page, int size) {
        log.trace(
                "getAllLogsPaginated: apiUser={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                page,
                size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<LogEntity> entityPage = logRepository.findAll(pageable);
        var dtoPage = enrichPage(entityPage);
        log.trace("getAllLogsPaginated: returning {} logs", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * Get logs by network with pagination
     *
     * <p>Retrieves all log entries for a specific network
     */
    public Page<LogDto> getLogsByNetwork(Long networkId, int page, int size) {
        log.trace(
                "getLogsByNetwork: apiUser={}, networkId={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId,
                page,
                size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<LogEntity> entityPage = logRepository.findByNetwork_Id(networkId, pageable);
        var dtoPage = enrichPage(entityPage);
        log.trace("getLogsByNetwork: returning {} logs", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * Get logs by device with pagination
     *
     * <p>Retrieves all log entries for a specific device
     */
    public Page<LogDto> getLogsByDevice(Long deviceId, int page, int size) {
        log.trace(
                "getLogsByDevice: apiUser={}, deviceId={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId,
                page,
                size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<LogEntity> entityPage = logRepository.findByDevice_Id(deviceId, pageable);
        var dtoPage = enrichPage(entityPage);
        log.trace("getLogsByDevice: returning {} logs", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * Get logs within a timestamp range with pagination
     *
     * <p>Retrieves log entries between minTimestamp and maxTimestamp
     */
    public Page<LogDto> getLogsByTimestampRange(
            LocalDateTime minTimestamp, LocalDateTime maxTimestamp, int page, int size) {
        log.trace(
                "getLogsByTimestampRange: apiUser={}, minTimestamp={}, maxTimestamp={}, page={},"
                        + " size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                minTimestamp,
                maxTimestamp,
                page,
                size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<LogEntity> entityPage =
                logRepository.findByTimestampBetween(minTimestamp, maxTimestamp, pageable);
        var dtoPage = enrichPage(entityPage);
        log.trace("getLogsByTimestampRange: returning {} logs", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * Get logs by network and timestamp range with pagination
     *
     * <p>Retrieves log entries for a specific network within a timestamp range
     */
    public Page<LogDto> getLogsByNetworkAndTimestampRange(
            Long networkId,
            LocalDateTime minTimestamp,
            LocalDateTime maxTimestamp,
            int page,
            int size) {
        log.trace(
                "getLogsByNetworkAndTimestampRange: apiUser={}, networkId={}, minTimestamp={},"
                        + " maxTimestamp={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId,
                minTimestamp,
                maxTimestamp,
                page,
                size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<LogEntity> entityPage =
                logRepository.findByNetwork_IdAndTimestampBetween(
                        networkId, minTimestamp, maxTimestamp, pageable);
        var dtoPage = enrichPage(entityPage);
        log.trace("getLogsByNetworkAndTimestampRange: returning {} logs", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * Get logs by device and timestamp range with pagination
     *
     * <p>Retrieves log entries for a specific device within a timestamp range
     */
    public Page<LogDto> getLogsByDeviceAndTimestampRange(
            Long deviceId,
            LocalDateTime minTimestamp,
            LocalDateTime maxTimestamp,
            int page,
            int size) {
        log.trace(
                "getLogsByDeviceAndTimestampRange: apiUser={}, deviceId={}, minTimestamp={},"
                        + " maxTimestamp={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId,
                minTimestamp,
                maxTimestamp,
                page,
                size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<LogEntity> entityPage =
                logRepository.findByDevice_IdAndTimestampBetween(
                        deviceId, minTimestamp, maxTimestamp, pageable);
        var dtoPage = enrichPage(entityPage);
        log.trace("getLogsByDeviceAndTimestampRange: returning {} logs", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * Get logs paginated, filtered to networks accessible by the given user.
     *
     * <p>Network IDs are resolved in one query; then a single paginated IN-query fetches the logs.
     * Returns an empty page when the user has no accessible networks.
     */
    public Page<LogDto> getLogsForUserNetworks(String username, int page, int size) {
        log.trace("getLogsForUserNetworks: username={}, page={}, size={}", username, page, size);
        var account = accountRepository.findByUsername(username).orElseThrow();
        List<Long> networkIds =
                accountNetworkRepository.findByAccount_Id(account.getId()).stream()
                        .map(an -> an.getNetwork().getId())
                        .toList();
        if (networkIds.isEmpty()) {
            return Page.empty(PageRequest.of(page, size));
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<LogEntity> entityPage = logRepository.findByNetwork_IdIn(networkIds, pageable);
        var dtoPage = enrichPage(entityPage);
        log.trace("getLogsForUserNetworks: returning {} logs", dtoPage.getSize());
        return dtoPage;
    }

    public Optional<LogDto> findLogDtoById(Long id) {
        return findLogById(id)
                .map(
                        entity ->
                                logMapper.toDto(
                                        entity,
                                        networkService.getAllNetworksAsMap(),
                                        buildDeviceMap(List.of(entity))));
    }

    // ========== PRIVATE HELPERS ==========

    private Page<LogDto> enrichPage(Page<LogEntity> entityPage) {
        var networkMap = networkService.getAllNetworksAsMap();
        var deviceMap = buildDeviceMap(entityPage.getContent());
        return logMapper.toDtoPage(entityPage, networkMap, deviceMap);
    }

    private Map<Long, DeviceDto> buildDeviceMap(Collection<LogEntity> entities) {
        Map<Long, DeviceDto> result = new HashMap<>();
        entities.stream()
                .map(e -> e.getNetwork() != null ? e.getNetwork().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(nid -> result.putAll(deviceService.getNetworkDevicesAsMap(nid)));
        return result;
    }
}
