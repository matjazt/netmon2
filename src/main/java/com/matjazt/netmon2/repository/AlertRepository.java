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
 * Repository for AlertEntity.
 *
 * <p>Manages alerts triggered by network/device issues.
 */
@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, Long> {

    /**
     * Find all open (unresolved) alerts
     *
     * <p>An alert is open if closureTimestamp is null.
     */
    List<AlertEntity> findByClosureTimestampIsNull();

    /** Find all closed (resolved) alerts */
    List<AlertEntity> findByClosureTimestampIsNotNull();

    /**
     * Find open alerts for a specific network
     *
     * <p>Useful for dashboard showing current issues per network.
     */
    List<AlertEntity> findByNetwork_IdAndClosureTimestampIsNull(Long networkId);

    /** Find all alerts for a specific network (open and closed) */
    List<AlertEntity> findByNetwork(NetworkEntity network);

    /**
     * Find alerts by type
     *
     * <p>Example: find all DEVICE_UNAUTHORIZED alerts
     */
    List<AlertEntity> findByAlertType(AlertType alertType);

    /**
     * Find open alerts by type
     *
     * <p>Combining enum and null check in derived query.
     */
    List<AlertEntity> findByAlertTypeAndClosureTimestampIsNull(AlertType alertType);

    /**
     * Find alerts for a specific device
     *
     * <p>Device can be null for network-level alerts, so check nullable fields carefully.
     */
    List<AlertEntity> findByDevice_Id(Long deviceId);

    /**
     * Find alerts within a date range
     *
     * <p>Between includes both boundaries.
     *
     * <p>Useful for generating reports.
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
