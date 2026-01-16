package com.matjazt.netmon2.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matjazt.netmon2.dto.response.DeviceResponseDto;
import com.matjazt.netmon2.entity.DeviceEntity;
import com.matjazt.netmon2.entity.DeviceOperationMode;
import com.matjazt.netmon2.entity.DeviceStatusHistoryEntity;
import com.matjazt.netmon2.entity.NetworkEntity;
import com.matjazt.netmon2.repository.DeviceRepository;
import com.matjazt.netmon2.repository.DeviceStatusHistoryRepository;
import com.matjazt.netmon2.repository.NetworkRepository;

/**
 * Example Service demonstrating how to use Spring Data JPA repositories.
 * 
 * @Service marks this as a business logic component.
 *          Services orchestrate multiple repository calls and add business
 *          logic.
 * 
 *          KEY SPRING CONCEPTS:
 * 
 *          1. DEPENDENCY INJECTION - Constructor injection (recommended
 *          approach)
 *          Spring automatically creates repository implementations and injects
 *          them.
 * 
 *          2. @Transactional - Wraps method in a database transaction
 *          - Automatically commits on success
 *          - Automatically rolls back on exception
 *          - Required for @Modifying queries
 * 
 *          3. Optional<T> - Java way to handle "not found" without nulls
 *          - isPresent() checks if value exists
 *          - get() retrieves the value (throws if empty)
 *          - orElse(default) provides fallback
 *          - orElseThrow() throws custom exception
 */
@Service
public class DeviceService {

    // Dependencies injected via constructor
    private final DeviceRepository deviceRepository;
    private final NetworkRepository networkRepository;
    private final DeviceStatusHistoryRepository statusHistoryRepository;

