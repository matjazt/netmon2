
package com.matjazt.netmon2.repository

import com.matjazt.netmon2.entity.DeviceEntity
import com.matjazt.netmon2.entity.DeviceOperationMode

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

import java.time.LocalDateTime

/**
 * Spring Data JPA repository for [DeviceEntity].
 *
 * Provides CRUD operations and custom query methods for managing network devices. Demonstrates
 * both derived query methods (named method patterns) and custom JPQL queries with `@Query`.
 *
 * Query Method Types:
 *
 * - Derived queries: Method name parsed to generate query (e.g., findByMacAddress)
 * - JPQL queries: Custom queries with @Query annotation for complex logic
 * - Native queries: Direct SQL when JPQL limitations are reached
 */
@Repository
interface DeviceRepository : JpaRepository<DeviceEntity, Long> {

    fun findByMacAddress(macAddress: String): DeviceEntity?
    fun findByNetwork_Id(networkId: Long): List<DeviceEntity>
    fun findByNetwork_Id(networkId: Long, sort: Sort): List<DeviceEntity>
    fun findByOnline(online: Boolean): List<DeviceEntity>
    fun findByNetwork_IdAndOnline(networkId: Long, online: Boolean): List<DeviceEntity>
    fun findByDeviceOperationMode(mode: DeviceOperationMode): List<DeviceEntity>
    fun findByNetwork_IdAndDeviceOperationMode(networkId: Long, mode: DeviceOperationMode): List<DeviceEntity>
    fun findByDeviceOperationModeAndOnline(mode: DeviceOperationMode, online: Boolean): List<DeviceEntity>
    fun findByActiveAlertIdIsNotNull(): List<DeviceEntity>
    fun findByLastSeenBefore(threshold: LocalDateTime): List<DeviceEntity>
    fun findByNetwork_IdAndMacAddress(networkId: Long, macAddress: String): DeviceEntity?
    fun existsByNetwork_IdAndMacAddress(networkId: Long, macAddress: String): Boolean
    fun countByNetwork_Id(networkId: Long): Long
    fun countByNetwork_IdAndOnline(networkId: Long, online: Boolean): Long

    @Query(
        "SELECT d FROM DeviceEntity d " +
        "WHERE d.deviceOperationMode = :alwaysOn " +
        "AND d.online = false " +
        "AND d.activeAlertId IS NULL"
    )
    fun findAlwaysOnDevicesNeedingAlert(
        @Param("alwaysOn") alwaysOn: DeviceOperationMode
    ): List<DeviceEntity>

    @Query(
        "SELECT d FROM DeviceEntity d " +
        "WHERE d.deviceOperationMode = :unauthorized " +
        "AND d.activeAlertId IS NULL"
    )
    fun findUnauthorizedDevicesNeedingAlert(
        @Param("unauthorized") unauthorized: DeviceOperationMode
    ): List<DeviceEntity>

    @Query("SELECT d FROM DeviceEntity d JOIN FETCH d.network WHERE d.id = :id")
    fun findByIdWithNetwork(@Param("id") id: Long): DeviceEntity?

    @Query("SELECT d FROM DeviceEntity d JOIN FETCH d.network")
    fun findAllWithNetwork(): List<DeviceEntity>

    @Query(
        value = "SELECT d FROM DeviceEntity d JOIN FETCH d.network",
        countQuery = "SELECT count(d) FROM DeviceEntity d"
    )
    fun findAllWithNetwork(pageable: Pageable): Page<DeviceEntity>

    @Query(
        value = "SELECT d FROM DeviceEntity d JOIN FETCH d.network WHERE d.network.id = :networkId",
        countQuery = "SELECT count(d) FROM DeviceEntity d WHERE d.network.id = :networkId"
    )
    fun findByNetworkIdWithNetwork(
        @Param("networkId") networkId: Long,
        pageable: Pageable
    ): Page<DeviceEntity>

    @Modifying
    @Query("UPDATE DeviceEntity d SET d.lastSeen = :timestamp WHERE d.id = :id")
    fun updateLastSeen(
        @Param("id") id: Long,
        @Param("timestamp") timestamp: LocalDateTime
    ): Int

    @Modifying
    @Query(
        "UPDATE DeviceEntity d SET d.online = :online, d.lastSeen = :timestamp " +
        "WHERE d.network.id = :networkId AND d.macAddress IN :macAddresses"
    )
    fun bulkUpdateOnlineStatus(
        @Param("networkId") networkId: Long,
        @Param("macAddresses") macAddresses: List<String>,
        @Param("online") online: Boolean,
        @Param("timestamp") timestamp: LocalDateTime
    ): Int
}
