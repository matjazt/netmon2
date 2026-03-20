
package com.matjazt.netmon2.repository

import com.matjazt.netmon2.entity.LogEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Spring Data JPA repository for [LogEntity].
 *
 * Provides CRUD operations for managing log entries. Used by the async log writer service to
 * persist log messages that reference network or device entities.
 */
@Repository
interface LogRepository : JpaRepository<LogEntity, Long> {

    /** Find logs by network ID with pagination */
    fun findByNetwork_Id(networkId: Long, pageable: Pageable): Page<LogEntity>

    /** Find logs by device ID with pagination */
    fun findByDevice_Id(deviceId: Long, pageable: Pageable): Page<LogEntity>

    /** Find logs by network ID and timestamp range with pagination */
    fun findByNetwork_IdAndTimestampBetween(
        networkId: Long,
        minTimestamp: LocalDateTime,
        maxTimestamp: LocalDateTime,
        pageable: Pageable
    ): Page<LogEntity>

    /** Find logs by device ID and timestamp range with pagination */
    fun findByDevice_IdAndTimestampBetween(
        deviceId: Long,
        minTimestamp: LocalDateTime,
        maxTimestamp: LocalDateTime,
        pageable: Pageable
    ): Page<LogEntity>

    /** Find logs by timestamp range with pagination */
    fun findByTimestampBetween(
        minTimestamp: LocalDateTime,
        maxTimestamp: LocalDateTime,
        pageable: Pageable
    ): Page<LogEntity>

    /** Find all logs with pagination (already provided by JpaRepository.findAll(Pageable)) */
}
