package com.matjazt.netmon2.repository;

import com.matjazt.netmon2.entity.DeviceStatusHistoryEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for DeviceStatusHistoryEntity.
 *
 * <p>Manages historical device status changes (online/offline events). This is append-only data for
 * audit trail and analytics.
 */
@Repository
public interface DeviceStatusHistoryRepository
        extends JpaRepository<DeviceStatusHistoryEntity, Long> {

    /**
     * Find status history for a specific device
     *
     * <p>Ordered by timestamp descending (newest first).
     */
    List<DeviceStatusHistoryEntity> findByDevice_IdOrderByTimestampDesc(Long deviceId);

    /**
     * Find status history for a device with pagination
     *
     * <p>Better for devices with lots of history.
     *
     * <p>Usage: findByDevice_Id(id, PageRequest.of(0, 50))
     */
    Page<DeviceStatusHistoryEntity> findByDevice_Id(Long deviceId, Pageable pageable);

    /**
     * Find status history for a network
     *
     * <p>All status changes across all devices on the network.
     */
    List<DeviceStatusHistoryEntity> findByNetwork_IdOrderByTimestampDesc(Long networkId);

    /**
     * Find status history within a date range
     *
     * <p>Useful for generating reports: "Show me all status changes last week"
     */
    List<DeviceStatusHistoryEntity> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find status history for a device within date range
     *
     * <p>Combining device filter with date range.
     */
    List<DeviceStatusHistoryEntity> findByDevice_IdAndTimestampBetween(
            Long deviceId, LocalDateTime start, LocalDateTime end);

    /**
     * Find only "went online" events
     *
     * <p>Filter by online status.
     */
    List<DeviceStatusHistoryEntity> findByOnlineOrderByTimestampDesc(Boolean online);

    /** Find "went offline" events for a device */
    List<DeviceStatusHistoryEntity> findByDevice_IdAndOnlineOrderByTimestampDesc(
            Long deviceId, Boolean online);

    /**
     * CUSTOM QUERY: Get latest status change for each device on a network
     *
     * <p>Uses DISTINCT ON pattern (PostgreSQL) or subquery approach. This finds the most recent
     * status change per device.
     */
    @Query(
            "SELECT h FROM DeviceStatusHistoryEntity h "
                    + "WHERE h.network.id = :networkId "
                    + "AND h.timestamp = ("
                    + "  SELECT MAX(h2.timestamp) "
                    + "  FROM DeviceStatusHistoryEntity h2 "
                    + "  WHERE h2.device.id = h.device.id"
                    + ") "
                    + "ORDER BY h.timestamp DESC")
    List<DeviceStatusHistoryEntity> findLatestStatusPerDevice(@Param("networkId") Long networkId);

    /** CUSTOM QUERY: Get latest status change for the given device */
    @Query(
            "SELECT h FROM DeviceStatusHistoryEntity h "
                    + "WHERE h.network.id = :networkId "
                    + "AND h.device.id = :deviceId "
                    + "ORDER BY h.timestamp DESC "
                    + "LIMIT 1")
    DeviceStatusHistoryEntity findLatestHistoryEntryByDevice(
            @Param("networkId") Long networkId, @Param("deviceId") Long deviceId);

    /**
     * CUSTOM QUERY: Get currently online devices on a network NOTE: index or query optimization
     * needed, but since this method is currentlynot used, we skip it for now.
     */
    @Query(
            "SELECT h FROM DeviceStatusHistoryEntity h "
                    + "WHERE h.network.id = :networkId "
                    + "AND h.online = true "
                    + "AND h.timestamp = ("
                    + "  SELECT MAX(h2.timestamp) "
                    + "  FROM DeviceStatusHistoryEntity h2 "
                    + "  WHERE h2.device.id = h.device.id"
                    + ") "
                    + "ORDER BY h.timestamp DESC")
    List<DeviceStatusHistoryEntity> findCurrentlyOnlineDevices(@Param("networkId") Long networkId);

    /**
     * CUSTOM QUERY: Calculate uptime percentage for a device
     *
     * <p>Returns aggregated data: total time online vs total time in period. Result is Object[]
     * with [deviceId, totalOnlineMinutes, totalMinutes]
     */
    @Query(
            "SELECT h.device.id, "
                    + "SUM(CASE WHEN h.online = true THEN 1 ELSE 0 END), "
                    + "COUNT(h) "
                    + "FROM DeviceStatusHistoryEntity h "
                    + "WHERE h.device.id = :deviceId "
                    + "AND h.timestamp BETWEEN :start AND :end "
                    + "GROUP BY h.device.id")
    Object[] calculateUptime(
            @Param("deviceId") Long deviceId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * CUSTOM QUERY: Find devices with frequent status changes
     *
     * <p>Identifies flaky devices that toggle online/offline repeatedly. Returns [deviceId,
     * changeCount] for devices with more than threshold changes.
     */
    @Query(
            "SELECT h.device.id, COUNT(h) "
                    + "FROM DeviceStatusHistoryEntity h "
                    + "WHERE h.timestamp BETWEEN :start AND :end "
                    + "GROUP BY h.device.id "
                    + "HAVING COUNT(h) > :threshold")
    List<Object[]> findFlakyDevices(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("threshold") long threshold);

    /** Count status changes for a device */
    long countByDevice_Id(Long deviceId);

    /** Count status changes in date range */
    long countByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
