package com.matjazt.netmon2.service;

import com.matjazt.netmon2.dto.DeviceDto;
import com.matjazt.netmon2.dto.request.SaveDeviceRequest;
import com.matjazt.netmon2.entity.DeviceEntity;
import com.matjazt.netmon2.entity.DeviceOperationMode;
import com.matjazt.netmon2.mapper.DeviceMapper;
import com.matjazt.netmon2.repository.DeviceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Example Service demonstrating how to use Spring Data JPA repositories.
 *
 * <p>@Service marks this as a business logic component. Services orchestrate multiple repository
 * calls and add business logic.
 *
 * <p>KEY SPRING CONCEPTS:
 *
 * <ol>
 *   <li>DEPENDENCY INJECTION - Constructor injection (recommended approach) Spring automatically
 *       creates repository implementations and injects them.
 *   <li>@Transactional - Wraps method in a database transaction
 *       <ul>
 *         <li>Automatically commits on success
 *         <li>Automatically rolls back on exception
 *         <li>Required for @Modifying queries
 *       </ul>
 *   <li>Optional&lt;T&gt; - Java way to handle "not found" without nulls
 *       <ul>
 *         <li>isPresent() checks if value exists
 *         <li>get() retrieves the value (throws if empty)
 *         <li>orElse(default) provides fallback
 *         <li>orElseThrow() throws custom exception
 *       </ul>
 * </ol>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceService {

    // Dependencies injected via constructor
    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;

    // ========== BASIC CRUD OPERATIONS ==========

    /**
     * EXAMPLE: Find device by ID
     *
     * <p>Optional avoids NullPointerException - you must check if value exists.
     */
    public Optional<DeviceEntity> findDeviceById(Long id) {
        // Use fetch-join to avoid lazy loading issues when serializing
        return deviceRepository.findByIdWithNetwork(id);
    }

    /** EXAMPLE: Get all devices (be careful with large datasets!) */
    public List<DeviceEntity> findAllDevices() {
        return deviceRepository.findAll();
    }

    /**
     * EXAMPLE: Save a new device or update existing one
     *
     * <p>save() does INSERT if ID is null, UPDATE if ID exists.
     */
    @Transactional
    public DeviceEntity saveDevice(DeviceEntity device) {
        return deviceRepository.save(device);
    }

    /** EXAMPLE: Delete a device */
    @Transactional
    public void deleteDevice(Long id) {
        deviceRepository.deleteById(id);
    }

    // ========== CUSTOM QUERY EXAMPLES ==========

    /** EXAMPLE: Find online devices on a network */
    public List<DeviceEntity> findOnlineDevices(Long networkId) {
        return deviceRepository.findByNetwork_IdAndOnline(networkId, true);
    }

    /** EXAMPLE: Find device by MAC address */
    public Optional<DeviceEntity> findDeviceByMac(String macAddress) {
        return deviceRepository.findByMacAddress(macAddress);
    }

    // ========== DISPLAY CACHE METHODS ==========

    /**
     * Returns all devices for the given network, indexed by device ID, for display-name lookups.
     *
     * <p>Result is cached in deviceDetailsCache keyed by networkId. Called by log and history
     * services to resolve device names without additional SQL queries.
     */
    @Cacheable(value = "deviceDetailsCache", key = "#networkId")
    public Map<Long, DeviceDto> getNetworkDevicesAsMap(Long networkId) {
        log.trace("getNetworkDevicesAsMap: loading devices for networkId={}", networkId);
        return deviceMapper.toDtos(deviceRepository.findByNetwork_Id(networkId)).stream()
                .collect(Collectors.toMap(DeviceDto::id, Function.identity()));
    }

    // ========== DTO SUMMARY METHODS ==========

    /** Get all devices as DTOs */
    public List<DeviceDto> findAllDeviceSummaries() {
        List<DeviceEntity> entities = deviceRepository.findAllWithNetwork();
        return deviceMapper.toDtos(entities);
    }

    /** Get devices by network as DTOs */
    @PreAuthorize(
            "hasAnyRole('admin', 'system') or"
                    + " @networkAuthorizationService.canAccessNetwork(authentication, #networkId)")
    public List<DeviceDto> getDevicesByNetwork(Long networkId) {
        log.trace(
                "getDevicesByNetwork: user={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId);
        var entities =
                deviceRepository.findByNetwork_Id(
                        networkId,
                        Sort.by(
                                Sort.Order.asc("name"),
                                Sort.Order.asc("vendor"),
                                Sort.Order.asc("macAddress")));
        var dtos = deviceMapper.toDtos(entities);
        log.trace("getDevicesByNetwork: returning {} devices", dtos.size());
        return dtos;
    }

    /** Get online devices by network as DTOs */
    public List<DeviceDto> findOnlineDeviceSummaries(Long networkId) {
        List<DeviceEntity> entities = deviceRepository.findByNetwork_IdAndOnline(networkId, true);
        return deviceMapper.toDtos(entities);
    }

    /**
     * EXAMPLE: Find devices needing alerts
     *
     * <p>Demonstrates calling custom repository query methods.
     */
    public List<DeviceEntity> findDevicesNeedingAlerts() {
        List<DeviceEntity> alwaysOnDown =
                deviceRepository.findAlwaysOnDevicesNeedingAlert(DeviceOperationMode.ALWAYS_ON);

        List<DeviceEntity> unauthorized =
                deviceRepository.findUnauthorizedDevicesNeedingAlert(
                        DeviceOperationMode.UNAUTHORIZED);

        // Combine both lists
        alwaysOnDown.addAll(unauthorized);
        return alwaysOnDown;
    }

    /**
     * EXAMPLE: Get device statistics for a network
     *
     * <p>Shows how to use multiple repository methods to build a response.
     */
    public DeviceStats getDeviceStats(Long networkId) {
        long totalDevices = deviceRepository.countByNetwork_Id(networkId);
        long onlineDevices = deviceRepository.countByNetwork_IdAndOnline(networkId, true);
        long offlineDevices = totalDevices - onlineDevices;

        return new DeviceStats(totalDevices, onlineDevices, offlineDevices);
    }

    /** EXAMPLE: Check if device exists */
    public boolean deviceExists(Long networkId, String macAddress) {
        return deviceRepository.existsByNetwork_IdAndMacAddress(networkId, macAddress);
    }

    /** EXAMPLE: Update device operation mode */
    @Transactional
    public DeviceEntity updateDeviceMode(Long deviceId, DeviceOperationMode mode) {
        DeviceEntity device =
                deviceRepository
                        .findById(deviceId)
                        .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));

        device.setDeviceOperationMode(mode);
        return deviceRepository.save(device);
    }

    // ========== DTO SINGLE-RECORD METHODS ==========

    public Optional<DeviceDto> findDeviceDtoById(Long id) {
        return findDeviceById(id).map(deviceMapper::toDto);
    }

    public Optional<DeviceDto> findDeviceDtoByMac(String macAddress) {
        return findDeviceByMac(macAddress).map(deviceMapper::toDto);
    }

    public List<DeviceDto> findDevicesNeedingAlertsDtos() {
        return deviceMapper.toDtos(findDevicesNeedingAlerts());
    }

    @Transactional
    public DeviceDto saveDeviceAndReturnDto(SaveDeviceRequest request, Long id) {
        DeviceEntity device = deviceMapper.toEntity(request);
        if (id != null) device.setId(id);
        return deviceMapper.toDto(saveDevice(device));
    }

    @Transactional
    public DeviceDto updateDeviceModeAndReturnDto(Long deviceId, DeviceOperationMode mode) {
        return deviceMapper.toDto(updateDeviceMode(deviceId, mode));
    }

    // ========== INNER CLASS FOR EXAMPLE ==========

    /**
     * Simple data class for returning statistics.
     *
     * <p>In real project, this would be in a DTO package.
     */
    public static class DeviceStats {
        private final long total;
        private final long online;
        private final long offline;

        public DeviceStats(long total, long online, long offline) {
            this.total = total;
            this.online = online;
            this.offline = offline;
        }

        public long getTotal() {
            return total;
        }

        public long getOnline() {
            return online;
        }

        public long getOffline() {
            return offline;
        }
    }
}
