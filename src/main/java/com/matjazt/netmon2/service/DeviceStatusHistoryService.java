package com.matjazt.netmon2.service;

import com.matjazt.netmon2.dto.DeviceStatusHistoryDto;
import com.matjazt.netmon2.entity.DeviceStatusHistoryEntity;
import com.matjazt.netmon2.mapper.DeviceStatusHistoryMapper;
import com.matjazt.netmon2.repository.DeviceStatusHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    // ========== READ-ONLY OPERATIONS ==========

    /** Find device status history by ID */
    @PreAuthorize("hasAnyRole('admin', 'system', 'user')")
    public Optional<DeviceStatusHistoryEntity> findById(Long id) {
        log.trace(
                "findById: user={}, id={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return deviceStatusHistoryRepository.findById(id);
    }

    /**
     * Get all device status history with pagination
     *
     * <p>Pageable defines page number, size, and sorting
     */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Page<DeviceStatusHistoryDto> getAllPaginated(int page, int size) {
        log.trace(
                "getAllPaginated: user={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                page,
                size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<DeviceStatusHistoryEntity> entityPage =
                deviceStatusHistoryRepository.findAll(pageable);
        var dtoPage = deviceStatusHistoryMapper.toDtoPage(entityPage);
        log.trace("getAllPaginated: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * Get device status history by device with pagination
     *
     * <p>Retrieves all status changes for a specific device
     */
    @PreAuthorize("hasAnyRole('admin', 'system', 'user')")
    public Page<DeviceStatusHistoryDto> getByDevice(Long deviceId, int page, int size) {
        log.trace(
                "getByDevice: user={}, deviceId={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId,
                page,
                size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<DeviceStatusHistoryEntity> entityPage =
                deviceStatusHistoryRepository.findByDevice_Id(deviceId, pageable);
        var dtoPage = deviceStatusHistoryMapper.toDtoPage(entityPage);
        log.trace("getByDevice: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * Get device status history by network with pagination
     *
     * <p>Retrieves all status changes for all devices on a network
     */
    @PreAuthorize(
            "hasAnyRole('admin', 'system') or"
                    + " @networkAuthorizationService.canAccess(authentication, #networkId)")
    public Page<DeviceStatusHistoryDto> getByNetwork(Long networkId, int page, int size) {
        log.trace(
                "getByNetwork: user={}, networkId={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId,
                page,
                size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<DeviceStatusHistoryEntity> entityPage =
                deviceStatusHistoryRepository.findByNetwork_Id(networkId, pageable);
        var dtoPage = deviceStatusHistoryMapper.toDtoPage(entityPage);
        log.trace("getByNetwork: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * Get device status history within a timestamp range with pagination
     *
     * <p>Retrieves status changes between minTimestamp and maxTimestamp
     */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Page<DeviceStatusHistoryDto> getByTimestampRange(
            LocalDateTime minTimestamp, LocalDateTime maxTimestamp, int page, int size) {
        log.trace(
                "getByTimestampRange: user={}, minTimestamp={}, maxTimestamp={}, page={}, size={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                minTimestamp,
                maxTimestamp,
                page,
                size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<DeviceStatusHistoryEntity> entityPage =
                deviceStatusHistoryRepository.findByTimestampBetween(
                        minTimestamp, maxTimestamp, pageable);
        var dtoPage = deviceStatusHistoryMapper.toDtoPage(entityPage);
        log.trace("getByTimestampRange: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * Get device status history by device and timestamp range with pagination
     *
     * <p>Retrieves status changes for a specific device within a timestamp range
     */
    @PreAuthorize("hasAnyRole('admin', 'system', 'user')")
    public Page<DeviceStatusHistoryDto> getByDeviceAndTimestampRange(
            Long deviceId,
            LocalDateTime minTimestamp,
            LocalDateTime maxTimestamp,
            int page,
            int size) {
        log.trace(
                "getByDeviceAndTimestampRange: user={}, deviceId={}, minTimestamp={},"
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
        var dtoPage = deviceStatusHistoryMapper.toDtoPage(entityPage);
        log.trace("getByDeviceAndTimestampRange: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    /**
     * Get device status history by network and timestamp range with pagination
     *
     * <p>Retrieves status changes for all devices on a network within a timestamp range
     */
    @PreAuthorize(
            "hasAnyRole('admin', 'system') or"
                    + " @networkAuthorizationService.canAccess(authentication, #networkId)")
    public Page<DeviceStatusHistoryDto> getByNetworkAndTimestampRange(
            Long networkId,
            LocalDateTime minTimestamp,
            LocalDateTime maxTimestamp,
            int page,
            int size) {
        log.trace(
                "getByNetworkAndTimestampRange: user={}, networkId={}, minTimestamp={},"
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
        var dtoPage = deviceStatusHistoryMapper.toDtoPage(entityPage);
        log.trace("getByNetworkAndTimestampRange: returning {} history records", dtoPage.getSize());
        return dtoPage;
    }

    /** Count status changes for a device */
    @PreAuthorize("hasAnyRole('admin', 'system', 'user')")
    public long countByDevice(Long deviceId) {
        log.trace(
                "countByDevice: user={}, deviceId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                deviceId);
        return deviceStatusHistoryRepository.countByDevice_Id(deviceId);
    }

    /** Count status changes in date range */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public long countByTimestampRange(LocalDateTime start, LocalDateTime end) {
        log.trace(
                "countByTimestampRange: user={}, start={}, end={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                start,
                end);
        return deviceStatusHistoryRepository.countByTimestampBetween(start, end);
    }

    @PreAuthorize("hasAnyRole('admin', 'system', 'user')")
    public Optional<DeviceStatusHistoryDto> findDtoById(Long id) {
        return findById(id).map(deviceStatusHistoryMapper::toDto);
    }
}