    /**
     * Constructor injection - Spring automatically provides the implementations.
     * 
     * This is preferred over @Autowired field injection because:
     * - Makes dependencies explicit and testable
     * - Allows final fields (immutability)
     * - Easier to mock in unit tests
     */
    public DeviceService(DeviceRepository deviceRepository,
            NetworkRepository networkRepository,
            DeviceStatusHistoryRepository statusHistoryRepository) {
        this.deviceRepository = deviceRepository;
        this.networkRepository = networkRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    // ========== BASIC CRUD OPERATIONS ==========

    /**
     * EXAMPLE: Find device by ID
     * 
     * Optional avoids NullPointerException - you must check if value exists.
     */
    public Optional<DeviceEntity> findDeviceById(Long id) {
        // Use fetch-join to avoid lazy loading issues when serializing
        return deviceRepository.findByIdWithNetwork(id);
    }

    /**
     * EXAMPLE: Get all devices (be careful with large datasets!)
     */
    public List<DeviceEntity> findAllDevices() {
        return deviceRepository.findAll();
    }

    /**
     * EXAMPLE: Save a new device or update existing one
     * 
     * save() does INSERT if ID is null, UPDATE if ID exists.
     */
    @Transactional
    public DeviceEntity saveDevice(DeviceEntity device) {
        return deviceRepository.save(device);
    }

    /**
     * EXAMPLE: Delete a device
     */
    @Transactional
    public void deleteDevice(Long id) {
        deviceRepository.deleteById(id);
    }

    // ========== CUSTOM QUERY EXAMPLES ==========

    /**
     * EXAMPLE: Find devices on a specific network
     */
    public List<DeviceEntity> findDevicesByNetwork(Long networkId) {
        return deviceRepository.findByNetwork_Id(networkId);
    }

    /**
     * EXAMPLE: Find online devices on a network
     */
    public List<DeviceEntity> findOnlineDevices(Long networkId) {
        return deviceRepository.findByNetwork_IdAndOnline(networkId, true);
    }

    /**
     * EXAMPLE: Find device by MAC address
     */
    public Optional<DeviceEntity> findDeviceByMac(String macAddress) {
        return deviceRepository.findByMacAddress(macAddress);
    }

    // ========== DTO SUMMARY METHODS ==========

    public List<DeviceResponseDto> findAllDeviceSummaries() {
        return deviceRepository.findAllSummaries();
    }

    public List<DeviceResponseDto> findDeviceSummariesByNetwork(Long networkId) {
        return deviceRepository.findSummariesByNetworkId(networkId);
    }

    public List<DeviceResponseDto> findOnlineDeviceSummaries(Long networkId) {
        return deviceRepository.findOnlineSummariesByNetworkId(networkId);
    }

    // ========== BUSINESS LOGIC EXAMPLES ==========

    /**
     * EXAMPLE: Process MQTT device update
     * 
     * This is real business logic combining multiple operations:
     * 1. Find or create device
     * 2. Check if status changed
     * 3. Update device
     * 4. Record history if status changed
     * 
     * @Transactional ensures all-or-nothing: if any step fails, everything rolls
     *                back.
     */
    @Transactional
    public DeviceEntity processDeviceUpdate(Long networkId, String macAddress,
            String ipAddress, Boolean online) {
        // Find existing device or create new one
        DeviceEntity device = deviceRepository.findByNetwork_IdAndMacAddress(networkId, macAddress)
                .orElseGet(() -> {
                    // Device doesn't exist - create it
                    NetworkEntity network = networkRepository.findById(networkId)
                            .orElseThrow(() -> new RuntimeException("Network not found: " + networkId));

                    DeviceEntity newDevice = new DeviceEntity(network, macAddress, ipAddress, online);
                    newDevice.setDeviceOperationMode(DeviceOperationMode.AUTHORIZED); // Default mode
                    return newDevice;
                });

        // Check if online status changed
        boolean statusChanged = device.getOnline() != null && !device.getOnline().equals(online);

        // Update device
        device.setIpAddress(ipAddress);
        device.setOnline(online);
        device.updateLastSeen();
        device = deviceRepository.save(device);

        // If status changed, record history
        if (statusChanged) {
            NetworkEntity network = device.getNetwork();
            DeviceStatusHistoryEntity history = new DeviceStatusHistoryEntity(
                    network, device, ipAddress, online, LocalDateTime.now(ZoneOffset.UTC));
            statusHistoryRepository.save(history);
        }

        return device;
    }

    /**
     * EXAMPLE: Find devices needing alerts
     * 
     * Demonstrates calling custom repository query methods.
     */
    public List<DeviceEntity> findDevicesNeedingAlerts() {
        List<DeviceEntity> alwaysOnDown = deviceRepository
                .findAlwaysOnDevicesNeedingAlert(DeviceOperationMode.ALWAYS_ON);

        List<DeviceEntity> unauthorized = deviceRepository
                .findUnauthorizedDevicesNeedingAlert(DeviceOperationMode.UNAUTHORIZED);

        // Combine both lists
        alwaysOnDown.addAll(unauthorized);
        return alwaysOnDown;
    }

    /**
     * EXAMPLE: Get device statistics for a network
     * 
     * Shows how to use multiple repository methods to build a response.
     */
    public DeviceStats getDeviceStats(Long networkId) {
        long totalDevices = deviceRepository.countByNetwork_Id(networkId);
        long onlineDevices = deviceRepository.countByNetwork_IdAndOnline(networkId, true);
        long offlineDevices = totalDevices - onlineDevices;

        return new DeviceStats(totalDevices, onlineDevices, offlineDevices);
    }

    /**
     * EXAMPLE: Pagination - get devices page by page
     * 
     * Pageable defines page number, size, and sorting.
     * Page contains results + metadata (total pages, total elements, etc.)
     */
    public Page<DeviceResponseDto> getDeviceSummariesPaginated(Long networkId, int page, int size) {
        // Create Pageable: page 0 is first page, sort by name ascending
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        if (networkId != null) {
            return deviceRepository.findSummariesByNetworkId(networkId, pageable);
        }
        return deviceRepository.findAllSummaries(pageable);
    }

    /**
     * EXAMPLE: Get device history
     */
    public List<DeviceStatusHistoryEntity> getDeviceHistory(Long deviceId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("timestamp").descending());
        Page<DeviceStatusHistoryEntity> page = statusHistoryRepository.findByDevice_Id(deviceId, pageable);
        return page.getContent();
    }

    /**
     * EXAMPLE: Check if device exists
     */
    public boolean deviceExists(Long networkId, String macAddress) {
        return deviceRepository.existsByNetwork_IdAndMacAddress(networkId, macAddress);
    }

    /**
     * EXAMPLE: Update device operation mode
     */
    @Transactional
    public DeviceEntity updateDeviceMode(Long deviceId, DeviceOperationMode mode) {
        DeviceEntity device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));

        device.setDeviceOperationMode(mode);
        return deviceRepository.save(device);
    }

    // ========== INNER CLASS FOR EXAMPLE ==========

    /**
     * Simple data class for returning statistics.
     * In real project, this would be in a DTO package.
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
