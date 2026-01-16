package com.matjazt.netmon2.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.matjazt.netmon2.entity.DeviceEntity;
import com.matjazt.netmon2.entity.DeviceOperationMode;

/**
 * Repository for DeviceEntity.
 * 
 * Manages devices on monitored networks.
 */
@Repository
public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {

        /**
         * Find device by MAC address
         * 
         * MAC address is the unique identifier for devices.
         */
        Optional<DeviceEntity> findByMacAddress(String macAddress);

        /**
         * Find all devices on a specific network
         */
        List<DeviceEntity> findByNetwork_Id(Long networkId);

        /**
         * Find devices by online status
         * 
         * Useful for dashboard showing online/offline devices.
         */
        List<DeviceEntity> findByOnline(Boolean online);

        /**
         * Find online devices on a specific network
         * 
         * Combining multiple conditions.
         */
        List<DeviceEntity> findByNetwork_IdAndOnline(Long networkId, Boolean online);

        /**
         * Find devices by operation mode
         * 
         * Example: find all ALWAYS_ON devices to monitor for downtime.
         */
        List<DeviceEntity> findByDeviceOperationMode(DeviceOperationMode mode);

        /**
         * Find unauthorized devices on a network
         * 
         * These should trigger alerts.
         */
        List<DeviceEntity> findByNetwork_IdAndDeviceOperationMode(Long networkId,
                        DeviceOperationMode mode);

        /**
         * Find offline ALWAYS_ON devices
         * 
         * These devices should be online but aren't - need alerts!
         */
        List<DeviceEntity> findByDeviceOperationModeAndOnline(DeviceOperationMode mode, Boolean online);

        /**
         * Find devices with active alerts
         */
        List<DeviceEntity> findByActiveAlertIdIsNotNull();

        /**
         * Find devices not seen since a certain time
         * 
         * Useful for finding stale devices that might need cleanup.
         */
        List<DeviceEntity> findByLastSeenBefore(LocalDateTime threshold);

        /**
         * Find device by network and MAC address
         * 
         * More specific lookup when you know both network and MAC.
         */
        Optional<DeviceEntity> findByNetwork_IdAndMacAddress(Long networkId, String macAddress);

        /**
         * Check if a device exists on a network
         */
        boolean existsByNetwork_IdAndMacAddress(Long networkId, String macAddress);

        /**
         * Count devices on a network
         */
        long countByNetwork_Id(Long networkId);

        /**
         * Count online devices on a network
         */
        long countByNetwork_IdAndOnline(Long networkId, Boolean online);

        /**
         * CUSTOM QUERY: Find devices that need alerts
         * 
         * Finds ALWAYS_ON devices that are offline but don't have active alerts yet.
         */
        @Query("SELECT d FROM DeviceEntity d " +
                        "WHERE d.deviceOperationMode = :alwaysOn " +
                        "AND d.online = false " +
                        "AND d.activeAlertId IS NULL")
        List<DeviceEntity> findAlwaysOnDevicesNeedingAlert(@Param("alwaysOn") DeviceOperationMode alwaysOn);

        /**
         * CUSTOM QUERY: Find unauthorized devices needing alerts
         */
        @Query("SELECT d FROM DeviceEntity d " +
                        "WHERE d.deviceOperationMode = :unauthorized " +
                        "AND d.activeAlertId IS NULL")
        List<DeviceEntity> findUnauthorizedDevicesNeedingAlert(@Param("unauthorized") DeviceOperationMode unauthorized);

        /**
         * CUSTOM QUERY with JOIN FETCH: Get device with network loaded
         * 
         * Eagerly loads the network to avoid lazy loading issues.
         */
        @Query("SELECT d FROM DeviceEntity d " +
                        "JOIN FETCH d.network " +
                        "WHERE d.id = :id")
        Optional<DeviceEntity> findByIdWithNetwork(@Param("id") Long id);

        /**
         * Find all devices with network loaded (to avoid lazy loading issues)
         */
        @Query("SELECT d FROM DeviceEntity d JOIN FETCH d.network")
        List<DeviceEntity> findAllWithNetwork();

        /**
         * Find all devices with network loaded and pagination
         */
        @Query(value = "SELECT d FROM DeviceEntity d JOIN FETCH d.network", countQuery = "SELECT count(d) FROM DeviceEntity d")
        Page<DeviceEntity> findAllWithNetwork(Pageable pageable);

        /**
         * Find devices by network with pagination
         */
        @Query(value = "SELECT d FROM DeviceEntity d JOIN FETCH d.network WHERE d.network.id = :networkId", countQuery = "SELECT count(d) FROM DeviceEntity d WHERE d.network.id = :networkId")
        Page<DeviceEntity> findByNetworkIdWithNetwork(@Param("networkId") Long networkId, Pageable pageable);

        /**
         * MODIFYING QUERY: Bulk update last seen time
         * 
         * @Modifying tells Spring this is an UPDATE/DELETE query, not SELECT.
         *            Use @Transactional in the service layer when calling this.
         * 
         *            Returns the number of entities updated.
         */
        @Modifying
        @Query("UPDATE DeviceEntity d SET d.lastSeen = :timestamp WHERE d.id = :id")
        int updateLastSeen(@Param("id") Long id, @Param("timestamp") LocalDateTime timestamp);

        /**
         * MODIFYING QUERY: Bulk update online status for multiple devices
         * 
         * Useful for batch updates from MQTT messages.
         */
        @Modifying
        @Query("UPDATE DeviceEntity d SET d.online = :online, d.lastSeen = :timestamp " +
                        "WHERE d.network.id = :networkId AND d.macAddress IN :macAddresses")
        int bulkUpdateOnlineStatus(@Param("networkId") Long networkId,
                        @Param("macAddresses") List<String> macAddresses,
                        @Param("online") Boolean online,
                        @Param("timestamp") LocalDateTime timestamp);
}
