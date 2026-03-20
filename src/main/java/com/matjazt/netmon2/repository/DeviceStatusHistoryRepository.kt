
package com.matjazt.netmon2.repository

import com.matjazt.netmon2.entity.DeviceStatusHistoryEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Repository for DeviceStatusHistoryEntity.
 *
 * Manages historical device status changes (online/offline events). This is append-only data for
 * audit trail and analytics.
 */
@Repository
open interface DeviceStatusHistoryRepository : JpaRepository<DeviceStatusHistoryEntity, Long> {

    fun findByDevice_IdOrderByTimestampDesc(deviceId: Long): List<DeviceStatusHistoryEntity>
    fun findByDevice_Id(deviceId: Long, pageable: Pageable): Page<DeviceStatusHistoryEntity>
    fun findByNetwork_IdOrderByTimestampDesc(networkId: Long): List<DeviceStatusHistoryEntity>
    fun findByNetwork_Id(networkId: Long, pageable: Pageable): Page<DeviceStatusHistoryEntity>
    fun findByTimestampBetween(start: LocalDateTime, end: LocalDateTime): List<DeviceStatusHistoryEntity>
    fun findByTimestampBetween(start: LocalDateTime, end: LocalDateTime, pageable: Pageable): Page<DeviceStatusHistoryEntity>
    fun findByDevice_IdAndTimestampBetween(deviceId: Long, start: LocalDateTime, end: LocalDateTime): List<DeviceStatusHistoryEntity>
    fun findByDevice_IdAndTimestampBetween(deviceId: Long, start: LocalDateTime, end: LocalDateTime, pageable: Pageable): Page<DeviceStatusHistoryEntity>
    fun findByNetwork_IdAndTimestampBetween(networkId: Long, start: LocalDateTime, end: LocalDateTime, pageable: Pageable): Page<DeviceStatusHistoryEntity>
    fun findByOnlineOrderByTimestampDesc(online: Boolean): List<DeviceStatusHistoryEntity>
    fun findByDevice_IdAndOnlineOrderByTimestampDesc(deviceId: Long, online: Boolean): List<DeviceStatusHistoryEntity>
    @Query(
        "SELECT h FROM DeviceStatusHistoryEntity h " +
        "WHERE h.network.id = :networkId " +
        "AND h.timestamp = (" +
        "  SELECT MAX(h2.timestamp) " +
        "  FROM DeviceStatusHistoryEntity h2 " +
        "  WHERE h2.device.id = h.device.id" +
        ") " +
        "ORDER BY h.timestamp DESC"
    )
    fun findLatestStatusPerDevice(@Param("networkId") networkId: Long): List<DeviceStatusHistoryEntity>
    @Query(
        "SELECT h FROM DeviceStatusHistoryEntity h " +
        "WHERE h.network.id = :networkId " +
        "AND h.device.id = :deviceId " +
        "ORDER BY h.timestamp DESC " +
        "LIMIT 1"
    )
    fun findLatestHistoryEntryByDevice(
        @Param("networkId") networkId: Long,
        @Param("deviceId") deviceId: Long
    ): DeviceStatusHistoryEntity
    @Query(
        "SELECT h FROM DeviceStatusHistoryEntity h " +
        "WHERE h.network.id = :networkId " +
        "AND h.online = true " +
        "AND h.timestamp = (" +
        "  SELECT MAX(h2.timestamp) " +
        "  FROM DeviceStatusHistoryEntity h2 " +
        "  WHERE h2.device.id = h.device.id" +
        ") " +
        "ORDER BY h.timestamp DESC"
    )
    fun findCurrentlyOnlineDevices(@Param("networkId") networkId: Long): List<DeviceStatusHistoryEntity>
    @Query(
        "SELECT h.device.id, " +
        "SUM(CASE WHEN h.online = true THEN 1 ELSE 0 END), " +
        "COUNT(h) " +
        "FROM DeviceStatusHistoryEntity h " +
        "WHERE h.device.id = :deviceId " +
        "AND h.timestamp BETWEEN :start AND :end " +
        "GROUP BY h.device.id"
    )
    fun calculateUptime(
        @Param("deviceId") deviceId: Long,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): Array<Any>
    @Query(
        "SELECT h.device.id, COUNT(h) " +
        "FROM DeviceStatusHistoryEntity h " +
        "WHERE h.timestamp BETWEEN :start AND :end " +
        "GROUP BY h.device.id " +
        "HAVING COUNT(h) > :threshold"
    )
    fun findFlakyDevices(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
        @Param("threshold") threshold: Long
    ): List<Array<Any>>
    fun countByDevice_Id(deviceId: Long): Long
    fun countByTimestampBetween(start: LocalDateTime, end: LocalDateTime): Long
}
