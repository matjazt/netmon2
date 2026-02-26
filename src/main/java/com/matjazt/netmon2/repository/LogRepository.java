package com.matjazt.netmon2.repository;

import com.matjazt.netmon2.entity.LogEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link LogEntity}.
 *
 * <p>Provides CRUD operations for managing log entries. Used by the async log writer service to
 * persist log messages that reference network or device entities.
 */
@Repository
public interface LogRepository extends JpaRepository<LogEntity, Long> {
    // Standard CRUD operations provided by JpaRepository
    // Custom query methods can be added here if needed
}
