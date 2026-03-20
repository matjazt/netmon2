
package com.matjazt.netmon2.repository

import com.matjazt.netmon2.entity.AlertEntity
import com.matjazt.netmon2.entity.AlertType
import com.matjazt.netmon2.entity.DeviceEntity
import com.matjazt.netmon2.entity.NetworkEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Spring Data JPA repository for [AlertEntity].
 *
 * Manages alerts triggered by network and device issues. Demonstrates advanced Spring Data JPA
 * features including derived queries, custom JPQL, pagination, and complex filtering.
 *
 * Alert Lifecycle:
 * - Alert created when issue detected (e.g., network down, device offline)
 * - Alert remains open while issue persists (closureTimestamp is null)
 * - Alert closed when issue resolves (closureTimestamp set to current time)
 * - Closure email sent to notify administrators
 */
@Repository
interface AlertRepository : JpaRepository<AlertEntity, Long> {

    /**
     * Finds all open (unresolved) alerts.
     *
     * An alert is open when [closureTimestamp] is null. Used by alert service to check
     * active alerts and determine if closure emails should be sent.
     *
     * @return list of open alerts
     */
    fun findByClosureTimestampIsNull(): List<AlertEntity>

    /**
     * Finds open alerts for a specific network.
     *
     * Useful for dashboard showing current issues per network. Combines network navigation and
     * null check.
     *
     * @param networkId the network ID
     * @return list of open alerts for the network
     */
    fun findByNetwork_IdAndClosureTimestampIsNull(networkId: Long): List<AlertEntity>

    /**
     * Finds all alerts (open and closed) for a specific network.
     *
     * Uses entity parameter instead of ID for demonstration. Spring Data JPA handles the join
     * automatically.
     *
     * @param network the network entity
     * @return list of all alerts for the network
     */
    fun findByNetwork(network: NetworkEntity): List<AlertEntity>

    /**
     * Finds alerts by type.
     *
     * Alert types: NETWORK_DOWN, DEVICE_DOWN, DEVICE_UNAUTHORIZED. Uses enum parameter -
     * Spring Data JPA maps to database ordinal value automatically.
     *
     * @param alertType the alert type
     * @return list of alerts of the specified type
     */
    fun findByAlertType(alertType: AlertType): List<AlertEntity>

    /**
     * Finds open alerts by type.
     *
     * Combines enum and null check. Useful for counting specific alert types currently active.
     *
     * @param alertType the alert type
     * @return list of open alerts of the specified type
     */
    fun findByAlertTypeAndClosureTimestampIsNull(alertType: AlertType): List<AlertEntity>

    /**
     * Finds alerts for a specific device.
     *
     * Note: Device can be null for network-level alerts (NETWORK_DOWN). Use [findByDevice]
     * instead of this method if you need to handle nulls explicitly.
     *
     * @param deviceId the device ID
     * @return list of alerts for the device
     */
    fun findByDevice_Id(deviceId: Long): List<AlertEntity>

    /** Finds open (unresolved) alerts for a specific device. */
    fun findByDevice_IdAndClosureTimestampIsNull(deviceId: Long): List<AlertEntity>

    /**
     * Finds alerts within a date range (inclusive).
     *
     * The "Between" keyword includes both start and end timestamps. Useful for generating
     * reports for specific time periods.
     *
     * @param start the start timestamp (inclusive)
     * @param end the end timestamp (inclusive)
     * @return list of alerts within the date range
     */
    fun findByTimestampBetween(start: LocalDateTime, end: LocalDateTime): List<AlertEntity>

    /**
     * PAGINATION EXAMPLE: Find all alerts with pagination
     *
     * Pageable parameter allows sorting and pagination. Returns a Page object with:
     * - List of results for current page
     * - Total number of records
     * - Page metadata (size, number, etc.)
     *
     * Usage: alertRepo.findAll(PageRequest.of(0, 20, Sort.by("timestamp").descending()))
     */
    fun findByNetwork_Id(networkId: Long, pageable: Pageable): Page<AlertEntity>

    /** Finds all alerts (open and closed) for a specific network, unpaginated. */
    fun findByNetwork_Id(networkId: Long): List<AlertEntity>

    /**
     * CUSTOM QUERY: Find open alerts for a network
     *
     * Example of custom JPQL query. Finds all open alerts for a network ordered by ID. Useful
     * for showing active issues on network dashboard.
     *
     * @param networkId the network ID
     * @return list of open alerts for the network ordered by ID
     */
    @Query(
        "SELECT a FROM AlertEntity a " +
        "WHERE a.network.id = :networkId " +
        "AND a.closureTimestamp IS NULL " +
        "ORDER BY a.id"
    )
    fun findOpenAlertsByNetworkId(@Param("networkId") networkId: Long): List<AlertEntity>

    /**
     * CUSTOM QUERY: Find recent open alerts across all networks
     *
     * Useful for dashboard overview. JOIN FETCH loads network eagerly to avoid N+1 queries.
     */
    @Query(
        "SELECT a FROM AlertEntity a " +
        "JOIN FETCH a.network " +
        "WHERE a.closureTimestamp IS NULL " +
        "ORDER BY a.timestamp DESC"
    )
    fun findRecentOpenAlerts(pageable: Pageable): List<AlertEntity>

    /**
     * Find the latest alert for a network or device.
     *
     * If device is null, finds latest network-level alert. If device is provided, finds latest
     * device-specific alert.
     *
     * @param network The network
     * @param device The device (null for network-level alerts)
     * @return The most recent alert, or empty if none exists
     */
    @Query(
        "SELECT a FROM AlertEntity a " +
        "WHERE a.network = :network " +
        "AND ((:device IS NULL AND a.device IS NULL) OR a.device = :device) " +
        "ORDER BY a.timestamp DESC " +
        "LIMIT 1"
    )
    fun findLatestAlert(
        @Param("network") network: NetworkEntity,
        @Param("device") device: DeviceEntity?
    ): AlertEntity?

    /**
     * CUSTOM QUERY: Count open alerts per network
     *
     * Returns custom projection: network ID and count. Useful for showing alert counts on
     * network list.
     */
    @Query(
        "SELECT a.network.id, COUNT(a) FROM AlertEntity a " +
        "WHERE a.closureTimestamp IS NULL " +
        "GROUP BY a.network.id"
    )
    fun countOpenAlertsByNetwork(): List<Array<Any>>

    /**
     * CUSTOM QUERY: Find alerts that have been open for too long
     *
     * Example: alerts open for more than 24 hours need escalation.
     */
    @Query(
        "SELECT a FROM AlertEntity a " +
        "WHERE a.closureTimestamp IS NULL " +
        "AND a.timestamp < :thresholdTime"
    )
    fun findStaleOpenAlerts(@Param("thresholdTime") thresholdTime: LocalDateTime): List<AlertEntity>

    /**
     * Count open alerts for a network
     *
     * Derived query for counting.
     */
    fun countByNetwork_IdAndClosureTimestampIsNull(networkId: Long): Long
}
