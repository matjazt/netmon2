package com.matjazt.netmon2.repository;

import com.matjazt.netmon2.entity.LogEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for {@link LogEntity}.
 *
 * <p>Provides CRUD operations for managing log entries. Used by the async log writer service to
 * persist log messages that reference network or device entities.
 */
@Repository
public interface LogRepository extends JpaRepository<LogEntity, Long> {

    /** Find logs by network ID with pagination */
    Page<LogEntity> findByNetwork_Id(Long networkId, Pageable pageable);

    /** Find logs by device ID with pagination */
    Page<LogEntity> findByDevice_Id(Long deviceId, Pageable pageable);

    /** Find logs by network ID and timestamp range with pagination */
    Page<LogEntity> findByNetwork_IdAndTimestampBetween(
            Long networkId,
            LocalDateTime minTimestamp,
            LocalDateTime maxTimestamp,
            Pageable pageable);

    /** Find logs by device ID and timestamp range with pagination */
    Page<LogEntity> findByDevice_IdAndTimestampBetween(
            Long deviceId,
            LocalDateTime minTimestamp,
            LocalDateTime maxTimestamp,
            Pageable pageable);

    /** Find logs by timestamp range with pagination */
    Page<LogEntity> findByTimestampBetween(
            LocalDateTime minTimestamp, LocalDateTime maxTimestamp, Pageable pageable);

    /** Find all logs with pagination (already provided by JpaRepository.findAll(Pageable)) */

    /** Find logs that belong to any of the given networks, with pagination */
    Page<LogEntity> findByNetwork_IdIn(List<Long> networkIds, Pageable pageable);
}
