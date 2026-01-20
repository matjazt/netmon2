package com.matjazt.netmon2.repository;

import com.matjazt.netmon2.entity.AlertEntity;
import com.matjazt.netmon2.entity.AlertType;
import com.matjazt.netmon2.entity.DeviceEntity;
import com.matjazt.netmon2.entity.NetworkEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link AlertEntity}.
 *
 * <p>Manages alerts triggered by network and device issues. Demonstrates advanced Spring Data JPA
 * features including derived queries, custom JPQL, pagination, and complex filtering.
 *
 * <p>Alert Lifecycle:
 *
 * <ul>
 *   <li>Alert created when issue detected (e.g., network down, device offline)
 *   <li>Alert remains open while issue persists (closureTimestamp is null)
 *   <li>Alert closed when issue resolves (closureTimestamp set to current time)
 *   <li>Closure email sent to notify administrators
 * </ul>
 */
@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, Long> {

    /**
     * Finds all open (unresolved) alerts.
     *
     * <p>An alert is open when {@code closureTimestamp} is null. Used by alert service to check
     * active alerts and determine if closure emails should be sent.
     *
     * @return list of open alerts
     */
    List<AlertEntity> findByClosureTimestampIsNull();

    /**
     * Finds open alerts for a specific network.
     *
     * <p>Useful for dashboard showing current issues per network. Combines network navigation and
     * null check.
     *
     * @param networkId the network ID
     * @return list of open alerts for the network
     */
    List<AlertEntity> findByNetwork_IdAndClosureTimestampIsNull(Long networkId);

    /**
     * Finds all alerts (open and closed) for a specific network.
     *
     * <p>Uses entity parameter instead of ID for demonstration. Spring Data JPA handles the join
     * automatically.
     *
     * @param network the network entity
     * @return list of all alerts for the network
     *     <p>s alerts by type.
     *     <p>Alert types: NETWORK_DOWN, DEVICE_DOWN, DEVICE_UNAUTHORIZED. Uses enum parameter -
     *     Spring Data JPA maps to database ordinal value automatically.
     * @param alertType the alert type
     * @return list of alerts of the specified type
     */
    List<AlertEntity> findByAlertType(AlertType alertType);

    /**
     * Finds open alerts by type.
     *
     * <p>Combines enum and null check. Useful for counting specific alert types currently active.
     *
     * @param alertType the alert type
     * @return list of open alerts of the specified type);
     *     <p>/** Find alerts by type
     *     <p>Example: find all DEVICE_UNAUTHORIZED alerts
     */
    List<AlertEntity> findByAlertTypeAndClosureTimestampIsNull(AlertType alertType);

    /**
     * Finds alerts for a specific device.
     *
     * <p>Note: Device can be null for network-level alerts (NETWORK_DOWN). Use {@code findByDevice}
     * instead of this method if you need to handle nulls explicitly.
     *
     * @param deviceId the device ID
     * @return list of alerts for the device
     */
    List<AlertEntity> findByDevice_Id(Long deviceId);

    /**
     * Finds alerts within a date range (inclusive).
     *
     * <p>The "Between" keyword includes both start and end timestamps. Useful for generating
     * reports for specific time periods.
     *
     * @param start the start timestamp (inclusive)
     * @param end the end timestamp (inclusive)
     * @return list of alerts within the date rangeng deviceId);
     *     <p>/** Find alerts within a date range
     *     <p>Between includes both boundaries.
     *     <p>Useful for generating reports.
     */
    List<AlertEntity> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * PAGINATION EXAMPLE: Find all alerts with pagination
     *
     * <p>Pageable parameter allows sorting and pagination. Returns a Page object with:
     *
     * <ul>
     *   <li>List of results for current page
     *   <li>Total number of records
     *   <li>Page metadata (size, number, etc.)
     * </ul>
     *
     * <p>Usage: alertRepo.findAll(PageRequest.of(0, 20, Sort.by("timestamp").descending()))
     */
    Page<AlertEntity> findByNetwork_Id(Long networkId, Pageable pageable);

    /**
     * CUSTOM QUERY: Find recent open alerts across all networks
     *
     * <p>Useful for dashboard overview. JOIN FETCH loads network eagerly to avoid N+1 queries.
     */
    @Query(
            "SELECT a FROM AlertEntity a "
                    + "JOIN FETCH a.network "
                    + "WHERE a.closureTimestamp IS NULL "
                    + "ORDER BY a.timestamp DESC")
    List<AlertEntity> findRecentOpenAlerts(Pageable pageable);

    /**
     * Find the latest alert for a network or device.
     *
     * <p>If device is null, finds latest network-level alert. If device is provided, finds latest
     * device-specific alert.
     *
     * @param network The network
     * @param device The device (null for network-level alerts)
     * @return The most recent alert, or empty if none exists
     */
    @Query(
            "SELECT a FROM AlertEntity a "
                    + "WHERE a.network = :network "
                    + "AND ((:device IS NULL AND a.device IS NULL) OR a.device = :device) "
                    + "ORDER BY a.timestamp DESC "
                    + "LIMIT 1")
    Optional<AlertEntity> findLatestAlert(
            @Param("network") NetworkEntity network, @Param("device") DeviceEntity device);

    /**
     * CUSTOM QUERY: Count open alerts per network
     *
     * <p>Returns custom projection: network ID and count. Useful for showing alert counts on
     * network list.
     */
    @Query(
            "SELECT a.network.id, COUNT(a) FROM AlertEntity a "
                    + "WHERE a.closureTimestamp IS NULL "
                    + "GROUP BY a.network.id")
    List<Object[]> countOpenAlertsByNetwork();

    /**
     * CUSTOM QUERY: Find alerts that have been open for too long
     *
     * <p>Example: alerts open for more than 24 hours need escalation.
     */
    @Query(
            "SELECT a FROM AlertEntity a "
                    + "WHERE a.closureTimestamp IS NULL "
                    + "AND a.timestamp < :thresholdTime")
    List<AlertEntity> findStaleOpenAlerts(@Param("thresholdTime") LocalDateTime thresholdTime);

    /**
     * Count open alerts for a network
     *
     * <p>Derived query for counting.
     */
    long countByNetwork_IdAndClosureTimestampIsNull(Long networkId);
}
